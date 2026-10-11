package com.example.ui.components

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    videoUrl: String,
    isMuted: Boolean = false,
    isPlaying: Boolean = true,
    modifier: Modifier = Modifier,
    useController: Boolean = false,
    fallbackImageUrl: String = ""
) {
    val context = LocalContext.current
    var hasError by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }

    val exoPlayer = remember(context) {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
        val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(context)
        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)
        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context).apply {
            setEnableDecoderFallback(true)
            forceDisableMediaCodecAsynchronousQueueing()
            setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
        }
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(500, 10000, 250, 500)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
        ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build().apply {
                setAudioAttributes(audioAttributes, true)
                repeatMode = Player.REPEAT_MODE_ONE
                playWhenReady = isPlaying
                volume = if (isMuted) 0f else 1f
            }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                hasError = true
                isBuffering = false
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    hasError = false
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.pause()
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.release()
        }
    }

    // Handle isPlaying state changes - immediately silence and pause when false
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            exoPlayer.playWhenReady = true
            exoPlayer.play()
        } else {
            exoPlayer.playWhenReady = false
            exoPlayer.pause()
        }
    }

    // Play/update media item
    LaunchedEffect(videoUrl) {
        if (videoUrl.isNotBlank()) {
            try {
                val resolvedUri: Uri? = if (videoUrl.startsWith("android.resource://") ||
                    videoUrl.startsWith("content://") ||
                    videoUrl.startsWith("http://") ||
                    videoUrl.startsWith("https://")
                ) {
                    Uri.parse(videoUrl)
                } else {
                    val filePath = if (videoUrl.startsWith("file://")) videoUrl.removePrefix("file://") else videoUrl
                    val file = java.io.File(filePath)
                    if (file.exists() && file.length() > 0) {
                        Uri.fromFile(file)
                    } else {
                        null
                    }
                }

                if (resolvedUri != null) {
                    hasError = false
                    val mediaItem = MediaItem.fromUri(resolvedUri)
                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.prepare()
                    exoPlayer.volume = if (isMuted) 0f else 1f
                    if (isPlaying) {
                        exoPlayer.playWhenReady = true
                        exoPlayer.play()
                    } else {
                        exoPlayer.playWhenReady = false
                        exoPlayer.pause()
                    }
                } else {
                    hasError = true
                    isBuffering = false
                }
            } catch (e: Exception) {
                hasError = true
                isBuffering = false
            }
        } else {
            hasError = true
            isBuffering = false
        }
    }

    // Handle Mute state
    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (!hasError && videoUrl.isNotBlank()) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        setUseController(useController)
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (isBuffering) {
                AnimatedLoadingDots(
                    dotColor = Color.White,
                    dotSize = 8.dp,
                    spacing = 5.dp
                )
            }
        } else {
            val rawFallback = if (fallbackImageUrl.isNotBlank()) fallbackImageUrl else videoUrl
            val imageToShow = if (rawFallback.isNotBlank() && !rawFallback.contains(".mp4", ignoreCase = true) && !rawFallback.contains(".mov", ignoreCase = true)) {
                rawFallback
            } else {
                ""
            }
            val isImageLocal = imageToShow.startsWith("file://") || imageToShow.startsWith("/")
            val imageLocalExists = if (isImageLocal) {
                val path = if (imageToShow.startsWith("file://")) imageToShow.removePrefix("file://") else imageToShow
                java.io.File(path).exists()
            } else true

            if (imageToShow.isNotBlank() && imageLocalExists) {
                AsyncImage(
                    model = imageToShow,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Clean dark background with loading indicator
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedLoadingDots(
                        dotColor = Color.White.copy(alpha = 0.6f),
                        dotSize = 8.dp,
                        spacing = 5.dp
                    )
                }
            }
        }
    }
}
