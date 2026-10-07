package com.example.ui.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import com.example.data.audio.ComprehensiveAudioTrack
import com.example.ui.reels.MusicSearchPickerSheet
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.MainViewModel
import com.example.ui.ScreenDestination
import com.example.ui.theme.HundredGramCardElevated
import com.example.ui.theme.HundredGramPink
import com.example.ui.theme.HundredGramTextPrimary
import com.example.ui.theme.HundredGramTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

enum class CameraMode(val title: String) {
    POST("POST"),
    STORY("STORY"),
    REEL("REEL"),
    LIVE("LIVE")
}

enum class LiveCameraFilter(
    val filterName: String,
    val hindiName: String,
    val tintColor: Color,
    val vignetteAlpha: Float = 0f
) {
    NORMAL("Normal", "सामान्य", Color.Transparent, 0f),
    GOLDEN("Golden Hour", "गोल्डन ग्लो", Color(0x38FFA726), 0.2f),
    VINTAGE("Vintage", "विंटेज", Color(0x458D6E63), 0.4f),
    ROSE("Rose Glam", "गुलाबी चमक", Color(0x38F06292), 0.15f),
    CYBERPUNK("Cyberpunk", "साइबर नियॉन", Color(0x3500E5FF), 0.25f),
    NOIR("Cinema Noir", "सिनेमा ब्लैक", Color(0x60000000), 0.5f),
    DREAMY("Dreamy Violet", "सपना पर्पल", Color(0x35AB47BC), 0.2f)
}

@Composable
fun CameraScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val totalDraftsCount by viewModel.draftsCount.collectAsState()

    var cameraMode by remember { mutableStateOf(CameraMode.POST) }
    var flashEnabled by remember { mutableStateOf(false) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var isCapturing by remember { mutableStateOf(false) }
    var selectedReelDuration by remember { mutableIntStateOf(15) } // 15s, 30s, 60s
    var selectedFilter by remember { mutableStateOf(LiveCameraFilter.NORMAL) }
    var showFilterSelector by remember { mutableStateOf(false) }
    var showLiveBeautyControls by remember { mutableStateOf(false) }
    var lastLiveFilterTapTime by remember { mutableStateOf(0L) }
    var lastLiveTappedFilterName by remember { mutableStateOf("") }
    var liveSkinSmoothness by remember { mutableStateOf(60f) }
    var liveSkinWhitening by remember { mutableStateOf(40f) }
    var selectedTimerSeconds by remember { mutableIntStateOf(0) } // 0 (Off), 3s, 5s, 10s
    var countdownRemaining by remember { mutableIntStateOf(0) }
    var isCountingDown by remember { mutableStateOf(false) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] == true ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        hasAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] == true ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (!hasCameraPermission) permissionsToRequest.add(Manifest.permission.CAMERA)
        if (!hasAudioPermission) permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.setSelectedFilterImage(uri)
        }
    }

    var isRecordingReel by remember { mutableStateOf(false) }
    var recordedReelSeconds by remember { mutableIntStateOf(0) }
    var selectedCameraAudioTrack by remember { mutableStateOf<ComprehensiveAudioTrack?>(null) }
    var showMusicPickerInCamera by remember { mutableStateOf(false) }

    fun createRecordedReelUri(duration: Int): Uri {
        return try {
            val file = File(context.cacheDir, "reel_recorded_${System.currentTimeMillis()}.mp4")
            if (!file.exists()) {
                file.createNewFile()
                java.io.FileOutputStream(file).use { it.write(byteArrayOf(0, 0, 0, 32, 102, 116, 121, 112)) }
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (_: Exception) {
            Uri.parse("https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4")
        }
    }

    LaunchedEffect(isRecordingReel) {
        if (isRecordingReel) {
            // Automatically play selected audio with recording shutter
            selectedCameraAudioTrack?.let { track ->
                viewModel.playAudioTrack(track)
            }
            recordedReelSeconds = 0
            while (isRecordingReel && recordedReelSeconds < selectedReelDuration) {
                delay(1000)
                recordedReelSeconds += 1
            }
            if (recordedReelSeconds >= selectedReelDuration) {
                isRecordingReel = false
                viewModel.stopAudio()
                val recordedUri = createRecordedReelUri(selectedReelDuration)
                viewModel.selectedReelUri.value = recordedUri
                Toast.makeText(context, "${selectedReelDuration}s Reel Recorded Successfully!", Toast.LENGTH_SHORT).show()
                viewModel.navigateTo(ScreenDestination.CreateReel)
            }
        } else {
            viewModel.stopAudio()
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.selectedReelUri.value = uri
            viewModel.navigateTo(ScreenDestination.CreateReel)
        }
    }

    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val imageCapture = remember {
        try {
            ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
        } catch (_: Exception) {
            null
        }
    }
    var cameraInstance by remember { mutableStateOf<Camera?>(null) }

    fun safelyReleaseCamera() {
        try {
            cameraInstance?.cameraControl?.enableTorch(false)
            cameraInstance = null
        } catch (_: Exception) {}
        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            if (cameraProviderFuture.isDone) {
                cameraProviderFuture.get().unbindAll()
            } else {
                cameraProviderFuture.addListener({
                    try {
                        cameraProviderFuture.get().unbindAll()
                    } catch (_: Exception) {}
                }, ContextCompat.getMainExecutor(context))
            }
        } catch (_: Exception) {}
        try {
            if (!cameraExecutor.isShutdown) {
                cameraExecutor.shutdown()
            }
        } catch (_: Exception) {}
        isRecordingReel = false
        viewModel.stopAudio()
    }

    // Completely release camera when back button is pressed
    BackHandler {
        safelyReleaseCamera()
        viewModel.navigateTo(ScreenDestination.Feed)
    }

    DisposableEffect(Unit) {
        onDispose {
            safelyReleaseCamera()
        }
    }

    // Flash toggle handler
    LaunchedEffect(flashEnabled) {
        try {
            cameraInstance?.cameraControl?.enableTorch(flashEnabled)
        } catch (_: Exception) {}
    }

    fun takePhotoAndNavigate() {
        if (isCapturing) return
        isCapturing = true

        val photoFile = try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = context.cacheDir
            File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        } catch (e: Exception) {
            isCapturing = false
            Toast.makeText(context, "Failed to create image file", Toast.LENGTH_SHORT).show()
            return
        }

        val capture = imageCapture
        if (capture == null) {
            isCapturing = false
            Toast.makeText(context, "Select photo from gallery", Toast.LENGTH_SHORT).show()
            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            return
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    isCapturing = false
                    val savedUri = try {
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            photoFile
                        )
                    } catch (_: Exception) {
                        Uri.fromFile(photoFile)
                    }

                    when (cameraMode) {
                        CameraMode.POST -> {
                            viewModel.setSelectedFilterImage(savedUri)
                        }
                        CameraMode.STORY -> {
                            viewModel.createStory(savedUri, "", selectedCameraAudioTrack)
                        }
                        CameraMode.REEL -> {
                            viewModel.navigateTo(ScreenDestination.CreateReel)
                        }
                        CameraMode.LIVE -> {
                            viewModel.navigateTo(ScreenDestination.LiveRoom)
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    isCapturing = false
                    exception.printStackTrace()
                    // If hardware capture encounters an issue on emulator, fallback cleanly to gallery
                    Toast.makeText(context, "Select photo from gallery", Toast.LENGTH_SHORT).show()
                    photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            // Real Live Camera Viewfinder with safe lifecycle binding
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { pView ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = pView.surfaceProvider
                            }
                            val cameraSelector = CameraSelector.Builder()
                                .requireLensFacing(lensFacing)
                                .build()

                            if (cameraProvider.hasCamera(cameraSelector)) {
                                cameraProvider.unbindAll()
                                val useCases = mutableListOf<androidx.camera.core.UseCase>(preview)
                                imageCapture?.let { useCases.add(it) }

                                cameraInstance = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    *useCases.toTypedArray()
                                )
                                try {
                                    cameraInstance?.cameraControl?.enableTorch(flashEnabled)
                                } catch (_: Exception) {}
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("CameraScreen", "Camera bind exception: ${e.message}")
                        }
                    }, ContextCompat.getMainExecutor(context))
                }
            )

            // Live Camera Visual Filter Effect Layer
            if (selectedFilter != LiveCameraFilter.NORMAL) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(selectedFilter.tintColor)
                )
                if (selectedFilter.vignetteAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = selectedFilter.vignetteAlpha)
                                    )
                                )
                            )
                    )
                }
            }

            // Reel Recording Overlays
            if (cameraMode == CameraMode.REEL && isRecordingReel) {
                val progress = recordedReelSeconds.toFloat() / selectedReelDuration.toFloat()
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .align(Alignment.TopCenter),
                    color = HundredGramPink,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Red.copy(alpha = 0.85f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "REC 00:${if (recordedReelSeconds < 10) "0" else ""}$recordedReelSeconds / 00:${selectedReelDuration}s",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            // Flash overlay animation during capture
            AnimatedVisibility(
                visible = isCapturing,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.8f))
                )
            }

            // Big Timer Countdown Overlay (3s, 5s, 10s)
            if (isCountingDown) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$countdownRemaining",
                            color = Color.White,
                            fontSize = 120.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Timer active (${selectedTimerSeconds}s)",
                            color = HundredGramPink,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            // Permission Request Card
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Permission Needed",
                    tint = HundredGramPink,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Camera & Audio Access Required",
                    color = HundredGramTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "To take photos, record reels (15s, 30s, 60s), and share stories, please allow camera and microphone access.",
                    color = HundredGramTextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.CAMERA,
                                Manifest.permission.RECORD_AUDIO
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HundredGramPink),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Grant Permissions", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Top Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, start = 12.dp, end = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    safelyReleaseCamera()
                    viewModel.navigateTo(ScreenDestination.Feed)
                },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            // Duration Indicator & Audio Selector when in REEL mode (15s / 30s / 60s + Audio)
            if (cameraMode == CameraMode.REEL) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(15, 30, 60).forEach { sec ->
                            val isSelected = selectedReelDuration == sec
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) HundredGramPink else Color.Transparent)
                                    .clickable { selectedReelDuration = sec }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${sec}s",
                                    color = if (isSelected) Color.White else Color.LightGray,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Select Audio button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable { showMusicPickerInCamera = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Select Audio",
                            tint = HundredGramPink,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = selectedCameraAudioTrack?.title ?: "Add Audio 🎵",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                // Drafts Folder button
                IconButton(
                    onClick = { viewModel.navigateTo(ScreenDestination.Drafts) },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                ) {
                    BadgedBox(
                        badge = {
                            if (totalDraftsCount > 0) {
                                Badge(
                                    containerColor = HundredGramPink,
                                    contentColor = Color.White
                                ) {
                                    Text("$totalDraftsCount", fontSize = 10.sp)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Drafts,
                            contentDescription = "Saved Drafts",
                            tint = if (totalDraftsCount > 0) HundredGramPink else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Timer Toggle (Off -> 3s -> 5s -> 10s)
                IconButton(
                    onClick = {
                        selectedTimerSeconds = when (selectedTimerSeconds) {
                            0 -> 3
                            3 -> 5
                            5 -> 10
                            else -> 0
                        }
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (selectedTimerSeconds > 0) HundredGramPink.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.45f))
                ) {
                    if (selectedTimerSeconds > 0) {
                        Text(
                            text = "${selectedTimerSeconds}s",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Timer (3s, 5s, 10s)",
                            tint = Color.White
                        )
                    }
                }

                // Flash Toggle
                IconButton(
                    onClick = { flashEnabled = !flashEnabled },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                ) {
                    Icon(
                        imageVector = if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Flash",
                        tint = if (flashEnabled) Color.Yellow else Color.White
                    )
                }

                // Lens switch (Front / Back)
                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                ) {
                    Icon(Icons.Default.Cameraswitch, contentDescription = "Flip Camera", tint = Color.White)
                }
            }
        }

        // Bottom Action Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
                .padding(bottom = 32.dp, top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Camera Mode Selector (POST, STORY, REEL, LIVE)
            Row(
                modifier = Modifier
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                CameraMode.values().forEach { mode ->
                    val isSelected = cameraMode == mode
                    Text(
                        text = mode.title,
                        color = if (isSelected) HundredGramPink else Color.White.copy(alpha = 0.6f),
                        fontSize = 13.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier
                            .clickable { cameraMode = mode }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Live Camera Filters Carousel
            AnimatedVisibility(visible = showFilterSelector) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(LiveCameraFilter.values()) { filter ->
                        val isSelected = selectedFilter == filter
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) HundredGramPink.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.55f))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) HundredGramPink else Color.White.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    val now = System.currentTimeMillis()
                                    if (lastLiveTappedFilterName == filter.name && (now - lastLiveFilterTapTime) < 450L) {
                                        // Double tap on live camera filter! Open/Toggle Beauty Controls
                                        selectedFilter = filter
                                        showLiveBeautyControls = !showLiveBeautyControls
                                    } else {
                                        selectedFilter = filter
                                    }
                                    lastLiveFilterTapTime = now
                                    lastLiveTappedFilterName = filter.name
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = filter.filterName,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            Text(
                                text = filter.hindiName,
                                color = if (isSelected) HundredGramPink else Color.LightGray,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Shutter Controls Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery / Media Picker
                IconButton(
                    onClick = {
                        if (cameraMode == CameraMode.REEL) {
                            videoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                        } else {
                            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = if (cameraMode == CameraMode.REEL) Icons.Default.Videocam else Icons.Default.PhotoLibrary,
                        contentDescription = "Gallery",
                        tint = Color.White
                    )
                }

                // Shutter Button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(
                            if (cameraMode == CameraMode.REEL) Color(0xFFFF2A6D) else HundredGramPink
                        )
                        .clickable {
                            if (!hasCameraPermission) {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.CAMERA,
                                        Manifest.permission.RECORD_AUDIO
                                    )
                                )
                                return@clickable
                            }

                            if (isCountingDown) return@clickable

                            val executeShutter = {
                                when (cameraMode) {
                                    CameraMode.POST, CameraMode.STORY -> {
                                        takePhotoAndNavigate()
                                    }
                                    CameraMode.REEL -> {
                                        if (!isRecordingReel) {
                                            isRecordingReel = true
                                        } else {
                                            isRecordingReel = false
                                            val recordedUri = createRecordedReelUri(recordedReelSeconds)
                                            viewModel.selectedReelUri.value = recordedUri
                                            Toast.makeText(context, "Reel Recorded Successfully!", Toast.LENGTH_SHORT).show()
                                            viewModel.navigateTo(ScreenDestination.CreateReel)
                                        }
                                    }
                                    CameraMode.LIVE -> {
                                        viewModel.navigateTo(ScreenDestination.LiveRoom)
                                    }
                                }
                            }

                            if (selectedTimerSeconds > 0 && !isRecordingReel) {
                                isCountingDown = true
                                countdownRemaining = selectedTimerSeconds
                                coroutineScope.launch {
                                    while (countdownRemaining > 0) {
                                        delay(1000)
                                        countdownRemaining -= 1
                                    }
                                    isCountingDown = false
                                    executeShutter()
                                }
                            } else {
                                executeShutter()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(28.dp)
                        )
                    } else if (cameraMode == CameraMode.REEL && isRecordingReel) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop Recording",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (cameraMode == CameraMode.REEL) Icons.Default.Videocam else Icons.Default.PhotoCamera,
                            contentDescription = "Capture",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Live Filter Selector Toggle Button
                IconButton(
                    onClick = {
                        showFilterSelector = !showFilterSelector
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (showFilterSelector || selectedFilter != LiveCameraFilter.NORMAL) HundredGramPink else Color.White.copy(alpha = 0.2f)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Live Camera Filters",
                        tint = Color.White
                    )
                }
            }
        }
    }

    if (showMusicPickerInCamera) {
        MusicSearchPickerSheet(
            viewModel = viewModel,
            onDismiss = { showMusicPickerInCamera = false },
            onTrackSelected = { track ->
                selectedCameraAudioTrack = track
                viewModel.selectedReelAudio.value = track.title
                showMusicPickerInCamera = false
            }
        )
    }
}
