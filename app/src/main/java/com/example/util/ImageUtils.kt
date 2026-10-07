package com.example.util

import com.example.data.PostEntity
import com.example.data.ReelVideo

object ImageUtils {
    fun getSafeImageModel(url: String): Any {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return ""
        if (trimmed.contains("cloudinary.com") && (trimmed.contains("/video/upload/") || isVideoExtension(trimmed))) {
            return trimmed.replace("/video/upload/", "/video/upload/so_0/")
                .replace(Regex("\\.(mp4|mov|mkv|webm|3gp)($|\\?)", RegexOption.IGNORE_CASE), ".jpg$2")
        }
        return trimmed
    }

    fun getReelThumbnailUrl(thumbnailUri: String, videoUrl: String, reelId: String = ""): String {
        val thumb = thumbnailUri.trim()
        val video = videoUrl.trim()

        if (thumb.isNotBlank() && !isVideoExtension(thumb)) {
            return getSafeImageModel(thumb).toString()
        }

        if (video.contains("cloudinary.com")) {
            return video.replace("/video/upload/", "/video/upload/so_0/")
                .replace(Regex("\\.(mp4|mov|mkv|webm|3gp)($|\\?)", RegexOption.IGNORE_CASE), ".jpg$2")
        }

        if (video.isNotBlank() && !isVideoExtension(video)) {
            return video
        }

        val seed = if (reelId.isNotBlank()) reelId.hashCode() else System.currentTimeMillis().toInt()
        val positiveSeed = (kotlin.math.abs(seed) % 900) + 100
        return "https://picsum.photos/id/$positiveSeed/400/600"
    }

    fun getPostThumbnailUrl(post: PostEntity): String {
        val img = post.imageUrl.trim()
        val media = post.mediaUrl.trim()

        if (img.isNotBlank() && !isVideoExtension(img)) {
            return getSafeImageModel(img).toString()
        }

        if (media.isNotBlank() && !isVideoExtension(media)) {
            return getSafeImageModel(media).toString()
        }

        if (media.contains("cloudinary.com")) {
            return media.replace("/video/upload/", "/video/upload/so_0/")
                .replace(Regex("\\.(mp4|mov|mkv|webm|3gp)($|\\?)", RegexOption.IGNORE_CASE), ".jpg$2")
        }

        val seed = if (post.id.isNotBlank()) post.id.hashCode() else System.currentTimeMillis().toInt()
        val positiveSeed = (kotlin.math.abs(seed) % 900) + 100
        return "https://picsum.photos/id/$positiveSeed/500/500"
    }

    private fun isVideoExtension(url: String): Boolean {
        val lower = url.lowercase()
        return lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".mkv") ||
               lower.endsWith(".webm") || lower.endsWith(".3gp") || lower.contains("/video/upload/")
    }
}
