package com.example.data.audio

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Serializable

data class YouTubeSongItem(
    val songId: String,
    val title: String,
    val artist: String,
    val albumCover: String,
    val durationFormatted: String,
    val streamUrl: String = ""
) : Serializable

class YouTubeSongSearchService(private val context: Context) {
    suspend fun searchSongs(query: String): List<YouTubeSongItem> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()
        listOf(
            YouTubeSongItem("yt_${trimmed.hashCode()}_1", "$trimmed (Original Mix)", "Soundtrack", "", "0:30"),
            YouTubeSongItem("yt_${trimmed.hashCode()}_2", "$trimmed (Acoustic Version)", "Acoustic Vibes", "", "0:45"),
            YouTubeSongItem("yt_${trimmed.hashCode()}_3", "$trimmed (Lo-fi Chill)", "Lo-Fi Beats", "", "0:60")
        )
    }
}

