package com.example.data.storage

import android.content.Context
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
import java.util.concurrent.TimeUnit

class MediaStorageManager(private val context: Context) {
    companion object {
        private const val TAG = "MediaStorageManager"

        // Base64 Obfuscated Runtime Credentials
        // "UmVlbF9hcHA=" -> "Reel_app"
        private val UPLOAD_PRESET by lazy {
            String(Base64.decode("UmVlbF9hcHA=", Base64.DEFAULT), Charsets.UTF_8).trim()
        }

        // "aHR0cHM6Ly9hcGkuY2xvdWRpbmFyeS5jb20vdjFfMS9vNDRka2F3Zy9hdXRvL3VwbG9hZA==" -> "https://api.cloudinary.com/v1_1/o44dkawg/auto/upload"
        private val UPLOAD_URL by lazy {
            String(Base64.decode("aHR0cHM6Ly9hcGkuY2xvdWRpbmFyeS5jb20vdjFfMS9vNDRka2F3Zy9hdXRvL3VwbG9hZA==", Base64.DEFAULT), Charsets.UTF_8).trim()
        }
    }

    private val compressor = MediaCompressor(context)

    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
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
            val buffer = ByteArray(4096)
            var bytesWritten = 0L
            FileInputStream(file).use { input ->
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    sink.write(buffer, 0, read)
                    bytesWritten += read
                    onProgress(bytesWritten, fileLength)
                }
            }
        }
    }

    fun uploadMediaFlow(
        uri: Uri,
        folder: String = "posts",
        contentTypeOverride: String? = null
    ): Flow<UploadStatus> = callbackFlow {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            trySend(UploadStatus.Error("Authentication missing. Please log in first."))
            close()
            return@callbackFlow
        }
        val userId = currentUser.uid
        val folderPath = "hundredgram/users/$userId/$folder"

        val contentResolver = context.contentResolver
        val mimeType = contentTypeOverride ?: contentResolver.getType(uri) ?: run {
            val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
        } ?: "application/octet-stream"

        Log.d(TAG, "Identified MIME Type: $mimeType for URI: $uri")

        val isVideo = mimeType.contains("video", ignoreCase = true)
        val isImage = mimeType.contains("image", ignoreCase = true)

        if (!isImage && !isVideo) {
            trySend(UploadStatus.Error("Unsupported media format: $mimeType"))
            close()
            return@callbackFlow
        }

        trySend(UploadStatus.Progress(0.05f, "Preparing media..."))

        var tempFile: File? = null
        try {
            val extension = when {
                isVideo -> "mp4"
                mimeType.contains("png", ignoreCase = true) -> "png"
                mimeType.contains("webp", ignoreCase = true) -> "webp"
                else -> "jpg"
            }
            tempFile = File.createTempFile("upload_media_", ".$extension", context.cacheDir)
            contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw IOException("Failed reading media stream.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed creating temporary upload file: ${e.message}")
            trySend(UploadStatus.Error("Media preparation failed."))
            close()
            return@callbackFlow
        }

        var finalUploadFile = tempFile
        var isCompressed = false

        if (isImage || isVideo) {
            trySend(UploadStatus.Progress(0.15f, "Optimizing media..."))
            try {
                val compressionResult = compressor.compressMedia(
                    uri = uri,
                    folder = folder,
                    contentType = mimeType,
                    onProgress = { progressRatio, message ->
                        val calculatedProgress = 0.15f + (progressRatio * 0.25f)
                        trySend(UploadStatus.Progress(calculatedProgress, message))
                    }
                )
                val compressedFile = compressionResult.file
                if (compressedFile != null && compressedFile.exists() && compressedFile.length() > 0) {
                    finalUploadFile = compressedFile
                    isCompressed = true
                    Log.d(TAG, "Media optimized: ${tempFile.length()} -> ${compressedFile.length()} bytes")
                }
            } catch (ex: Exception) {
                Log.w(TAG, "Media optimization skipped/failed, proceeding with original file: ${ex.message}")
            }
        }

        trySend(UploadStatus.Progress(0.40f, "Starting secure upload..."))

        val fileRequestBody = ProgressRequestBody(
            file = finalUploadFile,
            contentType = mimeType,
            onProgress = { bytesWritten, contentLength ->
                val ratio = (bytesWritten.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                val overallProgress = 0.40f + (ratio * 0.58f)
                val percent = (ratio * 100).toInt()
                trySend(UploadStatus.Progress(overallProgress, "Uploading $percent%..."))
            }
        )

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", finalUploadFile.name, fileRequestBody)
            .addFormDataPart("upload_preset", UPLOAD_PRESET)
            .addFormDataPart("folder", folderPath)
            .build()

        val request = Request.Builder()
            .url(UPLOAD_URL)
            .post(requestBody)
            .build()

        val activeCall = client.newCall(request)

        activeCall.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                cleanupFiles(tempFile, finalUploadFile, isCompressed)
                if (call.isCanceled()) {
                    trySend(UploadStatus.Error("Upload cancellation"))
                } else {
                    trySend(UploadStatus.Error("Network error or server unreachable: ${e.message}"))
                }
                close()
            }

            override fun onResponse(call: Call, response: Response) {
                cleanupFiles(tempFile, finalUploadFile, isCompressed)
                response.use {
                    if (!it.isSuccessful) {
                        val errBody = it.body?.string() ?: ""
                        Log.e(TAG, "Server error: $errBody")
                        trySend(UploadStatus.Error("Upload failed: HTTP ${it.code}"))
                        close()
                        return
                    }

                    try {
                        val responseData = it.body?.string() ?: throw IOException("Empty response body.")
                        Log.d(TAG, "Upload response received successfully")

                        val json = JSONObject(responseData)
                        val secureUrl = json.getString("secure_url")
                        val publicId = json.optString("public_id", "")
                        val resourceType = json.optString("resource_type", "")
                        val format = json.optString("format", "")
                        val bytes = json.optLong("bytes", 0L)
                        val width = json.optInt("width", 0)
                        val height = json.optInt("height", 0)
                        val originalFilename = json.optString("original_filename", "")

                        trySend(UploadStatus.Progress(1.0f, "Upload completed"))
                        trySend(
                            UploadStatus.Success(
                                downloadUrl = secureUrl,
                                publicId = publicId,
                                resourceType = resourceType,
                                format = format,
                                bytes = bytes,
                                width = width,
                                height = height,
                                originalFilename = originalFilename
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed parsing response: ${e.message}")
                        trySend(UploadStatus.Error("Response parsing failure."))
                    }
                    close()
                }
            }
        })

        awaitClose {
            try {
                if (!activeCall.isCanceled() && !activeCall.isExecuted()) {
                    Log.d(TAG, "Upload Flow collection cancelled. Cancelling OkHttp request.")
                    activeCall.cancel()
                }
            } catch (ex: Exception) {
                Log.w(TAG, "Failed cancelling OkHttp request: ${ex.message}")
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
