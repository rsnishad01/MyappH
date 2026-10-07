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
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 12; Pixel 6 Build/SD1A.210817.036) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(10000)
            .setReadTimeoutMs(10000)
        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                setAudioAttributes(audioAttributes, true)
                repeatMode = Player.REPEAT_MODE_ONE
                playWhenReady = isPlaying
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
            exoPlayer.release()
        }
    }

    // Handle isPlaying state changes
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    // Play/update media item
    LaunchedEffect(videoUrl) {
        if (videoUrl.isNotBlank()) {
            try {
                hasError = false
                val uri = if (videoUrl.startsWith("android.resource://") ||
                    videoUrl.startsWith("content://") ||
                    videoUrl.startsWith("file://") ||
                    videoUrl.startsWith("http://") ||
                    videoUrl.startsWith("https://")
                ) {
                    Uri.parse(videoUrl)
                } else if (videoUrl.startsWith("/")) {
                    Uri.fromFile(java.io.File(videoUrl))
                } else {
                    Uri.parse(videoUrl)
                }

                val mediaItem = MediaItem.fromUri(uri)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                if (isPlaying) {
                    exoPlayer.play()
                } else {
                    exoPlayer.pause()
                }
            } catch (e: Exception) {
                hasError = true
            }
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
