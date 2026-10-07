package com.example.ui.call

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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import com.example.data.network.NetworkStatus
import com.example.ui.MainViewModel
import com.example.ui.ScreenDestination
import com.example.ui.components.UserAvatar
import com.example.ui.theme.HundredGramCardElevated
import com.example.ui.theme.HundredGramLikeRed
import com.example.ui.theme.HundredGramPink
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class CallState {
    CONNECTING,     // Initial handshake
    RINGING,        // घंटी बज रही है (Waiting for receiver)
    CONNECTED,      // Receiver answered -> Timer starts!
    NOT_CONNECTED,  // Failed / No answer / Network issue -> Logged as Missed Call
    ENDED           // Call finished
}

@Composable
fun CallScreen(
    viewModel: MainViewModel,
    callName: String,
    isVideo: Boolean,
    callId: String = "",
    isIncoming: Boolean = false,
    callerAvatar: String = ""
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val networkStatus by viewModel.networkStatus.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val activeCallId = remember(callId, callName) { 
        if (callId.isNotBlank()) callId else "call_${java.util.UUID.randomUUID()}" 
    }

    val callSoundManager = remember { com.example.util.CallSoundManager(context) }

    var callState by remember { mutableStateOf(if (isIncoming) CallState.RINGING else CallState.CONNECTING) }
    var notConnectedReason by remember { mutableStateOf("") }
    var isMuted by remember { mutableStateOf(false) }
    var isVideoEnabled by remember { mutableStateOf(isVideo) }
    var isSpeakerOn by remember { mutableStateOf(true) }
    var isFrontCamera by remember { mutableStateOf(true) }
    var durationSeconds by remember { mutableIntStateOf(0) }
    var hasLoggedEnd by remember { mutableStateOf(false) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val effectiveAvatar = callerAvatar.ifBlank { "" }

    // Permission launcher
    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasCameraPermission = perms[Manifest.permission.CAMERA] == true ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(Unit) {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.RECORD_AUDIO)
        }
        if (isVideo && !hasCameraPermission) {
            needed.add(Manifest.permission.CAMERA)
        }
        if (needed.isNotEmpty()) {
            callPermissionLauncher.launch(needed.toTypedArray())
        }
    }

    // Sound & Camera Management Lifecycle
    DisposableEffect(Unit) {
        onDispose {
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                cameraProvider.unbindAll()
            } catch (_: Exception) {}
            callSoundManager.stopAll()
            viewModel.repository.firestoreRepository.stopListeningToCall()
        }
    }

    fun disconnectAndCloseCall() {
        try {
            val cameraProvider = ProcessCameraProvider.getInstance(context).get()
            cameraProvider.unbindAll()
        } catch (_: Exception) {}
        callSoundManager.stopAll()
        callSoundManager.playEndCallTone()
        val wasConnected = (callState == CallState.CONNECTED)
        callState = CallState.ENDED
        val isMissed = (!wasConnected && durationSeconds == 0)
        if (!hasLoggedEnd) {
            hasLoggedEnd = true
            coroutineScope.launch {
                try {
                    viewModel.repository.firestoreRepository.endCall(
                        activeCallId,
                        if (isMissed) com.example.data.CallStatus.MISSED else com.example.data.CallStatus.ENDED
                    )
                } catch (_: Exception) {}
            }
            viewModel.onEndCall(
                callerName = callName,
                callerAvatar = effectiveAvatar,
                isVideo = isVideoEnabled,
                durationSeconds = durationSeconds,
                isOutgoing = !isIncoming,
                isMissed = isMissed
            )
        } else {
            viewModel.navigateBack()
        }
    }

    // Release camera and end call immediately on back button
    BackHandler {
        disconnectAndCloseCall()
    }

    // Audio & Ringtone state machine
    LaunchedEffect(callState) {
        when (callState) {
            CallState.CONNECTING, CallState.RINGING -> {
                if (isIncoming) {
                    callSoundManager.startIncomingRingtone()
                } else {
                    callSoundManager.startOutgoingRingtone()
                }
            }
            CallState.CONNECTED -> {
                callSoundManager.playConnectedTone()
            }
            CallState.NOT_CONNECTED, CallState.ENDED -> {
                callSoundManager.playEndCallTone()
            }
        }
    }

    // Listen to Firestore Call Status
    LaunchedEffect(activeCallId) {
        viewModel.repository.firestoreRepository.startListeningToCall(activeCallId) { newStatus ->
            when (newStatus) {
                com.example.data.CallStatus.RINGING -> callState = CallState.RINGING
                com.example.data.CallStatus.CONNECTED -> callState = CallState.CONNECTED
                com.example.data.CallStatus.ENDED -> {
                    coroutineScope.launch {
                        disconnectAndCloseCall()
                    }
                }
                com.example.data.CallStatus.MISSED, com.example.data.CallStatus.FAILED -> {
                    callState = CallState.NOT_CONNECTED
                    notConnectedReason = "Call ended / not answered"
                }
                else -> {}
            }
        }
    }

    // Call Connection & Ringing State Machine
    LaunchedEffect(callState) {
        if (callState == CallState.CONNECTING) {
            // Check network
            if (networkStatus == NetworkStatus.Unavailable || networkStatus == NetworkStatus.Lost) {
                callState = CallState.NOT_CONNECTED
                notConnectedReason = "Call not connected • No internet connection (इंटरनेट नहीं है)"
                if (!hasLoggedEnd) {
                    hasLoggedEnd = true
                    viewModel.onEndCall(callerName = callName, callerAvatar = effectiveAvatar, isVideo = isVideoEnabled, durationSeconds = 0, isOutgoing = !isIncoming, isMissed = true)
                }
                return@LaunchedEffect
            }
            delay(1500)
            if (callState == CallState.CONNECTING) {
                callState = CallState.RINGING
            }
        } else if (callState == CallState.RINGING && !isIncoming) {
            // Realistic outgoing call ringing: rings until answered or times out after 25s
            var ringTime = 0
            while (ringTime < 25 && callState == CallState.RINGING) {
                delay(1000)
                ringTime++
                if (networkStatus == NetworkStatus.Unavailable || networkStatus == NetworkStatus.Lost) {
                    callState = CallState.NOT_CONNECTED
                    notConnectedReason = "Call not connected • Network disconnected (नेटवर्क कट गया)"
                    if (!hasLoggedEnd) {
                        hasLoggedEnd = true
                        coroutineScope.launch { viewModel.repository.firestoreRepository.endCall(activeCallId, com.example.data.CallStatus.FAILED) }
                        viewModel.onEndCall(callerName = callName, callerAvatar = effectiveAvatar, isVideo = isVideoEnabled, durationSeconds = 0, isOutgoing = true, isMissed = true)
                    }
                    return@LaunchedEffect
                }
            }
            if (callState == CallState.RINGING) {
                // Ring timeout - receiver didn't pick up
                callState = CallState.NOT_CONNECTED
                notConnectedReason = "No Answer • User is busy (यूज़र ने कॉल नहीं उठाया)"
                if (!hasLoggedEnd) {
                    hasLoggedEnd = true
                    coroutineScope.launch { viewModel.repository.firestoreRepository.endCall(activeCallId, com.example.data.CallStatus.MISSED) }
                    viewModel.onEndCall(callerName = callName, callerAvatar = effectiveAvatar, isVideo = isVideoEnabled, durationSeconds = 0, isOutgoing = true, isMissed = true)
                }
            }
        }
    }

    // Timer ONLY runs when call is CONNECTED
    LaunchedEffect(callState) {
        if (callState == CallState.CONNECTED) {
            while (callState == CallState.CONNECTED) {
                delay(1000)
                durationSeconds++
            }
        }
    }

    // Pulse animations for ringing state
    val ringPulse = remember { Animatable(1f) }
    LaunchedEffect(callState) {
        if (callState == CallState.RINGING || callState == CallState.CONNECTING) {
            ringPulse.animateTo(
                targetValue = 1.25f,
                animationSpec = infiniteRepeatable(
                    animation = tween(900, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            ringPulse.snapTo(1f)
        }
    }

    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    val formattedDuration = String.format("%02d:%02d", minutes, seconds)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090B10))
    ) {
        if (isVideoEnabled) {
            // Video Call Surface
            Box(modifier = Modifier.fillMaxSize()) {
                if (hasCameraPermission) {
                    // Self camera view preview (Large Background or Picture-in-Picture)
                    CameraPreviewView(
                        isFrontCamera = isFrontCamera,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Camera permission missing UI
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0F172A))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(HundredGramPink.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VideocamOff,
                                    contentDescription = null,
                                    tint = HundredGramPink,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Camera Permission Required",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "वीडियो कॉल के लिए कैमरा एक्सेस देना ज़रूरी है",
                                color = Color.LightGray,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = {
                                        callPermissionLauncher.launch(
                                            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = HundredGramPink)
                                ) {
                                    Text("Allow Camera", fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { isVideoEnabled = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E384D))
                                ) {
                                    Text("Voice Only")
                                }
                            }
                        }
                    }
                }

                // Dark gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.7f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )

                // PIP Remote/Receiver preview frame in top right
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 48.dp, end = 16.dp)
                        .size(width = 110.dp, height = 160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(2.dp, if (callState == CallState.CONNECTED) Color(0xFF10B981) else HundredGramPink, RoundedCornerShape(16.dp))
                        .background(Color(0xFF131722)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        UserAvatar(
                            avatarUrl = callerAvatar,
                            size = 54.dp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = when (callState) {
                                CallState.CONNECTED -> "Live"
                                CallState.RINGING -> "Ringing..."
                                else -> "Connecting"
                            },
                            color = if (callState == CallState.CONNECTED) Color(0xFF10B981) else Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Top Status Header / Caller Info
        Column(
            modifier = Modifier
                .align(if (isVideoEnabled) Alignment.TopStart else Alignment.Center)
                .padding(
                    top = if (isVideoEnabled) 48.dp else 0.dp,
                    start = if (isVideoEnabled) 20.dp else 0.dp,
                    bottom = if (isVideoEnabled) 0.dp else 100.dp
                ),
            horizontalAlignment = if (isVideoEnabled) Alignment.Start else Alignment.CenterHorizontally
        ) {
            if (!isVideoEnabled) {
                // Voice Call Avatar with status pulse ring
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(if (callState == CallState.RINGING || callState == CallState.CONNECTING) ringPulse.value else 1f)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = when (callState) {
                                    CallState.CONNECTED -> listOf(Color(0xFF10B981).copy(alpha = 0.35f), Color.Transparent)
                                    CallState.NOT_CONNECTED -> listOf(HundredGramLikeRed.copy(alpha = 0.35f), Color.Transparent)
                                    CallState.RINGING -> listOf(HundredGramPink.copy(alpha = 0.45f), Color.Transparent)
                                    else -> listOf(Color(0xFF3B82F6).copy(alpha = 0.35f), Color.Transparent)
                                }
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    UserAvatar(
                        avatarUrl = callerAvatar,
                        size = 120.dp
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            Text(
                text = callName,
                color = Color.White,
                fontSize = if (isVideoEnabled) 22.sp else 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Dynamic Status Badge
            when (callState) {
                CallState.CONNECTING -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Connecting... (कॉल जुड़ रहा है...)",
                            color = Color(0xFF60A5FA),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                CallState.RINGING -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Ringing",
                            tint = HundredGramPink,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Ringing... (घंटी बज रही है...)",
                            color = HundredGramPink,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                CallState.CONNECTED -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Text(
                            text = "$formattedDuration • Connected (कॉल चालू है)",
                            color = Color(0xFF10B981),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Voice Audio Frequency Bouncing Bars
                    if (!isVideoEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        VoiceAudioWaveVisualizer(isMuted = isMuted)
                    }
                }
                CallState.NOT_CONNECTED -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallMissed,
                                contentDescription = "Missed",
                                tint = HundredGramLikeRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Call Not Connected (कॉल कनेक्ट नहीं हुआ)",
                                color = HundredGramLikeRed,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (notConnectedReason.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = notConnectedReason,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                }
                CallState.ENDED -> {
                    Text(
                        text = "Call Ended (कॉल समाप्त)",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 15.sp
                    )
                }
            }
        }


        // Bottom Call Action Controls Bar
        if (callState == CallState.NOT_CONNECTED) {
            // Reconnect or Return Controls
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 40.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        // Redial / Retry
                        durationSeconds = 0
                        hasLoggedEnd = false
                        notConnectedReason = ""
                        callState = CallState.CONNECTING
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Redial (पुनः कॉल करें)", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        disconnectAndCloseCall()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Close (बंद करें)", color = Color.White)
                }
            }
        } else {
            // Standard In-Call Control Pill (Mute, Speaker, Video, Camera Switch, End)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 36.dp)
                    .clip(RoundedCornerShape(36.dp))
                    .background(Color(0xFF1E2433).copy(alpha = 0.95f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(36.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute / Unmute Mic
                    IconButton(
                        onClick = { isMuted = !isMuted },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (isMuted) Color.White else Color(0xFF2E384D))
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mute",
                            tint = if (isMuted) Color.Black else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Video Toggle
                    IconButton(
                        onClick = { isVideoEnabled = !isVideoEnabled },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (!isVideoEnabled) Color.White else Color(0xFF2E384D))
                    ) {
                        Icon(
                            imageVector = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            contentDescription = "Video",
                            tint = if (!isVideoEnabled) Color.Black else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Flip Camera (Only when video is enabled)
                    if (isVideoEnabled) {
                        IconButton(
                            onClick = { isFrontCamera = !isFrontCamera },
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2E384D))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Switch Camera",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Speaker Toggle
                    IconButton(
                        onClick = { isSpeakerOn = !isSpeakerOn },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (isSpeakerOn) Color(0xFF2E384D) else Color.White)
                    ) {
                        Icon(
                            imageVector = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Speaker",
                            tint = if (isSpeakerOn) Color.White else Color.Black,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // End Call Button
                    IconButton(
                        onClick = {
                            disconnectAndCloseCall()
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(HundredGramLikeRed)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End Call",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CameraPreviewView(
    isFrontCamera: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(Unit) {
        onDispose {
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                cameraProvider.unbindAll()
            } catch (_: Exception) {}
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val cameraSelector = if (isFrontCamera) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                } catch (e: Exception) {
                    // Fallback handled safely
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val cameraSelector = if (isFrontCamera) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                } catch (e: Exception) {}
            }, ContextCompat.getMainExecutor(context))
        },
        modifier = modifier
    )
}

@Composable
fun VoiceAudioWaveVisualizer(isMuted: Boolean) {
    val heights = remember {
        listOf(
            Animatable(8f),
            Animatable(20f),
            Animatable(36f),
            Animatable(16f),
            Animatable(28f),
            Animatable(12f)
        )
    }

    LaunchedEffect(isMuted) {
        if (!isMuted) {
            heights.forEachIndexed { i, anim ->
                launch {
                    anim.animateTo(
                        targetValue = (12 + (i * 7) % 30).toFloat(),
                        animationSpec = infiniteRepeatable(
                            animation = tween(400 + i * 80, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                }
            }
        } else {
            heights.forEach { anim ->
                anim.snapTo(4f)
            }
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEach { anim ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(anim.value.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isMuted) Color.Gray else Color(0xFF10B981))
            )
        }
    }
}
