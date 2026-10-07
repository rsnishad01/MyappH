package com.example.ui.reels

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.audio.ComprehensiveAudioTrack
import com.example.ui.MainViewModel
import com.example.ui.theme.HundredGramCardBackground
import com.example.ui.theme.HundredGramCardElevated
import com.example.ui.theme.HundredGramDivider
import com.example.ui.theme.HundredGramPink
import com.example.ui.theme.HundredGramTextPrimary
import com.example.ui.theme.HundredGramTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicSearchPickerSheet(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onTrackSelected: (ComprehensiveAudioTrack) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var searchQuery by remember { mutableStateOf("") }
    val playbackInfo by viewModel.audioPlaybackInfo.collectAsState()
    val allTracks = remember { viewModel.repository.getAvailableAudioTracks() }

    // Audio file picker from device storage
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "Custom Audio"
            val track = ComprehensiveAudioTrack(
                id = "custom_${System.currentTimeMillis()}",
                title = fileName.substringBeforeLast("."),
                artist = "My Music File",
                durationSeconds = 30,
                coverUrl = "",
                rawResId = null,
                streamUrl = uri.toString()
            )
            viewModel.stopAudio()
            onTrackSelected(track)
        }
    }

    val filteredTracks = remember(searchQuery) {
        if (searchQuery.isBlank()) allTracks
        else allTracks.filter { it.title.contains(searchQuery, true) || it.artist.contains(searchQuery, true) }
    }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.stopAudio()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = HundredGramCardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(520.dp)
                .padding(16.dp)
        ) {
            Text(
                text = "Add Reel Audio",
                color = HundredGramTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Pick Custom Audio from Phone
            Button(
                onClick = { audioPickerLauncher.launch("audio/*") },
                colors = ButtonDefaults.buttonColors(containerColor = HundredGramPink),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                Icon(Icons.Default.AudioFile, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("📁 Choose Audio File from Device", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search soundtrack beats...", color = HundredGramTextSecondary, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = HundredGramTextSecondary) },
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = HundredGramTextPrimary,
                    unfocusedTextColor = HundredGramTextPrimary,
                    focusedBorderColor = HundredGramPink,
                    unfocusedBorderColor = HundredGramDivider
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Authentic Music Tracks",
                color = HundredGramTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (filteredTracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🎵 Choose your audio file from device above",
                            color = HundredGramTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                // Tracks List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTracks) { track ->
                        val isCurrentPlaying = playbackInfo.isPlaying && playbackInfo.currentTrackId == track.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(HundredGramCardElevated)
                                .clickable {
                                    viewModel.stopAudio()
                                    onTrackSelected(track)
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(HundredGramPink.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = HundredGramPink,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    color = if (isCurrentPlaying) HundredGramPink else HundredGramTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${track.artist} • ${track.durationSeconds}s",
                                    color = HundredGramTextSecondary,
                                    fontSize = 12.sp
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (isCurrentPlaying) viewModel.togglePlayPauseAudio() else viewModel.playAudioTrack(track)
                                }
                            ) {
                                Icon(
                                    imageVector = if (isCurrentPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isCurrentPlaying) "Pause" else "Play",
                                    tint = if (isCurrentPlaying) HundredGramPink else HundredGramTextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
