package com.example.ui.reels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.UserAvatar
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import android.content.Intent
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.data.ReelVideo
import com.example.util.ShareManager
import com.example.ui.MainViewModel
import com.example.ui.ScreenDestination
import com.example.ui.feed.CommentsBottomSheet
import com.example.ui.components.UserAvatar
import com.example.ui.components.LikesListBottomSheet
import com.example.ui.components.AnimatedLoadingDots
import com.example.ui.theme.HundredGramCardElevated
import com.example.ui.theme.HundredGramLikeRed
import com.example.ui.theme.HundredGramPink
import com.example.ui.theme.HundredGramTextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelsScreen(viewModel: MainViewModel) {
    val reels by viewModel.reels.collectAsState()
    val selectedReelForPlayer by viewModel.selectedReelForPlayer.collectAsState()
    val playbackInfo by viewModel.audioPlaybackInfo.collectAsState()
    val commentsMap by viewModel.comments.collectAsState()
    var activeCommentReelId by remember { mutableStateOf<String?>(null) }
    var activeShareReel by remember { mutableStateOf<ReelVideo?>(null) }
    var reelForLikesList by remember { mutableStateOf<ReelVideo?>(null) }
    val context = LocalContext.current
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val totalPages = if (reels.size >= 3) {
        reels.size + (reels.size / 3)
    } else {
        reels.size
    }

    val pagerState = rememberPagerState(pageCount = { totalPages })

    // Jump to clicked reel if requested from Home feed / Profile
    LaunchedEffect(selectedReelForPlayer, reels) {
        val targetId = selectedReelForPlayer
        if (!targetId.isNullOrBlank() && reels.isNotEmpty()) {
            val targetIndex = reels.indexOfFirst { it.id == targetId }
            if (targetIndex >= 0) {
                val targetPage = targetIndex + (targetIndex / 3)
                if (targetPage < totalPages) {
                    pagerState.scrollToPage(targetPage)
                }
            }
            viewModel.selectedReelForPlayer.value = null
        }
    }

    // Auto-play original audio of the active reel on page change
    LaunchedEffect(pagerState.currentPage) {
        val page = pagerState.currentPage
        val isAdPage = page > 0 && page % 3 == 0
        if (!isAdPage && reels.isNotEmpty()) {
            val reelIndex = page - (page / 3)
            if (reelIndex in reels.indices) {
                val activeReel = reels[reelIndex]
                viewModel.playReelAudio(activeReel)
                viewModel.recordReelView(activeReel.id)
            }
        } else {
            viewModel.stopAudio()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopAudio()
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            coroutineScope.launch {
                isRefreshing = true
                viewModel.refreshFeed()
                delay(600)
                isRefreshing = false
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        if (reels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0D0F14))
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(HundredGramPink.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = HundredGramPink,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Reels Yet",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Share short video clips with music and trending audio.",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = { viewModel.navigateTo(ScreenDestination.CreateReel) },
                        colors = ButtonDefaults.buttonColors(containerColor = HundredGramPink),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Your First Reel", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                val isAdPage = page > 0 && page % 3 == 0
                
                // After every 3 reels -> 1 Video Ad (AdMob)
                if (isAdPage) {
                    val activity = context as? android.app.Activity
                    SponsoredAdReelItem(
                        adUnitId = "ca-app-pub-6058721301027431/1195539816",
                        onAdClicked = {
                            if (activity != null) {
                                com.example.util.AdsManager.showReelVideoAd(activity) {
                                    Toast.makeText(context, "Sponsored Video Ad Completed", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Opening Sponsored Reel Video Ad...", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                } else {
                    val adCount = page / 3
                    val reelIndex = page - adCount
                    if (reelIndex in reels.indices) {
                        val reel = reels[reelIndex]
                        ReelItem(
                            reel = reel,
                            isMuted = playbackInfo.isMuted,
                            onLike = { viewModel.onLikeReel(reel.id) },
                            onLikesClick = { reelForLikesList = reel },
                            onComment = { activeCommentReelId = reel.id },
                            onBookmark = { viewModel.onBookmarkReel(reel.id) },
                            onShare = { activeShareReel = reel },
                            onMuteToggle = { viewModel.toggleMuteAudio() },
                            onAuthorClick = { viewModel.navigateTo(ScreenDestination.CreatorProfile(reel.authorId)) },
                            onAudioClick = { viewModel.navigateTo(ScreenDestination.SongDetail(reel.audioTitle, reel.audioArtist, reel.videoUrl.ifBlank { reel.thumbnailUri }, reel.id)) },
                            onTogglePlayAudio = { viewModel.togglePlayPauseAudio() },
                            viewModel = viewModel
                        )
                    }
                }
            }

            // Top Create Reel Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reels",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = { viewModel.navigateTo(ScreenDestination.CreateReel) },
                    colors = ButtonDefaults.buttonColors(containerColor = HundredGramPink),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("Create +", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Comments Sheet
            activeCommentReelId?.let { reelId ->
                val reelComments = commentsMap[reelId] ?: emptyList()
                CommentsBottomSheet(
                    comments = reelComments,
                    onDismiss = { activeCommentReelId = null },
                    onSendComment = { text -> viewModel.onAddComment(reelId, text) }
                )
            }

            // Reel Share & Options Sheet
            activeShareReel?.let { reel ->
                ReelShareBottomSheet(
                    reel = reel,
                    onDismiss = { activeShareReel = null },
                    onCopyLink = {
                        ShareManager.copyToClipboard(
                            context = context,
                            text = "https://hundredgram.app/reel/${reel.id}",
                            label = "Reel Link",
                            toastMessage = "Reel link copied! (रील लिंक कॉपी हो गया)"
                        )
                        activeShareReel = null
                    },
                    onShareSystem = {
                        ShareManager.shareReel(context, reel)
                        viewModel.onShareReel(reel)
                        activeShareReel = null
                    },
                    onBookmark = {
                        viewModel.onBookmarkReel(reel.id)
                        Toast.makeText(context, if (reel.isSaved) "Removed from saved" else "Saved to collection", Toast.LENGTH_SHORT).show()
                        activeShareReel = null
                    },
                    onExtractAudio = {
                        viewModel.extractAndUseReelAudio(reel)
                        activeShareReel = null
                    },
                    onReport = {
                        viewModel.reportContent(reel.id, "Reel", reel.authorUsername, "Inappropriate Content")
                        Toast.makeText(context, "Report submitted (रिपोर्ट दर्ज कर ली गई)", Toast.LENGTH_SHORT).show()
                        activeShareReel = null
                    }
                )
            }

            // Likes list bottom sheet
            reelForLikesList?.let { reel ->
                com.example.ui.components.LikesListBottomSheet(
                    postAuthorId = reel.authorId,
                    likesCount = reel.likesCount,
                    isLikedByMe = reel.isLiked,
                    onDismiss = { reelForLikesList = null },
                    viewModel = viewModel
                )
            }
        }
    }
}
}

@Composable
fun ReelItem(
    reel: ReelVideo,
    isMuted: Boolean,
    onLike: () -> Unit,
    onLikesClick: () -> Unit = {},
    onComment: () -> Unit,
    onBookmark: () -> Unit,
    onShare: () -> Unit = {},
    onMuteToggle: () -> Unit,
    onAuthorClick: () -> Unit = {},
    onAudioClick: () -> Unit = {},
    onTogglePlayAudio: () -> Unit = {},
    viewModel: com.example.ui.MainViewModel
) {
    var isPlaying by remember { mutableStateOf(true) }
    var showPauseIndicator by remember { mutableStateOf(false) }

    // Auto-hide indicator if resumed
    LaunchedEffect(isPlaying, showPauseIndicator) {
        if (isPlaying && showPauseIndicator) {
            kotlinx.coroutines.delay(650)
            showPauseIndicator = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                isPlaying = !isPlaying
                showPauseIndicator = true
                onTogglePlayAudio()
            }
    ) {
        // Reel Video / Thumbnail Background
        val mediaToPlay = reel.videoUrl.ifBlank { reel.thumbnailUri }
        val fallbackThumb = remember(reel.thumbnailUri, reel.videoUrl, reel.id) {
            com.example.util.ImageUtils.getReelThumbnailUrl(reel.thumbnailUri, reel.videoUrl, reel.id)
        }

        if (mediaToPlay.isNotBlank()) {
            com.example.ui.components.VideoPlayer(
                videoUrl = mediaToPlay,
                isMuted = isMuted,
                isPlaying = isPlaying,
                fallbackImageUrl = fallbackThumb,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = HundredGramPink,
                        modifier = Modifier.size(42.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Buffering Reel...",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Center Play / Pause Indicator Button
        if (!isPlaying || showPauseIndicator) {
            IconButton(
                onClick = {
                    isPlaying = !isPlaying
                    onTogglePlayAudio()
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(68.dp)
                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause Reel" else "Play Reel",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        // Top-right Mute / Unmute Button
        IconButton(
            onClick = onMuteToggle,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 56.dp, end = 16.dp)
                .size(40.dp)
                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
        ) {
            Icon(
                imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                contentDescription = "Mute Toggle",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // Right Action Bar (Likes, Comments, Shares, Bookmarks, Audio)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 90.dp, end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val currentUser by viewModel.currentUser.collectAsState()
                val isCreator = reel.authorId == currentUser.userId
                var displayedLikesCount by remember(reel.likesCount, isCreator) { mutableStateOf(reel.likesCount) }

                if (!isCreator) {
                    LaunchedEffect(reel.likesCount, reel.isLiked) {
                        try {
                            val allUsers = viewModel.repository.getAllUsers()
                            val otherUsers = allUsers.filter { it.userId != currentUser.userId }
                            val itemsToTake = (reel.likesCount - (if (reel.isLiked) 1 else 0)).coerceIn(0, otherUsers.size)
                            val rawOtherLikers = otherUsers.take(itemsToTake)

                            var mutualCount = 0
                            rawOtherLikers.forEach { u ->
                                val curUserFollowsLiker = viewModel.repository.isFollowingUser(u.userId)
                                val likerFollowsCurUser = viewModel.repository.isUserFollowing(currentUser.userId, u.userId)
                                if (curUserFollowsLiker && likerFollowsCurUser) {
                                    mutualCount++
                                }
                            }
                            displayedLikesCount = mutualCount + (if (reel.isLiked) 1 else 0)
                        } catch (e: Exception) {
                            displayedLikesCount = reel.likesCount
                        }
                    }
                } else {
                    displayedLikesCount = reel.likesCount
                }

                IconButton(onClick = onLike) {
                    Icon(
                        imageVector = if (reel.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (reel.isLiked) HundredGramLikeRed else Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Text(
                    text = "$displayedLikesCount",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onLikesClick() }
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onComment) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Comment",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text("${reel.commentsCount}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            IconButton(onClick = onBookmark) {
                Icon(
                    imageVector = if (reel.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "Reel Views",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                val formattedViews = when {
                    reel.viewsCount >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", reel.viewsCount / 1_000_000.0)
                    reel.viewsCount >= 1_000 -> String.format(java.util.Locale.US, "%.1fK", reel.viewsCount / 1_000.0)
                    else -> reel.viewsCount.toString()
                }
                Text(
                    text = formattedViews,
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Audio Disc",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Bottom Left Details (Author, Caption, Audio Title)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.85f)
                .padding(start = 16.dp, bottom = 90.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Picture
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { onAuthorClick() }
                ) {
                    val currentUser by viewModel.currentUser.collectAsState()
                    val displayAvatar = if ((reel.authorId == currentUser.userId || reel.authorUsername.equals(currentUser.username, ignoreCase = true)) && currentUser.avatarUrl.isNotBlank()) currentUser.avatarUrl else reel.authorAvatarUrl
                    UserAvatar(
                        avatarUrl = displayAvatar,
                        size = 36.dp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onAuthorClick() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = reel.authorDisplayName.ifBlank { reel.authorUsername },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        // Follow / Following Button right next to creator name
                        val isFollowingMap by viewModel.isFollowingMap.collectAsState()
                        val currentUser by viewModel.currentUser.collectAsState()
                        val isSelf = reel.authorId == currentUser.userId
                        val isFollowing = isFollowingMap[reel.authorId] ?: false

                        LaunchedEffect(reel.authorId) {
                            if (!isSelf && reel.authorId.isNotBlank()) {
                                viewModel.checkIfFollowing(reel.authorId)
                            }
                        }

                        if (!isSelf) {
                            var showUnfollowDialog by remember { mutableStateOf(false) }
                            Button(
                                onClick = {
                                    if (isFollowing) {
                                        showUnfollowDialog = true
                                    } else {
                                        viewModel.toggleFollowUser(reel.authorId)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFollowing) Color.DarkGray.copy(alpha = 0.85f) else HundredGramPink
                                ),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text(
                                    text = if (isFollowing) "Following" else "Follow",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (showUnfollowDialog) {
                                AlertDialog(
                                    onDismissRequest = { showUnfollowDialog = false },
                                    title = { Text("Unfollow @${reel.authorUsername}?") },
                                    text = { Text("Are you sure you want to stop following this creator?") },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            showUnfollowDialog = false
                                            viewModel.toggleFollowUser(reel.authorId)
                                        }) {
                                            Text("Unfollow", color = Color.Red, fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showUnfollowDialog = false }) {
                                            Text("Cancel")
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = com.example.util.TimeUtils.getRelativeTimeAgo(reel.timestamp),
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        )
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.8f))
                        )
                        com.example.ui.components.PrivacyBadgePill(
                            privacyStr = reel.privacy
                        )
                    }
                }
            }

            if (reel.caption.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = reel.caption,
                    color = Color.White,
                    fontSize = 13.5.sp,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Audio track ticker
            if (reel.audioTitle.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { onAudioClick() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (reel.audioArtist.isNotBlank()) "${reel.audioTitle} • ${reel.audioArtist}" else reel.audioTitle,
                        color = Color.White,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelShareBottomSheet(
    reel: ReelVideo,
    onDismiss: () -> Unit,
    onCopyLink: () -> Unit,
    onShareSystem: () -> Unit,
    onBookmark: () -> Unit,
    onExtractAudio: () -> Unit = {},
    onReport: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1E1F29)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Share Reel",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Extract Audio / Use Audio
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        onDismiss()
                        onExtractAudio()
                    }
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Extract Audio",
                    tint = HundredGramPink,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Extract & Use Audio (ऑडियो अलग करें)",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Use this exact sound on your new reel",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }

            // Copy Link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onCopyLink() }
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy Link",
                    tint = HundredGramPink,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Copy Link (लिंक कॉपी करें)",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "https://hundredgram.app/reel/${reel.id}",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            // Share via Apps
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onShareSystem() }
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Share to other Apps (शेयर करें)",
                    color = Color.White,
                    fontSize = 15.sp
                )
            }

            // Save Reel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onBookmark() }
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (reel.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Save Reel",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = if (reel.isSaved) "Remove from Saved" else "Save Reel to Collection",
                    color = Color.White,
                    fontSize = 15.sp
                )
            }

            // Report Reel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onReport() }
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ReportProblem,
                    contentDescription = "Report",
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Report Reel (रील रिपोर्ट करें)",
                    color = Color(0xFFFF9800),
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SponsoredAdReelItem(
    adUnitId: String,
    onAdClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF31103F))
                )
            )
            .clickable { onAdClicked() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Sponsored Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF3B82F6))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "Sponsored Ad (प्रायोजित विज्ञापन)",
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Ad Hero Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF312E81), Color(0xFF4C1D95), Color(0xFF831843))
                        )
                    )
                    .border(1.5.dp, Color(0xFFE879F9).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "HundredGram Pro Creator Suite",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Boost your reels, analyze audience growth, and access pro video filters instantly with AdMob SDK integration.",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.5.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onAdClicked,
                        colors = ButtonDefaults.buttonColors(containerColor = HundredGramPink),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text("Install Now / अभी इंस्टॉल करें", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "AdUnit: $adUnitId",
                color = Color.Gray,
                fontSize = 10.sp
            )
        }
    }
}

