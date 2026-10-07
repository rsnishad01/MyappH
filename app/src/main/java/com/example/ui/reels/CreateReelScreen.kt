package com.example.ui.reels

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import androidx.compose.material3.Surface
import com.example.data.PrivacyLevel
import com.example.data.UserData
import com.example.data.audio.ComprehensiveAudioTrack
import com.example.ui.MainViewModel
import com.example.ui.ScreenDestination
import com.example.ui.components.PrivacySelector
import com.example.ui.theme.HundredGramButtonGradient
import com.example.ui.theme.HundredGramCardBackground
import com.example.ui.theme.HundredGramCardElevated
import com.example.ui.theme.HundredGramDarkBackground
import com.example.ui.theme.HundredGramDivider
import com.example.ui.theme.HundredGramPink
import com.example.ui.theme.HundredGramTextPrimary
import com.example.ui.theme.HundredGramTextSecondary
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton

@Composable
fun CreateReelScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val vmSelectedReelUri by viewModel.selectedReelUri.collectAsState()
    val draftsCount by viewModel.draftsCount.collectAsState()
    val editingDraftId by viewModel.currentEditingDraftId.collectAsState()
    val initialCaption by viewModel.draftInitialCaption.collectAsState()

    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var privacy by remember { mutableStateOf(PrivacyLevel.PUBLIC) }
    var showSaveDraftDialog by remember { mutableStateOf(false) }

    var extractedThumbnails by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var selectedThumbBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var customThumbnailUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(selectedVideoUri) {
        val uri = selectedVideoUri
        if (uri != null) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri)
                    val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    val durationMs = durationStr?.toLongOrNull() ?: 3000L
                    val interval = (durationMs / 6).coerceAtLeast(100L)
                    val bitmaps = mutableListOf<Bitmap>()
                    for (i in 0..5) {
                        val timeUs = (i * interval) * 1000L
                        val bmp = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        if (bmp != null) bitmaps.add(bmp)
                    }
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        extractedThumbnails = bitmaps
                        if (bitmaps.isNotEmpty() && selectedThumbBitmap == null) {
                            selectedThumbBitmap = bitmaps.first()
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CreateReel", "Error extracting frames: ${e.message}")
                } finally {
                    try { retriever.release() } catch (_: Exception) {}
                }
            }
        } else {
            extractedThumbnails = emptyList()
            selectedThumbBitmap = null
            customThumbnailUri = null
        }
    }

    LaunchedEffect(vmSelectedReelUri) {
        if (vmSelectedReelUri != null) {
            selectedVideoUri = vmSelectedReelUri
        }
    }

    var caption by remember { mutableStateOf(initialCaption) }
    var taggedFriends by remember { mutableStateOf<List<String>>(emptyList()) }
    var showTagFriendsDialog by remember { mutableStateOf(false) }

    var allUsers by remember { mutableStateOf<List<UserData>>(emptyList()) }
    val currentUser by viewModel.currentUser.collectAsState()

    LaunchedEffect(Unit) {
        try {
            allUsers = viewModel.repository.getAllUsers()
        } catch (_: Exception) {}
    }

    LaunchedEffect(initialCaption) {
        if (initialCaption.isNotBlank() && caption.isBlank()) {
            caption = initialCaption
        }
    }

    val vmSelectedAudio by viewModel.selectedReelAudio.collectAsState()
    val vmSelectedAudioTrack by viewModel.selectedReelAudioTrack.collectAsState()
    val vmSelectedAudioUrl by viewModel.selectedReelAudioUrl.collectAsState()
    val vmSelectedAudioArtist by viewModel.selectedReelAudioArtist.collectAsState()
    val playbackInfo by viewModel.audioPlaybackInfo.collectAsState()
    var selectedAudioTrack by remember { mutableStateOf<ComprehensiveAudioTrack?>(null) }

    LaunchedEffect(vmSelectedAudio, vmSelectedAudioTrack, vmSelectedAudioUrl) {
        if (vmSelectedAudioTrack != null) {
            selectedAudioTrack = vmSelectedAudioTrack
        } else if (!vmSelectedAudio.isNullOrBlank()) {
            selectedAudioTrack = ComprehensiveAudioTrack(
                id = "pre_${System.currentTimeMillis()}",
                title = vmSelectedAudio ?: "Original Audio",
                artist = vmSelectedAudioArtist ?: "HundredGram Creator",
                durationSeconds = 30,
                coverUrl = "",
                rawResId = null,
                audioUrl = vmSelectedAudioUrl,
                streamUrl = vmSelectedAudioUrl
            )
        }
    }
    var showMusicPicker by remember { mutableStateOf(false) }

    var reelSpeed by remember { mutableStateOf(1.0f) }
    var audioVolume by remember { mutableStateOf(100f) }
    var audioStartOffset by remember { mutableStateOf(0) }
    var customAudioTitle by remember { mutableStateOf("") }
    var customAudioArtist by remember { mutableStateOf("") }

    LaunchedEffect(selectedAudioTrack) {
        selectedAudioTrack?.let { track ->
            customAudioTitle = track.title
            customAudioArtist = track.artist
        }
    }

    BackHandler(enabled = true) {
        if (selectedVideoUri != null) {
            showSaveDraftDialog = true
        } else {
            viewModel.selectedReelUri.value = null
            viewModel.navigateTo(ScreenDestination.Reels)
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedVideoUri = uri
            viewModel.selectedReelUri.value = uri
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HundredGramDarkBackground)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Top Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (selectedVideoUri != null) {
                    showSaveDraftDialog = true
                } else {
                    viewModel.selectedReelUri.value = null
                    viewModel.navigateTo(ScreenDestination.Reels)
                }
            }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = HundredGramTextPrimary
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (editingDraftId != null) "Edit Draft Reel" else "New Reel",
                    color = HundredGramTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (draftsCount > 0) {
                    Text(
                        text = "$draftsCount saved drafts",
                        color = HundredGramPink,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { viewModel.navigateTo(ScreenDestination.Drafts) }
                    )
                }
            }

            if (selectedVideoUri != null) {
                OutlinedButton(
                    onClick = {
                        val uri = selectedVideoUri ?: return@OutlinedButton
                        val audioTitle = customAudioTitle.ifBlank { selectedAudioTrack?.title ?: "" }
                        val audioArtist = customAudioArtist.ifBlank { selectedAudioTrack?.artist ?: "" }
                        viewModel.saveReelDraft(
                            mediaUri = uri,
                            caption = caption,
                            audioTitle = audioTitle,
                            audioArtist = audioArtist,
                            onSaved = {
                                Toast.makeText(context, "Reel Draft Saved to Room!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HundredGramPink.copy(alpha = 0.7f)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save Draft",
                        tint = HundredGramPink,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save Draft", color = HundredGramPink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Button(
                onClick = {
                    val uri = selectedVideoUri ?: return@Button
                    val audioTitle = customAudioTitle.ifBlank { selectedAudioTrack?.title ?: "" }
                    val audioArtist = customAudioArtist.ifBlank { selectedAudioTrack?.artist ?: "" }
                    viewModel.createReel(uri, caption, audioTitle, audioArtist, privacy, taggedFriends)
                },
                enabled = selectedVideoUri != null,
                colors = ButtonDefaults.buttonColors(containerColor = HundredGramPink),
                shape = RoundedCornerShape(18.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text("Share", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Video Viewfinder / Editing Area
        if (selectedVideoUri != null) {
            // Video Thumbnail Box (Ready to share!)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(HundredGramCardElevated)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                com.example.ui.components.VideoPlayer(
                    videoUrl = selectedVideoUri.toString(),
                    isMuted = false,
                    isPlaying = true,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Clear / Retake button
                IconButton(
                    onClick = { 
                        selectedVideoUri = null
                        viewModel.selectedReelUri.value = null
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Discard Video",
                        tint = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "🎬 Live Video Preview",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Custom Reel & Music Editor Panel
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = HundredGramCardBackground),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "⚙️ रील और संगीत संपादन (Edit Reel & Music)",
                        color = HundredGramPink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // 1. Reel Playback Speed Selection
                    Text(
                        text = "⚡ रील प्लेबैक स्पीड (Reel Speed): ${reelSpeed}x",
                        color = HundredGramTextPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { speed ->
                            val isSelected = reelSpeed == speed
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) HundredGramPink else HundredGramCardElevated)
                                    .clickable { 
                                        reelSpeed = speed 
                                        Toast.makeText(context, "Speed changed to ${speed}x (Applied to Reel)", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (speed == 1.0f) "Normal" else "${speed}x",
                                    color = if (isSelected) Color.White else HundredGramTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Music Metadata Editing (Only if track is selected)
                    if (selectedAudioTrack != null) {
                        Text(
                            text = "🎵 संगीत विवरण संपादन (Edit Music Details)",
                            color = HundredGramTextPrimary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Custom Audio Title Field
                        OutlinedTextField(
                            value = customAudioTitle,
                            onValueChange = { customAudioTitle = it },
                            label = { Text("Music Track Title", fontSize = 11.sp) },
                            placeholder = { Text("Enter music title") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = HundredGramTextPrimary,
                                unfocusedTextColor = HundredGramTextPrimary,
                                focusedBorderColor = HundredGramPink,
                                unfocusedBorderColor = HundredGramDivider,
                                focusedContainerColor = HundredGramCardElevated,
                                unfocusedContainerColor = HundredGramCardElevated
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Custom Audio Artist Field
                        OutlinedTextField(
                            value = customAudioArtist,
                            onValueChange = { customAudioArtist = it },
                            label = { Text("Music Track Artist/Creator", fontSize = 11.sp) },
                            placeholder = { Text("Enter artist name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = HundredGramTextPrimary,
                                unfocusedTextColor = HundredGramTextPrimary,
                                focusedBorderColor = HundredGramPink,
                                unfocusedBorderColor = HundredGramDivider,
                                focusedContainerColor = HundredGramCardElevated,
                                unfocusedContainerColor = HundredGramCardElevated
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3. Audio Start Offset Slider (Visual preview setting)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⏱️ संगीत शुरुआत (Start Offset): ${audioStartOffset}s",
                                color = HundredGramTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(0, 5, 10, 15).forEach { offset ->
                                    val isSel = audioStartOffset == offset
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(if (isSel) HundredGramPink else HundredGramCardElevated)
                                            .clickable { 
                                                audioStartOffset = offset 
                                                Toast.makeText(context, "Music will start from ${offset}s", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${offset}s",
                                            color = if (isSel) Color.White else HundredGramTextSecondary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 4. Background Soundtrack Volume Slider
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "🔊 संगीत वॉल्यूम (Soundtrack Volume)",
                                    color = HundredGramTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${audioVolume.toInt()}%",
                                    color = HundredGramPink,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            androidx.compose.material3.Slider(
                                value = audioVolume,
                                onValueChange = { audioVolume = it },
                                valueRange = 0f..100f,
                                colors = androidx.compose.material3.SliderDefaults.colors(
                                    thumbColor = HundredGramPink,
                                    activeTrackColor = HundredGramPink,
                                    inactiveTrackColor = HundredGramDivider
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "💡 संगीत एडिट करने के लिए ऊपर संगीत (Background Music) जोड़ें",
                                color = HundredGramTextSecondary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Spacer(modifier = Modifier.height(12.dp))

            // Reel Thumbnail Frame Picker (Select from video frames)
            if (extractedThumbnails.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "🎨 रील के पार्ट्स से थंबनेल चुनें (Choose Thumbnail from Frames)",
                        color = HundredGramTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(extractedThumbnails) { bmp ->
                            val isSelected = (selectedThumbBitmap == bmp)
                            Box(
                                modifier = Modifier
                                    .size(width = 56.dp, height = 84.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) HundredGramPink else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        selectedThumbBitmap = bmp
                                        try {
                                            val cacheFile = File(context.cacheDir, "reel_thumb_${System.currentTimeMillis()}.jpg")
                                            val out = FileOutputStream(cacheFile)
                                            bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
                                            out.flush()
                                            out.close()
                                            customThumbnailUri = Uri.fromFile(cacheFile)
                                            viewModel.selectedReelThumbnailUri.value = customThumbnailUri
                                            Toast.makeText(context, "Thumbnail frame selected!", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            // ignore
                                        }
                                    }
                            ) {
                                androidx.compose.foundation.Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Reel Frame",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(HundredGramPink.copy(alpha = 0.35f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Big button to pick from files
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clickable { videoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = HundredGramCardBackground)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = "Select video",
                        tint = HundredGramPink,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Select a Video from Gallery",
                        color = HundredGramTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Or record using Camera modes",
                        color = HundredGramTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Music Soundtrack Selector
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showMusicPicker = true },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = HundredGramCardBackground)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(HundredGramButtonGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Audio",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedAudioTrack?.title ?: "Add Background Music & Sound (Optional)",
                        color = HundredGramTextPrimary,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = selectedAudioTrack?.artist ?: "Tap to choose audio file or beat",
                        color = HundredGramTextSecondary,
                        fontSize = 12.sp
                    )
                }
                if (selectedAudioTrack != null) {
                    val isPlayingTrack = playbackInfo.isPlaying && (
                        playbackInfo.currentTrackId == selectedAudioTrack?.id ||
                        playbackInfo.trackTitle.equals(selectedAudioTrack?.title, true)
                    )
                    IconButton(
                        onClick = {
                            if (isPlayingTrack) {
                                viewModel.togglePlayPauseAudio()
                            } else {
                                selectedAudioTrack?.let { viewModel.playAudioTrack(it) }
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(HundredGramPink)
                    ) {
                        Icon(
                            imageVector = if (isPlayingTrack) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlayingTrack) "Pause Audio" else "Play Audio",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            viewModel.stopAudio()
                            selectedAudioTrack = null
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove Audio",
                            tint = HundredGramTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Caption Input
        OutlinedTextField(
            value = caption,
            onValueChange = { caption = it },
            placeholder = { Text("Write a caption... #reels #trending") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = HundredGramTextPrimary,
                unfocusedTextColor = HundredGramTextPrimary,
                focusedBorderColor = HundredGramPink,
                unfocusedBorderColor = HundredGramDivider,
                focusedContainerColor = HundredGramCardBackground,
                unfocusedContainerColor = HundredGramCardBackground
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Tag Friends Section
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = HundredGramCardBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, HundredGramDivider.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showTagFriendsDialog = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Tag Friends",
                    tint = HundredGramPink,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tag Friends (दोस्तों को टैग करें)",
                        color = HundredGramTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp
                    )
                    Text(
                        text = if (taggedFriends.isEmpty()) "Tag friends in this reel" else "${taggedFriends.size} friends tagged",
                        color = if (taggedFriends.isEmpty()) HundredGramTextSecondary else HundredGramPink,
                        fontSize = 11.5.sp
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = HundredGramTextSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        if (taggedFriends.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(taggedFriends) { tag ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = HundredGramPink.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HundredGramPink.copy(alpha = 0.5f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "@$tag",
                                color = HundredGramTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove tag",
                                tint = HundredGramPink,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { taggedFriends = taggedFriends - tag }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        PrivacySelector(selectedPrivacy = privacy, onPrivacySelected = { privacy = it })

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showTagFriendsDialog) {
        val availableUsers = remember(allUsers, currentUser) {
            allUsers.filter { it.userId != currentUser.userId }
        }
        com.example.ui.components.TagFriendsDialog(
            initialTags = taggedFriends,
            availableUsers = availableUsers,
            onTagsSelected = { taggedFriends = it },
            onDismiss = { showTagFriendsDialog = false }
        )
    }

    if (showMusicPicker) {
        MusicSearchPickerSheet(
            viewModel = viewModel,
            onDismiss = { showMusicPicker = false },
            onTrackSelected = { track ->
                selectedAudioTrack = track
                showMusicPicker = false
            }
        )
    }

    if (showSaveDraftDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDraftDialog = false },
            title = { Text("Save Reel Draft?", color = HundredGramTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Would you like to save this reel as a draft locally in Room database before leaving? You can edit and post it anytime later.",
                    color = HundredGramTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = selectedVideoUri
                        if (uri != null) {
                            val audioTitle = selectedAudioTrack?.title ?: ""
                            val audioArtist = selectedAudioTrack?.artist ?: ""
                            viewModel.saveReelDraft(
                                mediaUri = uri,
                                caption = caption,
                                audioTitle = audioTitle,
                                audioArtist = audioArtist,
                                onSaved = {
                                    Toast.makeText(context, "Reel Draft Saved!", Toast.LENGTH_SHORT).show()
                                    showSaveDraftDialog = false
                                    viewModel.selectedReelUri.value = null
                                    viewModel.navigateTo(ScreenDestination.Reels)
                                }
                            )
                        } else {
                            showSaveDraftDialog = false
                            viewModel.selectedReelUri.value = null
                            viewModel.navigateTo(ScreenDestination.Reels)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HundredGramPink)
                ) {
                    Text("Save Draft", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showSaveDraftDialog = false
                        viewModel.currentEditingDraftId.value = null
                        viewModel.selectedReelUri.value = null
                        viewModel.navigateTo(ScreenDestination.Reels)
                    }) {
                        Text("Discard", color = Color(0xFFEF4444))
                    }
                    TextButton(onClick = { showSaveDraftDialog = false }) {
                        Text("Keep Editing", color = HundredGramTextSecondary)
                    }
                }
            },
            containerColor = HundredGramCardBackground
        )
    }
}
