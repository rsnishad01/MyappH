package com.example.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.NotificationEntity
import com.example.data.NotificationType
import com.example.data.PostEntity
import com.example.ui.MainViewModel
import com.example.ui.ScreenDestination
import com.example.ui.feed.FullScreenPostDialog
import com.example.ui.profile.SoundSettingsDialog
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(viewModel: MainViewModel) {
    val notifications by viewModel.notifications.collectAsState()
    val posts by viewModel.posts.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    var selectedTab by remember { mutableStateOf("All") }

    var activePostDetail by remember { mutableStateOf<PostEntity?>(null) }
    var notificationDetailMessage by remember { mutableStateOf<String?>(null) }
    var showSoundSettingsDialog by remember { mutableStateOf(false) }

    val filteredNotifications = remember(notifications, selectedTab) {
        when (selectedTab) {
            "Follows" -> notifications.filter { it.type == NotificationType.FOLLOW }
            "Likes & Comments" -> notifications.filter { it.type == NotificationType.LIKE || it.type == NotificationType.COMMENT }
            "Mentions & Tags" -> notifications.filter { it.type == NotificationType.MENTION || it.type == NotificationType.TAG }
            "Alerts" -> notifications.filter { it.type == NotificationType.SYSTEM_ALERT }
            else -> notifications
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HundredGramDarkBackground)
    ) {
        // App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Notification Center (सूचनाएं)",
                    color = HundredGramTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            navigationIcon = {
                IconButton(onClick = { viewModel.navigateTo(ScreenDestination.Feed) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = HundredGramTextPrimary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = HundredGramCardBackground),
            actions = {
                IconButton(onClick = { showSoundSettingsDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Sound Settings",
                        tint = HundredGramPink
                    )
                }
                IconButton(onClick = { viewModel.clearNotifications() }) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear All",
                        tint = HundredGramTextSecondary
                    )
                }
            }
        )

        // Horizontal Category Scroll
        ScrollableTabRow(
            selectedTabIndex = when (selectedTab) {
                "All" -> 0
                "Follows" -> 1
                "Likes & Comments" -> 2
                "Mentions & Tags" -> 3
                else -> 4
            },
            containerColor = HundredGramCardBackground,
            contentColor = HundredGramPink,
            edgePadding = 16.dp,
            divider = { Divider(color = HundredGramDivider) }
        ) {
            listOf("All", "Follows", "Likes & Comments", "Mentions & Tags", "Alerts").forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = {
                        Text(
                            text = tab,
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }
        }

        // Notification List
        if (filteredNotifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "📭",
                        fontSize = 50.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No notifications yet in this category.",
                        color = HundredGramTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "जब भी कोई लाइक, कमेंट या फॉलो करेगा, आपको तुरंत यहाँ दिखाई देगा।",
                        color = HundredGramTextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredNotifications) { item ->
                    NotificationRow(
                        notification = item,
                        onClick = {
                            if (item.type == NotificationType.FOLLOW) {
                                val targetUserId = item.fromUserId.ifBlank { item.relatedId }
                                if (targetUserId.isNotBlank()) {
                                    viewModel.navigateTo(ScreenDestination.CreatorProfile(targetUserId))
                                } else if (item.fromUser.isNotBlank()) {
                                    viewModel.navigateTo(ScreenDestination.CreatorProfile(item.fromUser))
                                } else {
                                    notificationDetailMessage = "${item.title}\n\n${item.message}"
                                }
                            } else {
                                val matchedPost = posts.find { it.id == item.relatedId }
                                if (matchedPost != null) {
                                    activePostDetail = matchedPost
                                } else if (item.fromUserId.isNotBlank()) {
                                    viewModel.navigateTo(ScreenDestination.CreatorProfile(item.fromUserId))
                                } else {
                                    notificationDetailMessage = "${item.title}\n\n${item.message}"
                                }
                            }
                        },
                        onAvatarClick = {
                            val targetUserId = item.fromUserId.ifBlank { item.relatedId }
                            if (targetUserId.isNotBlank()) {
                                viewModel.navigateTo(ScreenDestination.CreatorProfile(targetUserId))
                            } else if (item.fromUser.isNotBlank()) {
                                viewModel.navigateTo(ScreenDestination.CreatorProfile(item.fromUser))
                            }
                        }
                    )
                }
            }
        }
    }

    activePostDetail?.let { post ->
        FullScreenPostDialog(
            post = post,
            onDismiss = { activePostDetail = null },
            viewModel = viewModel,
            currentUser = currentUser
        )
    }

    notificationDetailMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { notificationDetailMessage = null },
            containerColor = HundredGramCardBackground,
            title = { Text("Notification Content", color = HundredGramTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text(msg, color = HundredGramTextPrimary, fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = { notificationDetailMessage = null }) {
                    Text("Close", color = HundredGramPink)
                }
            }
        )
    }

    if (showSoundSettingsDialog) {
        SoundSettingsDialog(onDismiss = { showSoundSettingsDialog = false })
    }
}

@Composable
fun NotificationRow(
    notification: NotificationEntity, 
    onClick: () -> Unit,
    onAvatarClick: (() -> Unit)? = null
) {
    val indicatorColor = when (notification.type) {
        NotificationType.FOLLOW -> Color(0xFF3B82F6) // Blue
        NotificationType.LIKE -> Color(0xFFEF4444) // Red
        NotificationType.COMMENT -> Color(0xFF10B981) // Green
        NotificationType.MENTION -> Color(0xFF8B5CF6) // Purple
        NotificationType.TAG -> Color(0xFFF59E0B) // Amber
        NotificationType.SYSTEM_ALERT -> HundredGramPink
        NotificationType.MESSAGE -> Color(0xFF06B6D4) // Cyan
        NotificationType.SHARE -> Color(0xFF8B5CF6) // Indigo
    }

    val emoji = when (notification.type) {
        NotificationType.FOLLOW -> "👤"
        NotificationType.LIKE -> "❤️"
        NotificationType.COMMENT -> "💬"
        NotificationType.MENTION -> "🏷️"
        NotificationType.TAG -> "📌"
        NotificationType.SYSTEM_ALERT -> "🚨"
        NotificationType.MESSAGE -> "✉️"
        NotificationType.SHARE -> "↗️"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = HundredGramCardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left avatar or System Alert Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(HundredGramCardElevated)
                    .then(if (onAvatarClick != null) Modifier.clickable(onClick = onAvatarClick) else Modifier),
                contentAlignment = Alignment.Center
            ) {
                if (notification.fromUserAvatar.isNotBlank()) {
                    AsyncImage(
                        model = notification.fromUserAvatar,
                        contentDescription = "Sender Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(text = emoji, fontSize = 20.sp)
                }

                // Small badge indicator on the avatar bottom-right
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(indicatorColor)
                        .align(Alignment.BottomEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 9.sp)
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text info
            Column(modifier = Modifier.weight(1f)) {
                val authorName = if (notification.fromUser.isNotBlank()) "@${notification.fromUser}" else "Someone"
                val displayText = if (notification.message.startsWith("@")) {
                    notification.message
                } else {
                    "$authorName ${notification.message}"
                }

                Text(
                    text = displayText,
                    color = HundredGramTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = notification.title.ifBlank { "Notification" },
                    color = HundredGramTextTertiary,
                    fontSize = 11.sp
                )
            }

            // Thumbnail on the right if present
            if (notification.relatedThumbnail.isNotBlank()) {
                Spacer(modifier = Modifier.width(10.dp))
                AsyncImage(
                    model = notification.relatedThumbnail,
                    contentDescription = "Related Media",
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(HundredGramCardElevated),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
