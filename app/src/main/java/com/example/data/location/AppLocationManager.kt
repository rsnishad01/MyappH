package com.example.data.location

import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class AccurateLocationResult(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String = "Location unavailable",
    val city: String = ""
)

class AppLocationManager(private val context: Context) {

    suspend fun getCurrentAccurateLocation(): AccurateLocationResult = withContext(Dispatchers.IO) {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            
            // Try to get the best last known location
            var location: Location? = null
            
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )
            
            for (provider in providers) {
                try {
                    val loc = locationManager?.getLastKnownLocation(provider)
                    if (loc != null) {
                        // Use the most recent location
                        if (location == null || loc.time > location!!.time) {
                            location = loc
                        }
                    }
                } catch (e: SecurityException) {
                    continue
                }
            }

            if (location != null) {
                val lat = location.latitude
                val lon = location.longitude
                val address = reverseGeocode(lat, lon)
                AccurateLocationResult(lat, lon, address.ifBlank { "Current Location" }, "")
            } else {
                AccurateLocationResult(null, null, "Location unavailable", "")
            }
        } catch (e: Exception) {
            AccurateLocationResult(null, null, "Location unavailable", "")
        }
    }

    suspend fun getCurrentLocationName(): String = withContext(Dispatchers.IO) {
        val result = getCurrentAccurateLocation()
        result.address
    }

    suspend fun getPopularLocations(): List<String> = withContext(Dispatchers.IO) {
        val currentLoc = getCurrentLocationName()
        if (currentLoc != "Location unavailable" && currentLoc.isNotBlank()) {
            listOf(currentLoc)
        } else {
            emptyList()
        }
    }

    suspend fun searchLocations(query: String): List<String> = withContext(Dispatchers.IO) {
        if (query.isBlank()) {
            return@withContext emptyList()
        }
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocationName(query, 5)
            if (!addresses.isNullOrEmpty()) {
                addresses.mapNotNull { addr ->
                    val feature = addr.featureName
                    val locality = addr.locality ?: addr.adminArea
                    val country = addr.countryName
                    listOfNotNull(feature, locality, country).distinct().joinToString(", ").takeIf { it.isNotBlank() }
                }
            } else {
                listOf(query.trim())
            }
        } catch (e: Exception) {
            listOf(query.trim())
        }
    }

    suspend fun reverseGeocode(latitude: Double, longitude: Double): String = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val subLocality = addr.subLocality ?: addr.featureName
                val locality = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: ""
                val country = addr.countryName ?: ""
                return@withContext when {
                    !subLocality.isNullOrBlank() && locality.isNotBlank() -> "$subLocality, $locality"
                    locality.isNotBlank() && country.isNotBlank() -> "$locality, $country"
                    locality.isNotBlank() -> locality
                    country.isNotBlank() -> country
                    else -> "Current Location"
                }
            }
        } catch (e: Exception) {
            // Ignore geocoding failures gracefully
        }
        return@withContext "Location unavailable"
    }
}


