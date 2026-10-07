package com.example

import android.app.Application
import android.content.Context
import android.util.Base64
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.disk.DiskCache
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.memory.MemoryCache
import coil.request.Options
import okio.Buffer
import java.io.File

/**
 * Custom Fetcher to enable Coil to decode and render data: URIs (Base64).
 */
class DataUriFetcher(
    private val dataUri: String,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val commaIndex = dataUri.indexOf(',')
        val base64Data = if (commaIndex != -1) dataUri.substring(commaIndex + 1) else dataUri
        var cleanBase64 = base64Data.trim()
            .replace("\n", "")
            .replace("\r", "")
            .replace(" ", "")
            .replace("-", "+")
            .replace("_", "/")
        while (cleanBase64.length % 4 != 0) {
            cleanBase64 += "="
        }
        val bytes = try {
            Base64.decode(cleanBase64, Base64.DEFAULT)
        } catch (e: Exception) {
            try {
                Base64.decode(cleanBase64, Base64.URL_SAFE)
            } catch (e2: Exception) {
                Log.e("DataUriFetcher", "Failed to decode base64: ${e2.message}")
                ByteArray(0)
            }
        }
        val buffer = Buffer().write(bytes)
        val mimeType = if (commaIndex != -1 && dataUri.startsWith("data:")) {
            dataUri.substring(5, commaIndex).substringBefore(';')
        } else "image/jpeg"

        return SourceResult(
            source = ImageSource(buffer, options.context),
            mimeType = mimeType,
            dataSource = DataSource.MEMORY
        )
    }

    class Factory : Fetcher.Factory<String> {
        override fun create(data: String, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (data.startsWith("data:image/") || data.startsWith("data:application/")) {
                return DataUriFetcher(data, options)
            }
            return null
        }
    }
}

class HundredGramApplication : Application(), ImageLoaderFactory {

    companion object {
        private const val TAG = "HundredGramApp"
        lateinit var instance: HundredGramApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "HundredGramApplication initialized with high-performance media pipeline.")
        createNotificationChannel()
        try {
            com.example.util.AdsManager.initialize(this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AdsManager: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channelId = "notifications_channel"
            val channel = android.app.NotificationChannel(
                channelId,
                "Notifications",
                android.app.NotificationManager.IMPORTANCE_HIGH
            )
            val notificationManager = getSystemService(android.app.NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(cacheDir, "image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024) // 100 MB disk cache
                    .build()
            }
            .components {
                add(DataUriFetcher.Factory())
            }
            .crossfade(true)
            .respectCacheHeaders(false)
            .build()
    }
}
