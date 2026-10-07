package com.example.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.Serializable

private const val TAG = "ReelAudioService"

data class ComprehensiveAudioTrack(
    val id: String,
    val title: String,
    val artist: String,
    val durationSeconds: Int,
    val coverUrl: String,
    val rawResId: Int? = null,
    val audioUrl: String? = null,
    val streamUrl: String? = null
) : Serializable {
    fun getEffectiveAudioUri(): String? = streamUrl.takeIf { !it.isNullOrBlank() }
        ?: audioUrl.takeIf { !it.isNullOrBlank() }
        ?: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
}

data class ReelPlaybackInfo(
    val isPlaying: Boolean = false,
    val currentTrackId: String = "",
    val trackTitle: String = "",
    val trackArtist: String = "",
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val isMuted: Boolean = false
) : Serializable

class ReelAudioService(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null
    private var isPrepared = false

    private val _playbackInfo = MutableStateFlow(ReelPlaybackInfo())
    val playbackInfo: StateFlow<ReelPlaybackInfo> = _playbackInfo.asStateFlow()

    private val audioAttributes = AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .build()

    fun playAudioTrack(track: ComprehensiveAudioTrack) {
        stop()

        _playbackInfo.value = ReelPlaybackInfo(
            isPlaying = true,
            currentTrackId = track.id,
            trackTitle = track.title,
            trackArtist = track.artist,
            durationMs = track.durationSeconds * 1000,
            isMuted = _playbackInfo.value.isMuted
        )

        try {
            val player = MediaPlayer()
            player.setAudioAttributes(audioAttributes)

            var dataSourceSet = false

            if (track.rawResId != null) {
                try {
                    val afd = context.resources.openRawResourceFd(track.rawResId)
                    if (afd != null) {
                        player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        afd.close()
                        dataSourceSet = true
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load raw resource audio: ${e.message}")
                }
            }

            val effectiveUri = track.getEffectiveAudioUri()
            if (!dataSourceSet && !effectiveUri.isNullOrBlank()) {
                try {
                    val uri = Uri.parse(effectiveUri)
                    if (effectiveUri.startsWith("http://") || effectiveUri.startsWith("https://")) {
                        val headers = mapOf(
                            "User-Agent" to "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36",
                            "Accept" to "*/*"
                        )
                        player.setDataSource(context, uri, headers)
                    } else {
                        player.setDataSource(context, uri)
                    }
                    dataSourceSet = true
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to set audio data source: ${e.message}")
                }
            }

            if (!dataSourceSet) {
                // No valid audio stream found, continue with simulated playback metadata
                player.release()
                startProgressTracker()
                return
            }

            player.isLooping = true

            player.setOnPreparedListener { mp ->
                isPrepared = true
                val currentMuted = _playbackInfo.value.isMuted
                val volume = if (currentMuted) 0f else 1f
                try {
                    mp.setVolume(volume, volume)
                    mp.start()
                    val duration = if (mp.duration > 0) mp.duration else (track.durationSeconds * 1000)
                    _playbackInfo.value = _playbackInfo.value.copy(
                        isPlaying = true,
                        durationMs = duration
                    )
                } catch (ex: Exception) {
                    Log.w(TAG, "Error starting MediaPlayer onPrepared: ${ex.message}")
                }
            }

            player.setOnErrorListener { mp, what, extra ->
                Log.w(TAG, "MediaPlayer error occurred: what=$what, extra=$extra (recovering safely)")
                isPrepared = false
                try {
                    mp.reset()
                    mp.release()
                } catch (ignored: Exception) { }
                if (mediaPlayer == mp) {
                    mediaPlayer = null
                }
                true // Handled gracefully without crash
            }

            mediaPlayer = player
            player.prepareAsync()
            startProgressTracker()

        } catch (e: Exception) {
            Log.e(TAG, "Exception initializing audio track playback: ${e.message}")
            startProgressTracker()
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val current = try {
                    if (isPrepared && mediaPlayer?.isPlaying == true) mediaPlayer?.currentPosition ?: 0 else 0
                } catch (e: Exception) { 0 }

                val duration = try {
                    if (isPrepared) mediaPlayer?.duration?.takeIf { it > 0 } ?: 30000 else 30000
                } catch (e: Exception) { 30000 }

                _playbackInfo.value = _playbackInfo.value.copy(
                    currentPositionMs = current,
                    durationMs = duration
                )
                delay(300)
            }
        }
    }

    fun togglePlayPause() {
        try {
            mediaPlayer?.let { player ->
                if (isPrepared) {
                    if (player.isPlaying) {
                        player.pause()
                        _playbackInfo.value = _playbackInfo.value.copy(isPlaying = false)
                    } else {
                        player.start()
                        _playbackInfo.value = _playbackInfo.value.copy(isPlaying = true)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error toggling play/pause: ${e.message}")
        }
    }

    fun toggleMute() {
        val nextMuted = !_playbackInfo.value.isMuted
        val volume = if (nextMuted) 0f else 1f
        try {
            if (isPrepared) {
                mediaPlayer?.setVolume(volume, volume)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error setting audio volume: ${e.message}")
        }
        _playbackInfo.value = _playbackInfo.value.copy(isMuted = nextMuted)
    }

    fun stop() {
        progressJob?.cancel()
        isPrepared = false
        try {
            mediaPlayer?.let { player ->
                player.setOnErrorListener(null)
                player.setOnPreparedListener(null)
                try {
                    if (player.isPlaying) {
                        player.stop()
                    }
                } catch (ignored: Exception) { }
                try {
                    player.reset()
                } catch (ignored: Exception) { }
                try {
                    player.release()
                } catch (ignored: Exception) { }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error in stop(): ${e.message}")
        }
        mediaPlayer = null
        _playbackInfo.value = ReelPlaybackInfo()
    }
}

