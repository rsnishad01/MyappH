package com.example.data.storage

import android.content.Context
import android.net.Uri
import com.example.data.compression.MediaCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class MediaStorageHelper(private val context: Context) {
    private val compressor = MediaCompressor(context)

    fun getPostStorageDirectory(): File {
        return File(context.filesDir, "posts").apply { mkdirs() }
    }

    fun getReelStorageDirectory(): File {
        return File(context.filesDir, "reels").apply { mkdirs() }
    }

    fun getStoryStorageDirectory(): File {
        return File(context.filesDir, "stories").apply { mkdirs() }
    }

    fun getAvatarStorageDirectory(): File {
        return File(context.filesDir, "avatars").apply { mkdirs() }
    }

    fun getPostStoragePath(): String = getPostStorageDirectory().absolutePath

    fun getReelStoragePath(): String = getReelStorageDirectory().absolutePath

    fun getStoryStoragePath(): String = getStoryStorageDirectory().absolutePath

    suspend fun savePostMedia(inputUri: Uri, userId: String = "user"): String = withContext(Dispatchers.IO) {
        val compressed = compressor.compressImage(inputUri, maxDimension = 1440)
        val outputFile = File(getPostStorageDirectory(), "post_${userId}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg")
        val compFile = compressed.file
        if (compFile != null && compFile.exists() && compFile.length() > 0) {
            compFile.copyTo(outputFile, overwrite = true)
        } else if (compressed.data != null && compressed.data.isNotEmpty()) {
            outputFile.writeBytes(compressed.data)
        } else {
            context.contentResolver.openInputStream(inputUri)?.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        Uri.fromFile(outputFile).toString()
    }

    suspend fun saveReelMedia(inputUri: Uri, userId: String = "user"): String = withContext(Dispatchers.IO) {
        val compressed = compressor.compressVideoReel(inputUri)
        val outputFile = File(getReelStorageDirectory(), "reel_${userId}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.mp4")
        val compFile = compressed.file
        if (compFile != null && compFile.exists() && compFile.length() > 0) {
            compFile.copyTo(outputFile, overwrite = true)
        } else {
            // Fallback: stream copy directly from inputUri
            context.contentResolver.openInputStream(inputUri)?.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        Uri.fromFile(outputFile).toString()
    }

    suspend fun saveStoryMedia(inputUri: Uri, userId: String = "user"): String = withContext(Dispatchers.IO) {
        val compressed = compressor.compressImage(inputUri, maxDimension = 1920)
        val outputFile = File(getStoryStorageDirectory(), "story_${userId}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg")
        val compFile = compressed.file
        if (compFile != null && compFile.exists() && compFile.length() > 0) {
            compFile.copyTo(outputFile, overwrite = true)
        } else if (compressed.data != null && compressed.data.isNotEmpty()) {
            outputFile.writeBytes(compressed.data)
        } else {
            context.contentResolver.openInputStream(inputUri)?.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        Uri.fromFile(outputFile).toString()
    }

    suspend fun saveMediaLocally(inputUri: Uri, prefix: String = "media"): String = withContext(Dispatchers.IO) {
        val uploadsDir = File(context.filesDir, "uploads").apply { mkdirs() }
        val compressed = compressor.compressImage(inputUri, maxDimension = 1440)
        val outputFile = File(uploadsDir, "${prefix}_${UUID.randomUUID().toString().take(8)}.jpg")
        val compFile = compressed.file
        if (compFile != null && compFile.exists() && compFile.length() > 0) {
            compFile.copyTo(outputFile, overwrite = true)
        } else if (compressed.data != null && compressed.data.isNotEmpty()) {
            outputFile.writeBytes(compressed.data)
        } else {
            context.contentResolver.openInputStream(inputUri)?.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        Uri.fromFile(outputFile).toString()
    }
}
