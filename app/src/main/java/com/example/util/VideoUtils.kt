package com.example.util

object VideoUtils {
    /**
     * Ensures video URL streams at maximum 720p resolution (720x1280 limit for portrait, 1280x720 for landscape).
     * If Cloudinary URL, applies w_720,h_1280,c_limit,q_auto,vc_h264 transformation.
     */
    fun get720pVideoUrl(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return ""

        // Cloudinary video transformation
        if (trimmed.contains("cloudinary.com") && trimmed.contains("/video/upload/")) {
            if (trimmed.contains("w_720") || trimmed.contains("c_limit")) {
                return trimmed
            }
            return trimmed.replace(
                "/video/upload/",
                "/video/upload/w_720,h_1280,c_limit,q_auto,vc_h264/"
            )
        }

        return trimmed
    }
}
