package com.example.ui.stories

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.data.StoryEntity
import com.example.ui.MainViewModel
import com.example.ui.ScreenDestination
import com.example.ui.components.UserAvatar
import com.example.ui.components.AnimatedLoadingDots
import com.example.ui.theme.HundredGramDarkBackground
import com.example.ui.theme.HundredGramLikeRed
import com.example.ui.theme.HundredGramPink
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val STORY_DURATION_MS = 5500L

@Composable
fun StoryViewerScreen(
    viewModel: MainViewModel,
    initialStoryIndex: Int = 0,
    userIdFilter: String? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allStories by viewModel.stories.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val stories = remember(allStories, userIdFilter) {
        if (userIdFilter != null) {
            val userStories = allStories.filter { it.userId == userIdFilter || it.username == userIdFilter }
            if (userStories.isNotEmpty()) userStories else allStories
        } else {
            allStories
        }
    }

    if (stories.isEmpty()) {
        LaunchedEffect(Unit) {
            viewModel.navigateTo(ScreenDestination.Feed)
        }
        return
    }

    val validInitialIndex = initialStoryIndex.coerceIn(0, stories.size - 1)
    val pagerState = rememberPagerState(
        initialPage = validInitialIndex,
        pageCount = { stories.size }
    )

    val likedStories = remember { mutableStateMapOf<String, Boolean>() }
    val savedStories = remember { mutableStateMapOf<String, Boolean>() }
    var storyToDelete by remember { mutableStateOf<StoryEntity?>(null) }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) { page ->
        val currentStory = stories[page]
        val isOwner = currentStory.userId == currentUser.userId || (currentUser.username.isNotBlank() && currentStory.username == currentUser.username)

        SingleStoryView(
            story = currentStory,
            pageIndex = page,
            totalCount = stories.size,
            isCurrentPage = pagerState.currentPage == page,
            isLiked = likedStories[currentStory.id] == true,
            isSaved = savedStories[currentStory.id] == true,
            isOwner = isOwner,
            onAddStoryClick = {
                viewModel.navigateTo(ScreenDestination.Camera)
            },
            onDeleteClick = {
                storyToDelete = currentStory
            },
            onLikeToggle = {
                val newStatus = !(likedStories[currentStory.id] ?: false)
                likedStories[currentStory.id] = newStatus
                Toast.makeText(
                    context,
                    if (newStatus) "Story Liked! (स्टोरी पसंद आई ❤️)" else "Unliked",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onSaveToggle = {
                val newStatus = !(savedStories[currentStory.id] ?: false)
                savedStories[currentStory.id] = newStatus
                Toast.makeText(
                    context,
                    if (newStatus) "Saved to bookmarks! (स्टोरी सेव कर ली गई)" else "Removed from saved",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onShare = {
                com.example.util.ShareManager.shareStory(context, currentStory)
            },
            onSendReply = { text ->
                viewModel.onSendChatMessage("t_direct", "@${currentStory.username} [Story Reply]: $text")
                Toast.makeText(
                    context,
                    "Reply sent to @${currentStory.username} (कमेंट भेजा गया)",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onNext = {
                if (page < stories.size - 1) {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(page + 1)
                    }
                } else {
                    viewModel.navigateTo(ScreenDestination.Feed)
                }
            },
            onPrevious = {
                if (page > 0) {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(page - 1)
                    }
                }
            },
            onClose = {
                viewModel.navigateTo(ScreenDestination.Feed)
            }
        )
    }

    // Delete Story Confirmation Dialog
    storyToDelete?.let { story ->
        AlertDialog(
            onDismissRequest = { storyToDelete = null },
            containerColor = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Delete Story? (स्टोरी हटाएं?)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this story? This action cannot be undone.\n(क्या आप वाकई इस स्टोरी को हटाना चाहते हैं?)",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onDeleteStory(story.id)
                        storyToDelete = null
                        Toast.makeText(context, "Story deleted (स्टोरी हटा दी गई)", Toast.LENGTH_SHORT).show()
                        if (stories.size <= 1) {
                            viewModel.navigateTo(ScreenDestination.Feed)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HundredGramLikeRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { storyToDelete = null }
                ) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }
}

@Composable
fun SingleStoryView(
    story: StoryEntity,
    pageIndex: Int,
    totalCount: Int,
    isCurrentPage: Boolean,
    isLiked: Boolean,
    isSaved: Boolean,
    isOwner: Boolean = false,
    onAddStoryClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onLikeToggle: () -> Unit,
    onSaveToggle: () -> Unit,
    onShare: () -> Unit,
    onSendReply: (String) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClose: () -> Unit
) {
    val progress = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    var isPaused by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }

    // Automatic progress timer when page is actively viewed
    LaunchedEffect(isCurrentPage, isPaused) {
        if (isCurrentPage) {
            if (!isPaused) {
                val remainingRatio = 1f - progress.value
                val remainingTime = (remainingRatio * STORY_DURATION_MS).toLong().coerceAtLeast(0L)

                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = remainingTime.toInt(),
                        easing = LinearEasing
                    )
                )

                if (progress.value >= 1f) {
                    onNext()
                }
            }
        } else {
            progress.snapTo(0f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(pageIndex) {
                detectTapGestures(
                    onPress = {
                        isPaused = true
                        tryAwaitRelease()
                        isPaused = false
                    },
                    onTap = { offset ->
                        if (offset.x < size.width * 0.35f) {
                            // Tap left: previous story or restart progress
                            if (progress.value > 0.25f) {
                                coroutineScope.launch { progress.snapTo(0f) }
                            } else {
                                onPrevious()
                            }
                        } else {
                            // Tap right: next story
                            onNext()
                        }
                    }
                )
            }
    ) {
        // Story Media Layer with graceful fallback to prevent any black box
        if (story.mediaUrl.isNotBlank()) {
            val safeMediaUrl = remember(story.mediaUrl) {
                com.example.util.ImageUtils.getSafeImageModel(story.mediaUrl)
            }
            SubcomposeAsyncImage(
                model = coil.request.ImageRequest.Builder(LocalContext.current)
                    .data(safeMediaUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = story.caption.ifBlank { "Story by @${story.username}" },
                contentScale = ContentScale.Crop,
                loading = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = HundredGramPink,
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Text story fallback
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF6A11CB),
                                Color(0xFF2575FC)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    UserAvatar(avatarUrl = story.userAvatarUrl, size = 80.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = story.caption.ifBlank { "Story by @${story.username}" },
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Top Gradient Shadow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.75f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Bottom Gradient Shadow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // Top Overlay: Segmented Progress Bars and User Info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 22.dp, start = 12.dp, end = 12.dp)
        ) {
            // Segment progress bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("story_progress_bar_row"),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (i in 0 until totalCount) {
                    val segmentProgress = when {
                        i < pageIndex -> 1f
                        i == pageIndex -> progress.value
                        else -> 0f
                    }
                    LinearProgressIndicator(
                        progress = { segmentProgress },
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.35f),
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // User Info & Time stamp
            val timeText = remember(story.timestamp) {
                val diffMinutes = (System.currentTimeMillis() - story.timestamp) / (1000 * 60)
                when {
                    diffMinutes < 1 -> "Just now"
                    diffMinutes < 60 -> "${diffMinutes}m"
                    diffMinutes < 1440 -> "${diffMinutes / 60}h"
                    else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(story.timestamp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(avatarUrl = story.userAvatarUrl, size = 36.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = story.username,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• $timeText",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                if (isOwner) {
                    IconButton(
                        onClick = onAddStoryClick,
                        modifier = Modifier.testTag("add_another_story_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add another story",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.testTag("delete_story_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Story",
                            tint = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("close_story_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Story",
                        tint = Color.White
                    )
                }
            }
        }

        // Caption overlay if provided
        if (story.caption.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, end = 16.dp, bottom = 86.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = story.caption,
                    color = Color.White,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Bottom Action Bar: Reply Input, Like, Save, Share
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reply / Comment TextField
            OutlinedTextField(
                value = replyText,
                onValueChange = { replyText = it },
                placeholder = {
                    Text(
                        text = "Reply / Comment...",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.45f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("story_reply_input")
            )

            // Like Story Button
            IconButton(
                onClick = onLikeToggle,
                modifier = Modifier.testTag("story_like_button")
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like Story",
                    tint = if (isLiked) HundredGramLikeRed else Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Save Story to Bookmarks Button
            IconButton(
                onClick = onSaveToggle,
                modifier = Modifier.testTag("story_save_button")
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Save Story",
                    tint = if (isSaved) HundredGramPink else Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Share Story Button
            IconButton(
                onClick = onShare,
                modifier = Modifier.testTag("story_share_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share Story",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Send Reply Button (when text is typed)
            if (replyText.isNotBlank()) {
                IconButton(
                    onClick = {
                        onSendReply(replyText)
                        replyText = ""
                    },
                    modifier = Modifier.testTag("story_send_reply_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send Reply",
                        tint = HundredGramPink,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
