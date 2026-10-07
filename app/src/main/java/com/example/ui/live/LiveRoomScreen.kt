package com.example.ui.live

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.MainViewModel
import com.example.ui.ScreenDestination
import com.example.ui.components.UserAvatar
import com.example.ui.theme.HundredGramButtonGradient
import com.example.ui.theme.HundredGramCardBackground
import com.example.ui.theme.HundredGramLikeRed
import com.example.ui.theme.HundredGramPink
import com.example.ui.theme.HundredGramTextPrimary
import com.example.ui.theme.HundredGramTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class LiveComment(val username: String, val text: String)

@Composable
fun LiveRoomScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val currentUser by viewModel.currentUser.collectAsState()
    val followersCount = currentUser.followersCount
    var showFollowerCountDialog by remember(followersCount) { mutableStateOf(followersCount < 500) }

    // Stream Setup States
    var liveCaption by remember { mutableStateOf("") }
    var isLiveStarted by remember { mutableStateOf(false) }
    var isCountingDown by remember { mutableStateOf(false) }
    var countdownSeconds by remember { mutableIntStateOf(3) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_FRONT) } // Live usually starts in selfie mode

    // Comments & Viewers
    var comments by remember { mutableStateOf<List<LiveComment>>(emptyList()) }
    var commentInput by remember { mutableStateOf("") }
    var viewersCount by remember { mutableIntStateOf(1) }

    // Permissions
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
        hasCameraPermission = permissions[Manifest.permission.CAMERA] == true
        hasAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] == true
    }

    fun safelyReleaseCamera() {
        try {
            val cameraProvider = ProcessCameraProvider.getInstance(context).get()
            cameraProvider.unbindAll()
        } catch (_: Exception) {}
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

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (!hasCameraPermission) permissionsToRequest.add(Manifest.permission.CAMERA)
        if (!hasAudioPermission) permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    // Countdown Timer logic (3-2-1)
    LaunchedEffect(isCountingDown) {
        if (isCountingDown) {
            countdownSeconds = 3
            while (countdownSeconds > 0) {
                delay(1000)
                countdownSeconds -= 1
            }
            isCountingDown = false
            isLiveStarted = true
            Toast.makeText(context, "You are now LIVE!", Toast.LENGTH_SHORT).show()
        }
    }

    // Simulate incoming viewers and comments during Live
    LaunchedEffect(isLiveStarted) {
        if (isLiveStarted) {
            val sampleComments = listOf(
                LiveComment("rahul_m", "Wow bro super live! 🔥"),
                LiveComment("priya_sharma", "Best quality stream! 👌"),
                LiveComment("amit_singh", "Hii there! Where are you live from?"),
                LiveComment("riya_v", "This is so cool! 💖"),
                LiveComment("neon_gamer", "Keep going bro! #hundredgram"),
                LiveComment("surbhi_j", "Awesome caption! 😍"),
                LiveComment("rohit_verma", "Dhamakedar stream! ⛰️⛰️")
            )
            var index = 0
            while (isLiveStarted) {
                delay(4000 + (1000..4000).random().toLong()) // Random delay
                viewersCount += (1..4).random()
                if (index < sampleComments.size) {
                    comments = comments + sampleComments[index]
                    index++
                } else {
                    comments = comments + LiveComment(
                        username = "viewer_${(10..99).random()}",
                        text = listOf("Awesome!", "Nice stream!", "👋👋", "Love this!", "🔥👏").random()
                    )
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (showFollowerCountDialog) {
            AlertDialog(
                onDismissRequest = {
                    viewModel.navigateTo(ScreenDestination.Feed)
                },
                title = {
                    Text(
                        text = "लाइव प्रसारण प्रतिबंधित 🔒",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "कृपया पहले 500 फोलोवर पूरे करे",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "वर्तमान फ़ॉलोअर्स: ${currentUser.followersCount} / 500",
                            fontSize = 13.sp,
                            color = HundredGramTextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showFollowerCountDialog = false
                            viewModel.navigateTo(ScreenDestination.Feed)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HundredGramPink),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("ठीक है", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = Color(0xFF1F1D2B),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            )
        }

        // 1. ACTIVE LIVE CAMERA PREVIEW BACKGROUND
        if (hasCameraPermission) {
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
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview
                                )
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("LiveRoomScreen", "Camera bind exception: ${e.message}")
                        }
                    }, ContextCompat.getMainExecutor(context))
                }
            )
        } else {
            // Gradient fallback if permission is not granted
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF1F1D2B), Color(0xFF0F0E17))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Text("Camera Access is Required for Live Streaming", color = Color.White, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HundredGramPink)
                    ) {
                        Text("Grant Permissions")
                    }
                }
            }
        }

        // 2. DARK DIMMING SCENE FOR SETUP SCREEN (only when setup is active)
        if (!isLiveStarted && !isCountingDown) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
            )
        }

        // 3. STREAM SETUP OVERLAY (Enter caption & choose Lens before going live)
        if (!isLiveStarted && !isCountingDown) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top header controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            safelyReleaseCamera()
                            viewModel.navigateTo(ScreenDestination.Feed)
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }

                    // Camera Switch (Front/Back)
                    IconButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.Cameraswitch, contentDescription = "Switch Camera", tint = Color.White)
                    }
                }

                // Middle: Caption & Start setup dialog card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = HundredGramCardBackground.copy(alpha = 0.92f)),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        UserAvatar(avatarUrl = currentUser.avatarUrl, size = 64.dp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Prepare Your Live Broadcast",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Streaming to your followers as @${currentUser.username}",
                            color = HundredGramTextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Live Caption Input (Requirement 1)
                        OutlinedTextField(
                            value = liveCaption,
                            onValueChange = { liveCaption = it },
                            placeholder = { Text("Write a broadcast caption... (e.g. Chatting with fans! ☕)", color = Color.Gray, fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = HundredGramPink,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.3f)
                            )
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Modern "GO LIVE" Start Button (Trigger countdown)
                        Button(
                            onClick = {
                                isCountingDown = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(HundredGramButtonGradient),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "GO LIVE NOW 🎥",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. LIVE TIMER COUNTDOWN OVERLAY (3-2-1 Animation)
        if (isCountingDown) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                // Pulse Animation scale
                val infiniteTransition = rememberInfiniteTransition(label = "countdownPulse")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 0.8f,
                    targetValue = 1.3f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (countdownSeconds > 0) "$countdownSeconds" else "LIVE!",
                        color = HundredGramPink,
                        fontSize = 110.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        style = LocalTextStyle.current.copy(
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Preparing Live Broadcast...",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 5. ACTIVE LIVE STREAM HUD (when live started is true)
        if (isLiveStarted) {
            // Dim background slightly at the bottom & top for better HUD text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.5f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.65f)
                            )
                        )
                    )
            )

            // Top HUD Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, start = 14.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pulsing LIVE Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(HundredGramLikeRed)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("LIVE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Viewer Count HUD
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("$viewersCount viewers", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.weight(1f))

                // End Stream Button
                Button(
                    onClick = {
                        safelyReleaseCamera()
                        isLiveStarted = false
                        comments = emptyList()
                        viewersCount = 1
                        Toast.makeText(context, "Live Broadcast Ended", Toast.LENGTH_SHORT).show()
                        viewModel.navigateTo(ScreenDestination.Feed)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("End Broadcast", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Bottom HUD Comments and Message Sender
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(14.dp)
            ) {
                // Caption Tag overlay
                if (liveCaption.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "📌 $liveCaption",
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Scrollable Live Comments Box
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(comments) { comment ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "@${comment.username}: ",
                                    color = HundredGramPink,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = comment.text,
                                    color = Color.White,
                                    fontSize = 12.5.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Real-time Chat Comment Sender
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentInput,
                        onValueChange = { commentInput = it },
                        placeholder = { Text("Comment in live stream...", color = Color.LightGray, fontSize = 13.sp) },
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = HundredGramPink,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                            focusedContainerColor = Color.Black.copy(alpha = 0.5f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (commentInput.isNotBlank()) {
                                comments = comments + LiveComment(currentUser.username, commentInput)
                                commentInput = ""
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(HundredGramPink)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
