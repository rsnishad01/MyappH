package com.example.ui.reels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.MainViewModel
import com.example.ui.ScreenDestination
import com.example.ui.theme.HundredGramCardBackground
import com.example.ui.theme.HundredGramCardElevated
import com.example.ui.theme.HundredGramDarkBackground
import com.example.ui.theme.HundredGramPink
import com.example.ui.theme.HundredGramTextPrimary
import com.example.ui.theme.HundredGramTextSecondary

@Composable
fun SongDetailScreen(
    viewModel: MainViewModel,
    audioTitle: String,
    audioArtist: String,
    audioUrl: String = "",
    sourceReelId: String = ""
) {
    val allReels by viewModel.reels.collectAsState()
    val matchingReels = allReels.filter { 
        it.audioTitle.trim().equals(audioTitle.trim(), ignoreCase = true) ||
        (sourceReelId.isNotBlank() && it.id == sourceReelId) ||
        (audioTitle.isBlank() && it.audioTitle.isBlank())
    }
    val displayReels = matchingReels

    // Effective media URL from reel or parameter
    val effectiveAudioUrl = audioUrl.ifBlank {
        matchingReels.firstOrNull { it.videoUrl.isNotBlank() }?.videoUrl.orEmpty()
    }

    val playbackInfo by viewModel.audioPlaybackInfo.collectAsState()
    val isAudioPlaying = playbackInfo.isPlaying && (
        playbackInfo.trackTitle.equals(audioTitle, true) ||
        (effectiveAudioUrl.isNotBlank() && playbackInfo.currentTrackId.contains(audioTitle.hashCode().toString()))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HundredGramDarkBackground)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(ScreenDestination.Reels) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = HundredGramTextPrimary
                )
            }
            Text(
                text = "Audio Details",
                color = HundredGramTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        // Song Banner Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = HundredGramCardElevated)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(HundredGramPink),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = audioTitle.ifBlank { "Original Audio" },
                        color = HundredGramTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = audioArtist.ifBlank { "HundredGram Creator" },
                        color = HundredGramTextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "🎵 ${displayReels.size} reels created with this audio",
                        color = HundredGramPink,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (isAudioPlaying) {
                                viewModel.togglePlayPauseAudio()
                            } else {
                                val track = com.example.data.audio.ComprehensiveAudioTrack(
                                    id = "audio_${audioTitle.hashCode()}",
                                    title = audioTitle.ifBlank { "Original Audio" },
                                    artist = audioArtist.ifBlank { "Creator" },
                                    durationSeconds = 30,
                                    coverUrl = "",
                                    rawResId = null,
                                    audioUrl = effectiveAudioUrl,
                                    streamUrl = effectiveAudioUrl
                                )
                                viewModel.playAudioTrack(track)
                            }
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isAudioPlaying) HundredGramPink else HundredGramCardBackground)
                    ) {
                        Icon(
                            imageVector = if (isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isAudioPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.extractAndUseAudioFromDetails(
                                title = audioTitle.ifBlank { "Original Audio" },
                                artist = audioArtist.ifBlank { "Creator" },
                                audioUrl = effectiveAudioUrl
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HundredGramPink),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("Use Audio (साउंड लगाएं)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Reels using this sound",
            color = HundredGramTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Grid of Reels using this audio
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 2.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(displayReels) { reel ->
                Box(
                    modifier = Modifier
                        .aspectRatio(0.8f)
                        .background(HundredGramCardBackground)
                        .clickable {
                            viewModel.openReelInPlayer(reel.id)
                        }
                ) {
                    val safeThumb = remember(reel.thumbnailUri, reel.videoUrl, reel.id) {
                        com.example.util.ImageUtils.getReelThumbnailUrl(reel.thumbnailUri, reel.videoUrl, reel.id)
                    }
                    AsyncImage(
                        model = safeThumb,
                        contentDescription = "Reel thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${reel.likesCount}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
