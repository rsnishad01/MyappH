package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

enum class DraftType {
    POST, REEL
}

@Entity(tableName = "drafts")
data class DraftEntity(
    @PrimaryKey val id: String,
    val type: String, // "POST" or "REEL"
    val mediaUri: String,
    val caption: String = "",
    val location: String = "",
    val filterId: String = "normal",
    val audioTitle: String = "",
    val audioArtist: String = "",
    val thumbnailUri: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : Serializable
