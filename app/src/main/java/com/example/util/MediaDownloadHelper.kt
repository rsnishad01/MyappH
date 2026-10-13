package com.example.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast

object MediaDownloadHelper {

    fun downloadReel(context: Context, videoUrl: String, title: String = "Reel") {
        downloadMedia(
            context = context,
            mediaUrl = videoUrl,
            defaultTitle = title.ifBlank { "HundredGram_Reel" },
            isAudioOrVideo = true
        )
    }

    fun downloadImage(context: Context, imageUrl: String, title: String = "Image") {
        downloadMedia(
            context = context,
            mediaUrl = imageUrl,
            defaultTitle = title.ifBlank { "HundredGram_Image" },
            isAudioOrVideo = false
        )
    }

    private fun downloadMedia(
        context: Context,
        mediaUrl: String,
        defaultTitle: String,
        isAudioOrVideo: Boolean
    ) {
        val cleanUrl = mediaUrl.trim()
        if (cleanUrl.isBlank()) {
            Toast.makeText(context, "डाउनलोड के लिए कोई वैध लिंक नहीं मिला", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            if (downloadManager == null) {
                Toast.makeText(context, "Download Manager अनुपलब्ध है", Toast.LENGTH_SHORT).show()
                return
            }

            val uri = Uri.parse(cleanUrl)
            val extension = if (isAudioOrVideo) "mp4" else {
                when {
                    cleanUrl.contains(".png", ignoreCase = true) -> "png"
                    cleanUrl.contains(".webp", ignoreCase = true) -> "webp"
                    else -> "jpg"
                }
            }

            val sanitizedTitle = defaultTitle.take(24).replace(Regex("[^a-zA-Z0-9_]"), "_").ifBlank { "Media" }
            val fileName = "${sanitizedTitle}_${System.currentTimeMillis()}.$extension"

            val request = DownloadManager.Request(uri).apply {
                setTitle("डाउनलोड हो रहा है: $sanitizedTitle")
                setDescription(if (isAudioOrVideo) "HundredGram Reel Video" else "HundredGram Post Image")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            downloadManager.enqueue(request)
            Toast.makeText(context, "डाउनलोड शुरू हो गया है... (Downloads फोल्डर में सेव होगा)", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "डाउनलोड विफल: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
