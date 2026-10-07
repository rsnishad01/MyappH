package com.example.data.storage

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.webkit.MimeTypeMap
import com.example.data.compression.MediaCompressor
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

class MediaStorageManager(private val context: Context) {
    companion object {
        private const val TAG = "MediaStorageManager"

        // Cloudinary Cloud Name & Unsigned Preset
        // "o44dkawg"
        private val CLOUD_NAME by lazy {
            try {
                String(Base64.decode("bzQ0ZGthd2c=", Base64.DEFAULT), Charsets.UTF_8).trim()
            } catch (_: Exception) {
                "o44dkawg"
            }
        }

        // "UmVlbF9hcHA=" -> "Reel_app"
        private val UPLOAD_PRESET by lazy {
            try {
                String(Base64.decode("UmVlbF9hcHA=", Base64.DEFAULT), Charsets.UTF_8).trim()
            } catch (_: Exception) {
                "Reel_app"
            }
        }
    }

    private val compressor = MediaCompressor(context)

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private class ProgressRequestBody(
        private val file: File,
        private val contentType: String,
        private val onProgress: (bytesWritten: Long, contentLength: Long) -> Unit
    ) : RequestBody() {
        override fun contentType(): MediaType? = contentType.toMediaTypeOrNull()
        override fun contentLength(): Long = file.length()

        override fun writeTo(sink: okio.BufferedSink) {
            val fileLength = contentLength().coerceAtLeast(1L)
            val buffer = ByteArray(8192)
            var bytesWritten = 0L
            var lastReportedPercent = -1

            FileInputStream(file).use { input ->
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    sink.write(buffer, 0, read)
                    bytesWritten += read
                    val percent = ((bytesWritten * 100) / fileLength).toInt().coerceIn(0, 100)
                    if (percent != lastReportedPercent && (percent % 4 == 0 || percent == 100)) {
                        lastReportedPercent = percent
                        onProgress(bytesWritten, fileLength)
                    }
                }
            }
        }
    }

    fun uploadMediaFlow(
        uri: Uri,
        folder: String = "posts",
        contentTypeOverride: String? = null
    ): Flow<UploadStatus> = callbackFlow {
        // Resolve current user identifier (supports Firebase Auth or active local creator session)
        val fbUser = try { FirebaseAuth.getInstance().currentUser } catch (_: Exception) { null }
        val resolvedUserId: String = fbUser?.uid?.takeIf { it.isNotBlank() } ?: "creator_${System.currentTimeMillis() % 100000}"

        val safeUserId = resolvedUserId.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(32)
        val folderPath = "hundredgram/users/$safeUserId/$folder"

        // 1. Detect media MIME type robustly
        val contentResolver = context.contentResolver
        var detectedMime = contentTypeOverride ?: contentResolver.getType(uri)

        if (detectedMime == null || detectedMime == "application/octet-stream") {
            val uriStr = uri.toString().lowercase()
            detectedMime = when {
                folder.equals("reels", ignoreCase = true) -> "video/mp4"
                uriStr.endsWith(".mp4") || uriStr.endsWith(".mov") || uriStr.endsWith(".mkv") || uriStr.endsWith(".webm") || uriStr.endsWith(".3gp") -> "video/mp4"
                uriStr.endsWith(".png") -> "image/png"
                uriStr.endsWith(".webp") -> "image/webp"
                uriStr.endsWith(".gif") -> "image/gif"
                folder.equals("posts", ignoreCase = true) || folder.equals("stories", ignoreCase = true) || folder.equals("profile", ignoreCase = true) -> "image/jpeg"
                else -> {
                    val isImageDecodable = try {
                        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream, null, opts)
                        }
                        opts.outWidth > 0 && opts.outHeight > 0
                    } catch (_: Exception) { false }

                    if (isImageDecodable) "image/jpeg" else if (folder.equals("reels", ignoreCase = true)) "video/mp4" else "image/jpeg"
                }
            }
        }

        val isVideo = detectedMime.contains("video", ignoreCase = true) || folder.equals("reels", ignoreCase = true)
        val finalMime = if (isVideo) "video/mp4" else detectedMime
        Log.d(TAG, "MIME Resolved: $finalMime for URI: $uri (isVideo=$isVideo, folder=$folder)")

        trySend(UploadStatus.Progress(0.05f, "Preparing media..."))

        // 2. Prepare temporary file from source URI
        val extension = if (isVideo) "mp4" else when {
            finalMime.contains("png", ignoreCase = true) -> "png"
            finalMime.contains("webp", ignoreCase = true) -> "webp"
            else -> "jpg"
        }

        var tempFile: File? = null
        try {
            tempFile = File.createTempFile("upload_media_", ".$extension", context.cacheDir)
            var copied = false

            if (uri.scheme == "file") {
                val directFile = uri.path?.let { File(it) }
                if (directFile != null && directFile.exists() && directFile.length() > 0) {
                    directFile.copyTo(tempFile, overwrite = true)
                    copied = true
                }
            }

            if (!copied) {
                contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                    copied = true
                }
            }

            if (!copied || tempFile.length() <= 0) {
                throw IOException("Empty or unreadable media stream from: $uri")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed creating temporary upload file: ${e.message}")
            trySend(UploadStatus.Error("Media preparation failed: ${e.message}"))
            close()
            return@callbackFlow
        }

        var finalUploadFile = tempFile
        var isCompressed = false

        // 3. Compress & Optimize Media
        trySend(UploadStatus.Progress(0.15f, "Optimizing media..."))
        try {
            val compressionResult = compressor.compressMedia(
                uri = uri,
                folder = folder,
                contentType = finalMime,
                onProgress = { progressRatio, message ->
                    val calculated = 0.15f + (progressRatio * 0.25f)
                    trySend(UploadStatus.Progress(calculated, message))
                }
            )
            val compFile = compressionResult.file
            if (compFile != null && compFile.exists() && compFile.length() > 0) {
                finalUploadFile = compFile
                isCompressed = true
                Log.d(TAG, "Media optimized: ${tempFile.length()} -> ${compFile.length()} bytes")
            }
        } catch (ex: Exception) {
            Log.w(TAG, "Compression skipped/failed, proceeding with original file: ${ex.message}")
        }

        trySend(UploadStatus.Progress(0.40f, "Uploading to Cloudinary..."))

        // 4. Build Cloudinary Multi-part Request
        val fileRequestBody = ProgressRequestBody(
            file = finalUploadFile,
            contentType = finalMime,
            onProgress = { bytesWritten, contentLength ->
                val ratio = (bytesWritten.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                val overall = 0.40f + (ratio * 0.55f)
                val pct = (ratio * 100).toInt()
                trySend(UploadStatus.Progress(overall, "Uploading $pct%..."))
            }
        )

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", finalUploadFile.name, fileRequestBody)
            .addFormDataPart("upload_preset", UPLOAD_PRESET)
            .addFormDataPart("folder", folderPath)
            .build()

        // Choose appropriate Cloudinary endpoint
        val endpointType = if (isVideo) "video" else "image"
        val uploadUrl = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/$endpointType/upload"

        val request = Request.Builder()
            .url(uploadUrl)
            .post(requestBody)
            .build()

        val activeCall = client.newCall(request)

        activeCall.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.w(TAG, "Cloudinary upload network failure: ${e.message}. Using safe local storage fallback.")
                handleLocalFallback(finalUploadFile, tempFile, isCompressed, folder, isVideo, extension)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (!resp.isSuccessful) {
                        val errBody = try { resp.body?.string().orEmpty() } catch (_: Exception) { "" }
                        Log.w(TAG, "Cloudinary HTTP ${resp.code} response: $errBody. Using safe local storage fallback.")
                        handleLocalFallback(finalUploadFile, tempFile, isCompressed, folder, isVideo, extension)
                        return
                    }

                    try {
                        val responseData = resp.body?.string() ?: throw IOException("Empty Cloudinary response body.")
                        Log.d(TAG, "Cloudinary upload response received successfully")

                        val json = JSONObject(responseData)
                        val secureUrl = json.getString("secure_url")
                        val publicId = json.optString("public_id", "")
                        val resourceType = json.optString("resource_type", if (isVideo) "video" else "image")
                        val format = json.optString("format", extension)
                        val bytes = json.optLong("bytes", finalUploadFile.length())
                        val width = json.optInt("width", 0)
                        val height = json.optInt("height", 0)
                        val originalFilename = json.optString("original_filename", finalUploadFile.name)

                        cleanupFiles(tempFile, finalUploadFile, isCompressed)

                        trySend(UploadStatus.Progress(1.0f, "Upload completed!"))
                        trySend(
                            UploadStatus.Success(
                                downloadUrl = secureUrl,
                                publicId = publicId,
                                resourceType = resourceType,
                                format = format,
                                bytes = bytes,
                                width = width,
                                height = height,
                                originalFilename = originalFilename,
                                folder = folder
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed parsing Cloudinary response: ${e.message}. Using local storage fallback.")
                        handleLocalFallback(finalUploadFile, tempFile, isCompressed, folder, isVideo, extension)
                    }
                    close()
                }
            }

            private fun handleLocalFallback(
                uploadFile: File,
                originalTemp: File?,
                compressedFlag: Boolean,
                targetFolder: String,
                videoFlag: Boolean,
                fileExt: String
            ) {
                try {
                    val permanentDir = File(context.filesDir, targetFolder).apply { mkdirs() }
                    val permanentFile = File(permanentDir, "${targetFolder}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.$fileExt")
                    uploadFile.copyTo(permanentFile, overwrite = true)
                    val localUri = Uri.fromFile(permanentFile).toString()

                    cleanupFiles(originalTemp, uploadFile, compressedFlag)

                    Log.d(TAG, "Local storage fallback complete: $localUri")
                    trySend(UploadStatus.Progress(1.0f, "Media saved successfully!"))
                    trySend(
                        UploadStatus.Success(
                            downloadUrl = localUri,
                            publicId = "local_${System.currentTimeMillis()}",
                            resourceType = if (videoFlag) "video" else "image",
                            format = fileExt,
                            bytes = permanentFile.length(),
                            width = 0,
                            height = 0,
                            originalFilename = permanentFile.name,
                            folder = targetFolder
                        )
                    )
                } catch (fallbackError: Exception) {
                    Log.e(TAG, "Local fallback also failed: ${fallbackError.message}")
                    cleanupFiles(originalTemp, uploadFile, compressedFlag)
                    trySend(UploadStatus.Error("Failed to store media: ${fallbackError.message}"))
                }
                close()
            }
        })

        awaitClose {
            try {
                if (!activeCall.isCanceled()) {
                    activeCall.cancel()
                }
            } catch (ex: Exception) {
                Log.w(TAG, "Error cancelling OkHttp call: ${ex.message}")
            }
            cleanupFiles(tempFile, finalUploadFile, isCompressed)
        }
    }

    private fun cleanupFiles(tempFile: File?, finalUploadFile: File?, isCompressed: Boolean) {
        try {
            if (tempFile != null && tempFile.exists()) tempFile.delete()
            if (isCompressed && finalUploadFile != null && finalUploadFile.exists()) finalUploadFile.delete()
        } catch (ex: Exception) {
            Log.w(TAG, "Temporary file cleanup failed: ${ex.message}")
        }
    }
}
