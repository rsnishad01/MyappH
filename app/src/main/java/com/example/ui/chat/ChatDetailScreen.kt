package com.example.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.ChatMessageEntity
import com.example.ui.MainViewModel
import com.example.ui.ScreenDestination
import com.example.ui.components.UserAvatar
import com.example.ui.theme.HundredGramBlue
import com.example.ui.theme.HundredGramCardElevated
import com.example.ui.theme.HundredGramDarkBackground
import com.example.ui.theme.HundredGramDivider
import com.example.ui.theme.HundredGramLikeRed
import com.example.ui.theme.HundredGramPink
import com.example.ui.theme.HundredGramTextPrimary
import com.example.ui.theme.HundredGramTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatDetailScreen(
    viewModel: MainViewModel,
    threadId: String,
    recipientUsername: String,
    recipientAvatar: String
) {
    val context = LocalContext.current
    val allMessages by viewModel.chatMessages.collectAsState()
    val chatThreads by viewModel.chatThreads.collectAsState()
    val thread = chatThreads.find { it.threadId == threadId }
    val isRequest = thread?.isMessageRequest == true

    val messages = allMessages[threadId] ?: emptyList()
    var messageInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val typingMap by viewModel.typingStatus.collectAsState()
    val isOtherTyping = typingMap[threadId] == true

    val recordingState by viewModel.voiceRecordingState.collectAsState()
    val playbackState by viewModel.voicePlaybackState.collectAsState()

    // Realtime chat listening
    DisposableEffect(threadId) {
        viewModel.startListeningToChatThread(threadId)
        onDispose {
            viewModel.setTypingStatus(threadId, false)
            viewModel.stopListeningToChatThread(threadId)
            viewModel.stopVoicePlayback()
            viewModel.cancelVoiceRecording()
        }
    }

    // Auto-scroll to latest message or typing indicator
    LaunchedEffect(messages.size, isOtherTyping) {
        val totalCount = messages.size + if (isOtherTyping) 1 else 0
        if (totalCount > 0) {
            listState.animateScrollToItem(totalCount - 1)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val started = viewModel.startVoiceRecording()
            if (!started) {
                Toast.makeText(context, "Could not start voice recording", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Microphone permission required for voice message (माइक की अनुमति आवश्यक है)", Toast.LENGTH_SHORT).show()
        }
    }

    val videoCallPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val hasCamera = perms[Manifest.permission.CAMERA] == true ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (hasCamera) {
            viewModel.initiateCall(recipientUsername, threadId, isVideo = true)
        } else {
            Toast.makeText(context, "वीडियो कॉल के लिए कैमरा परमिशन अनिवार्य है (Camera permission is required for video call)", Toast.LENGTH_LONG).show()
        }
    }

    val voiceCallPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.initiateCall(recipientUsername, threadId, isVideo = false)
        } else {
            Toast.makeText(context, "वॉइस कॉल के लिए माइक्रोफ़ोन परमिशन आवश्यक है", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HundredGramDarkBackground)
    ) {
        // Chat Header with Voice & Video Call Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(HundredGramDarkBackground)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(ScreenDestination.DirectMessages) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = HundredGramTextPrimary)
            }
            UserAvatar(avatarUrl = recipientAvatar, size = 36.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recipientUsername,
                    color = HundredGramTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isOtherTyping) "typing... • • •" else "Active now",
                    color = if (isOtherTyping) HundredGramPink else Color(0xFF10B981),
                    fontSize = 11.sp,
                    fontWeight = if (isOtherTyping) FontWeight.Bold else FontWeight.Normal
                )
            }
            IconButton(onClick = {
                val hasAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                if (hasAudio) {
                    viewModel.initiateCall(recipientUsername, threadId, isVideo = false)
                } else {
                    voiceCallPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }) {
                Icon(Icons.Default.Call, contentDescription = "Voice Call", tint = HundredGramTextPrimary)
            }
            IconButton(onClick = {
                val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                val hasAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                if (hasCamera && hasAudio) {
                    viewModel.initiateCall(recipientUsername, threadId, isVideo = true)
                } else {
                    videoCallPermissionLauncher.launch(
                        arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                    )
                }
            }) {
                Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = HundredGramTextPrimary)
            }
        }

        if (isRequest) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF3B82F6).copy(alpha = 0.25f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "@$recipientUsername wants to send you a message request.",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                androidx.compose.material3.Button(
                    onClick = { viewModel.acceptMessageRequest(threadId) },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = HundredGramPink)
                ) {
                    Text("Accept", fontSize = 12.sp)
                }
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.messageId }) { message ->
                MessageBubble(
                    message = message,
                    playbackState = playbackState,
                    onPlayVoice = { viewModel.playVoiceMessage(message.messageId, message.mediaUrl) },
                    onReact = { emoji -> viewModel.onReactToMessage(threadId, message.messageId, emoji) },
                    onRetry = { viewModel.onRetrySendMessage(threadId, message.messageId) }
                )
            }

            if (isOtherTyping) {
                item(key = "typing_dots_indicator") {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        UserAvatar(avatarUrl = recipientAvatar, size = 28.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp))
                                .background(Color(0xFF1E2433))
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            TypingDotsIndicator()
                        }
                    }
                }
            }
        }

        // Voice Recording Banner or Message Input Bottom Bar
        if (recordingState.isRecording) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E2230))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { viewModel.cancelVoiceRecording() },
                    modifier = Modifier.size(36.dp).background(Color.DarkGray, CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel Recording", tint = Color.White)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(HundredGramLikeRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recording... ${String.format("%02d:%02d", recordingState.durationSeconds / 60, recordingState.durationSeconds % 60)}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                IconButton(
                    onClick = { viewModel.stopVoiceRecordingAndSend(threadId, recipientUsername, recipientAvatar, isRequest) },
                    modifier = Modifier.size(42.dp).background(HundredGramPink, CircleShape)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send Voice Message", tint = Color.White)
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { /* Attach photo */ }) {
                    Icon(Icons.Default.Image, contentDescription = "Attach Media", tint = HundredGramTextSecondary)
                }

                OutlinedTextField(
                    value = messageInput,
                    onValueChange = {
                        messageInput = it
                        viewModel.setTypingStatus(threadId, it.isNotBlank())
                    },
                    placeholder = { Text("Message...", color = HundredGramTextSecondary, fontSize = 13.sp) },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = HundredGramTextPrimary,
                        unfocusedTextColor = HundredGramTextPrimary,
                        focusedBorderColor = HundredGramPink,
                        unfocusedBorderColor = HundredGramDivider
                    ),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(6.dp))

                if (messageInput.isNotBlank()) {
                    IconButton(
                        onClick = {
                            val textToSend = messageInput
                            messageInput = ""
                            viewModel.setTypingStatus(threadId, false)
                            viewModel.onSendChatMessage(threadId, textToSend, recipientUsername = recipientUsername, recipientAvatar = recipientAvatar, isMessageRequest = isRequest)
                        }
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = HundredGramPink)
                    }
                } else {
                    // Voice Record Button
                    IconButton(
                        onClick = {
                            val perm = Manifest.permission.RECORD_AUDIO
                            if (ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED) {
                                val started = viewModel.startVoiceRecording()
                                if (!started) {
                                    Toast.makeText(context, "Could not start voice recording", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                permissionLauncher.launch(perm)
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(HundredGramPink.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Message (वॉयस मैसेज रिकॉर्ड करें)",
                            tint = HundredGramPink,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessageEntity,
    playbackState: com.example.data.audio.VoicePlaybackState,
    onPlayVoice: () -> Unit,
    onReact: (String) -> Unit,
    onRetry: () -> Unit = {}
) {
    val isOutgoing = message.isOutgoing
    val isThisPlaying = playbackState.isPlaying && playbackState.activeMessageId == message.messageId

    val messageTime = remember(message.timestamp) {
        try {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            sdf.format(Date(message.timestamp))
        } catch (e: Exception) {
            ""
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
    ) {
        if (message.isVoiceMessage) {
            // Voice Message Audio Player Bubble
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isOutgoing) 16.dp else 4.dp,
                            bottomEnd = if (isOutgoing) 4.dp else 16.dp
                        )
                    )
                    .background(if (isOutgoing) Color(0xFF6366F1) else HundredGramCardElevated)
                    .clickable { onReact("❤️") }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.width(220.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = onPlayVoice,
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isThisPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Voice Message",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${message.voiceDurationSeconds}s",
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            val progress = if (isThisPlaying && playbackState.durationMs > 0) {
                                (playbackState.currentPositionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
                            } else {
                                0f
                            }

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = Color.White,
                                trackColor = Color.White.copy(alpha = 0.3f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Time and Delivery Status Footer
                    MessageStatusFooter(
                        messageTime = messageTime,
                        isOutgoing = isOutgoing,
                        sendStatus = message.sendStatus,
                        onRetry = onRetry
                    )
                }
            }
        } else {
            // Regular Text Message Bubble
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isOutgoing) 16.dp else 4.dp,
                            bottomEnd = if (isOutgoing) 4.dp else 16.dp
                        )
                    )
                    .background(if (isOutgoing) HundredGramBlue else HundredGramCardElevated)
                    .clickable { onReact("❤️") }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.widthIn(min = 60.dp, max = 270.dp),
                    horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
                ) {
                    Text(
                        text = message.text,
                        color = Color.White,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    // Time and Delivery Status Footer
                    MessageStatusFooter(
                        messageTime = messageTime,
                        isOutgoing = isOutgoing,
                        sendStatus = message.sendStatus,
                        onRetry = onRetry
                    )
                }
            }
        }

        if (message.reaction.isNotBlank()) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .clip(CircleShape)
                    .background(HundredGramCardElevated)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(message.reaction, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun MessageStatusFooter(
    messageTime: String,
    isOutgoing: Boolean,
    sendStatus: String,
    onRetry: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.padding(top = 1.dp)
    ) {
        if (messageTime.isNotBlank()) {
            Text(
                text = messageTime,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        if (isOutgoing) {
            Spacer(modifier = Modifier.width(4.dp))
            when (sendStatus) {
                "SENT" -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = "Sent",
                            tint = Color(0xFF86EFAC), // Soft green checkmark
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Sent",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
                "FAILED" -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF991B1B).copy(alpha = 0.8f))
                            .clickable { onRetry() }
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Didn't Sent",
                            tint = Color(0xFFFCA5A5),
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "Didn't Sent",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFCA5A5)
                        )
                        Spacer(modifier = Modifier.width(1.dp))
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
                else -> {
                    // SENDING
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color.White.copy(alpha = 0.8f),
                            strokeWidth = 1.2.dp,
                            modifier = Modifier.size(9.dp)
                        )
                        Text(
                            text = "Sending...",
                            fontSize = 9.5.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TypingDotsIndicator() {
    val dot1 = remember { Animatable(0f) }
    val dot2 = remember { Animatable(0f) }
    val dot3 = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        val spec = infiniteRepeatable<Float>(
            animation = tween(durationMillis = 450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
        launch {
            dot1.animateTo(targetValue = -5f, animationSpec = spec)
        }
        launch {
            delay(120)
            dot2.animateTo(targetValue = -5f, animationSpec = spec)
        }
        launch {
            delay(240)
            dot3.animateTo(targetValue = -5f, animationSpec = spec)
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .offset(y = dot1.value.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(HundredGramPink)
        )
        Box(
            modifier = Modifier
                .offset(y = dot2.value.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(HundredGramPink.copy(alpha = 0.85f))
        )
        Box(
            modifier = Modifier
                .offset(y = dot3.value.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(HundredGramPink.copy(alpha = 0.7f))
        )
    }
}
