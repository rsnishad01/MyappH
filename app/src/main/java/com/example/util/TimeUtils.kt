package com.example.util

object TimeUtils {
    fun getRelativeTimeAgo(timestamp: Long): String {
        val diff = (System.currentTimeMillis() - timestamp).coerceAtLeast(0L)
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val months = days / 30

        return when {
            seconds < 60 -> "अभी-अभी (Just now)"
            minutes < 60 -> "$minutes मिनट पहले (${minutes}m ago)"
            hours < 24 -> "$hours घंटे पहले (${hours}h ago)"
            days < 30 -> "$days दिन पहले (${days}d ago)"
            months < 12 -> "$months महीने पहले (${months}mo ago)"
            else -> {
                val years = months / 12
                if (years < 1) {
                    "कई महीने पहले (Months ago)"
                } else {
                    "$years साल पहले (${years}y ago)"
                }
            }
        }
    }
}
