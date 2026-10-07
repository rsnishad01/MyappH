package com.example.data.nearby

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Serializable

data class NearbyUser(
    val id: String,
    val username: String,
    val name: String,
    val avatarUrl: String,
    val distanceMeters: Int,
    val bio: String,
    val isOnline: Boolean = true
) : Serializable

data class NearbyEvent(
    val id: String,
    val title: String,
    val location: String,
    val distanceKm: Double,
    val attendeesCount: Int,
    val bannerUrl: String
) : Serializable

data class NearbySearchResult(
    val users: List<NearbyUser> = emptyList(),
    val events: List<NearbyEvent> = emptyList()
) : Serializable

class NearbyService(private val context: Context) {
    suspend fun searchNearby(radiusKm: Double = 5.0): NearbySearchResult = withContext(Dispatchers.IO) {
        // Return empty result when no nearby users/events are in radius
        NearbySearchResult(
            users = emptyList(),
            events = emptyList()
        )
    }
}

