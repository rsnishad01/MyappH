package com.example.ui.feed

import android.content.Intent
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import kotlinx.coroutines.delay
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.LocalOffer
import com.example.ui.components.TagFriendsDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.CommentEntity
import com.example.data.PostEntity
import com.example.data.PrivacyLevel
import com.example.data.StoryEntity
import com.example.data.UserData
import com.example.ui.MainViewModel
import androidx.compose.runtime.LaunchedEffect
import com.example.ui.ScreenDestination
import com.example.ui.components.GradientActionButton
import com.example.ui.components.PostOptionsSheet
import com.example.ui.components.SecondaryActionButton
import com.example.ui.components.UserAvatar
import com.example.ui.components.LikesListBottomSheet
import com.example.ui.components.PrivacySelector
import com.example.ui.components.AnimatedLoadingDots
import com.example.ui.theme.HundredGramBlue
import com.example.ui.theme.HundredGramBorderSubtle
import com.example.ui.theme.HundredGramButtonGradient
import com.example.ui.theme.HundredGramCardBackground
import com.example.ui.theme.HundredGramCardElevated
import com.example.ui.theme.HundredGramCardGlass
import com.example.ui.theme.HundredGramCardGradient
import com.example.ui.theme.HundredGramDarkBackground
import com.example.ui.theme.HundredGramDivider
import com.example.ui.theme.HundredGramLikeRed
import com.example.ui.theme.HundredGramPink
import com.example.ui.theme.HundredGramTextPrimary
import com.example.ui.theme.HundredGramTextSecondary
import com.example.ui.theme.InstagramStoryGradient
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.text.style.TextAlign
import com.example.util.ShareManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun sharePostSystem(context: android.content.Context, post: PostEntity, viewModel: MainViewModel? = null) {
    ShareManager.sharePost(context, post)
    viewModel?.onSharePost(post)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(viewModel: MainViewModel) {
    val posts by viewModel.posts.collectAsState()
    val stories by viewModel.stories.collectAsState()
    val commentsMap by viewModel.comments.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val reels by viewModel.reels.collectAsState()
    val followedUserIds by viewModel.followedUserIds.collectAsState()

    val combinedFeed = remember(posts, reels, followedUserIds) {
        val adaptedReels = reels.map { reel ->
            PostEntity(
                id = reel.id,
                authorId = reel.authorId,
                authorUsername = reel.authorUsername,
                authorDisplayName = reel.authorDisplayName,
                authorAvatarUrl = reel.authorAvatarUrl,
                imageUrl = com.example.util.ImageUtils.getReelThumbnailUrl(reel.thumbnailUri, reel.videoUrl, reel.id),
                mediaUrl = reel.videoUrl,
                type = "video",
                caption = "🎬 Reel: ${reel.caption} \n🎵 Music: ${reel.audioTitle} - ${reel.audioArtist}",
                likesCount = reel.likesCount,
                isLiked = reel.isLiked,
                commentsCount = reel.commentsCount,
                timestamp = reel.timestamp,
                location = "Reels",
                isSaved = reel.isSaved,
                tags = reel.tags,
                likedByUserIds = reel.likedByUserIds,
                commentedByUserIds = reel.commentedByUserIds
            )
        }
        val allItems = posts + adaptedReels
        allItems.sortedWith(
            compareByDescending<PostEntity> { post ->
                val hasFriendLiked = post.likedByUserIds.any { followedUserIds.contains(it) }
                val hasFriendCommented = post.commentedByUserIds.any { followedUserIds.contains(it) }
                val isAuthorFollowed = followedUserIds.contains(post.authorId)
                when {
                    hasFriendLiked || hasFriendCommented -> 3
                    isAuthorFollowed -> 2
                    else -> 1
                }
            }.thenByDescending { it.timestamp }
        )
    }

    var activeCommentPostId by remember { mutableStateOf<String?>(null) }
    var activeOptionsPost by remember { mutableStateOf<PostEntity?>(null) }
    var activeEditPost by remember { mutableStateOf<PostEntity?>(null) }
    var reportTargetPost by remember { mutableStateOf<PostEntity?>(null) }
    var blockTargetPost by remember { mutableStateOf<PostEntity?>(null) }
    var postForLikesList by remember { mutableStateOf<PostEntity?>(null) }
    var hiddenAdIds by remember { mutableStateOf(setOf<String>()) }
    var activeAdDetail by remember { mutableStateOf<FeedAdItem?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        com.example.util.AdsManager.loadNativeFeedAd(
            context = context,
            onAdLoaded = { /* native ad loaded and active */ },
            onAdFailed = { /* graceful fallback to sample native ads */ }
        )
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
        modifier = Modifier
            .fillMaxSize()
            .background(HundredGramDarkBackground)
    ) {
        if (combinedFeed.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Story Ring List
                StoriesRow(
                    stories = stories,
                    currentUser = currentUser,
                    onAddStoryClick = { viewModel.navigateTo(ScreenDestination.Camera) },
                    onStoryClick = { index, userIdFilter ->
                        viewModel.navigateTo(ScreenDestination.StoryViewer(index, userIdFilter))
                    }
                )

                // Ultra-Compact Suggested Creators Section
                SuggestedCreatorsSection(viewModel = viewModel)

                Divider(color = HundredGramDivider.copy(alpha = 0.4f), thickness = 0.5.dp)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Camera icon removed as requested
                        Spacer(modifier = Modifier.height(64.dp))
                        Text(
                            text = "Welcome to HundredGram",
                            color = HundredGramTextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your circle of creativity and moments.\nPull down to refresh or capture your first post!",
                            color = HundredGramTextSecondary,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { viewModel.navigateTo(ScreenDestination.Camera) },
                            colors = ButtonDefaults.buttonColors(containerColor = HundredGramPink),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("Create Post", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Posts & Reels Feed with scrollable Stories and Suggestions at the top
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Header 1: Instagram-like Story Ring List
                item(key = "feed_stories_header") {
                    StoriesRow(
                        stories = stories,
                        currentUser = currentUser,
                        onAddStoryClick = { viewModel.navigateTo(ScreenDestination.Camera) },
                        onStoryClick = { index, userIdFilter ->
                            viewModel.navigateTo(ScreenDestination.StoryViewer(index, userIdFilter))
                        }
                    )
                }

                // Suggestions Section
                item(key = "feed_suggestions_header") {
                    SuggestedCreatorsSection(viewModel = viewModel)
                }

                // Posts, Reels, and Sponsored Ads items
                itemsIndexed(combinedFeed, key = { _, post -> post.id }) { index, post ->
                    var showFullScreenDetail by remember { mutableStateOf(false) }
                    val reels by viewModel.reels.collectAsState()
                    PostCard(
                        post = post,
                        isCurrentUser = post.authorId == currentUser.userId,
                        onLike = { viewModel.onLikePost(post.id) },
                        onBookmark = { viewModel.onBookmarkPost(post.id) },
                        onCommentClick = { activeCommentPostId = post.id },
                        onOptionsClick = { activeOptionsPost = post },
                        onShare = { sharePostSystem(context, post, viewModel) },
                        onAuthorClick = { viewModel.navigateTo(ScreenDestination.CreatorProfile(post.authorId)) },
                        onPostClick = {
                            val isReel = post.location == "Reels" || post.type == "video" || post.id.startsWith("r_") || reels.any { it.id == post.id }
                            if (isReel) {
                                viewModel.openReelInPlayer(post.id)
                            } else {
                                showFullScreenDetail = true
                            }
                        },
                        onLikesClick = { postForLikesList = post },
                        viewModel = viewModel,
                        currentUser = currentUser
                    )

                    if (showFullScreenDetail) {
                        FullScreenPostDialog(
                            post = post,
                            onDismiss = { showFullScreenDetail = false },
                            viewModel = viewModel,
                            currentUser = currentUser
                        )
                    }

                    // Sponsored Native Ad item inserted after every 4 posts/reels
                    if ((index + 1) % 4 == 0) {
                        val adIndex = ((index + 1) / 4 - 1) % SampleFeedAds.size
                        val ad = SampleFeedAds[adIndex].copy(id = "ad_post_${index}_${SampleFeedAds[adIndex].id}")
                        if (!hiddenAdIds.contains(ad.id)) {
                            SponsoredFeedAdCard(
                                ad = ad,
                                onCtaClick = { activeAdDetail = ad },
                                onHideAd = { hiddenAdIds = hiddenAdIds + ad.id },
                                onShareAd = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, "${ad.adTitle} - ${ad.ctaText}: ${ad.ctaUrl}")
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Sponsored Ad"))
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Sponsored Ad Detail BottomSheet
    activeAdDetail?.let { ad ->
        SponsoredAdDetailSheet(
            ad = ad,
            onDismiss = { activeAdDetail = null },
            onHideAd = {
                hiddenAdIds = hiddenAdIds + ad.id
                activeAdDetail = null
            }
        )
    }

    // Comments Sheet
    activeCommentPostId?.let { postId ->
        val postComments = commentsMap[postId] ?: emptyList()
        CommentsBottomSheet(
            comments = postComments,
            onDismiss = { activeCommentPostId = null },
            onSendComment = { text -> viewModel.onAddComment(postId, text) }
        )
    }

    // Post Options Sheet
    activeOptionsPost?.let { post ->
        PostOptionsSheet(
            post = post,
            isOwner = post.authorId == currentUser.userId,
            onDismiss = { activeOptionsPost = null },
            onSaveToggle = { viewModel.onBookmarkPost(post.id) },
            onShare = {
                sharePostSystem(context, post, viewModel)
                activeOptionsPost = null
            },
            onEdit = {
                activeOptionsPost = null
                activeEditPost = post
            },
            onDelete = { viewModel.onDeletePost(post.id) },
            onReport = {
                reportTargetPost = post
                activeOptionsPost = null
            },
            onBlock = {
                blockTargetPost = post
                activeOptionsPost = null
            }
        )
    }

    // Edit Post Sheet
    activeEditPost?.let { post ->
        EditPostBottomSheet(
            post = post,
            viewModel = viewModel,
            onDismiss = { activeEditPost = null },
            onSave = { newCaption, newLocation, newPrivacy, newTags ->
                viewModel.onEditPost(post.id, newCaption, newLocation, newPrivacy, newTags)
                activeEditPost = null
            }
        )
    }

    // Report Post Dialog
    reportTargetPost?.let { post ->
        val reasons = listOf(
            "Spam or misleading (स्पैम या भ्रामक)",
            "Hate speech or harassment (घृणास्पद भाषण / उत्पीड़न)",
            "Inappropriate imagery (अनुचित छवि / दृश्य)",
            "Violence or harmful content (हिंसा / नुकसानदेह)",
            "Intellectual property violation (कॉपीराइट उल्लंघन)"
        )
        var selectedReason by remember { mutableStateOf(reasons[0]) }

        AlertDialog(
            onDismissRequest = { reportTargetPost = null },
            containerColor = HundredGramCardBackground,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Report Post (पोस्ट रिपोर्ट करें)",
                    color = HundredGramTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Why are you reporting this post by @${post.authorUsername}?", color = HundredGramTextSecondary, fontSize = 13.sp)
                    reasons.forEach { reason ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedReason == reason) HundredGramPink.copy(alpha = 0.2f) else HundredGramCardElevated)
                                .clickable { selectedReason = reason }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedReason == reason,
                                onClick = { selectedReason = reason },
                                colors = RadioButtonDefaults.colors(selectedColor = HundredGramPink)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(reason, color = HundredGramTextPrimary, fontSize = 12.5.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.reportContent(
                            targetId = post.id,
                            targetType = "Post",
                            authorUsername = post.authorUsername,
                            reason = selectedReason
                        )
                        reportTargetPost = null
                        android.widget.Toast.makeText(
                            context,
                            "Report submitted successfully (रिपोर्ट दर्ज कर ली गई)",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HundredGramPink),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Submit Report", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { reportTargetPost = null }) {
                    Text("Cancel", color = HundredGramTextSecondary)
                }
            }
        )
    }

    // Block Creator Confirmation Dialog
    blockTargetPost?.let { post ->
        AlertDialog(
            onDismissRequest = { blockTargetPost = null },
            containerColor = HundredGramCardBackground,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Block @${post.authorUsername}?",
                    color = HundredGramTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "You will no longer see posts, reels, or stories from @${post.authorUsername}. They will be immediately removed from your feed.",
                    color = HundredGramTextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.blockUser(
                            userId = post.authorId,
                            username = post.authorUsername,
                            displayName = "",
                            avatarUrl = post.authorAvatarUrl
                        )
                        blockTargetPost = null
                        android.widget.Toast.makeText(
                            context,
                            "Creator @${post.authorUsername} blocked (ब्लॉक कर दिया गया)",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.HundredGramLikeRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Block (ब्लॉक करें)", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { blockTargetPost = null }) {
                    Text("Cancel", color = HundredGramTextSecondary)
                }
            }
        )
    }

    // Likes list bottom sheet
    postForLikesList?.let { post ->
        com.example.ui.components.LikesListBottomSheet(
            postAuthorId = post.authorId,
            likesCount = post.likesCount,
            isLikedByMe = post.isLiked,
            onDismiss = { postForLikesList = null },
            viewModel = viewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoriesRow(
    stories: List<StoryEntity>,
    currentUser: UserData,
    onAddStoryClick: () -> Unit,
    onStoryClick: (Int, String?) -> Unit
) {
    var showMyStoryOptionsSheet by remember { mutableStateOf(false) }

    // Current user's own stories
    val myStories = remember(stories, currentUser) {
        stories.filter {
            (it.userId.isNotBlank() && it.userId == currentUser.userId) ||
                    (currentUser.username.isNotBlank() && it.username == currentUser.username)
        }
    }
    val hasMyStories = myStories.isNotEmpty()

    // Other creators' stories grouped by user
    val otherStoriesGrouped = remember(stories, currentUser) {
        stories.filterNot {
            (it.userId.isNotBlank() && it.userId == currentUser.userId) ||
                    (currentUser.username.isNotBlank() && it.username == currentUser.username)
        }.groupBy {
            if (it.userId.isNotBlank()) it.userId else it.username
        }.values.toList()
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 0.dp, bottom = 2.dp),
        contentPadding = PaddingValues(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Current User Story avatar item (Your Story)
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (hasMyStories) {
                            showMyStoryOptionsSheet = true
                        } else {
                            onAddStoryClick()
                        }
                    }
                )
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    UserAvatar(
                        avatarUrl = currentUser.avatarUrl,
                        size = 44.dp,
                        hasActiveStory = hasMyStories,
                        isSeen = false
                    )
                    // Add Story / Story Count Badge
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(HundredGramButtonGradient)
                            .border(1.2.dp, HundredGramDarkBackground, CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onAddStoryClick
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Story",
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (hasMyStories) "Your Story (${myStories.size})" else "Your Story",
                    color = HundredGramTextPrimary,
                    fontSize = 10.sp,
                    fontWeight = if (hasMyStories) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1
                )
            }
        }

        // Other Creators' Stories
        items(otherStoriesGrouped) { userStories ->
            val firstStory = userStories.first()
            val isAllSeen = userStories.all { it.isSeen }
            val creatorTarget = firstStory.userId.ifBlank { firstStory.username }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onStoryClick(0, creatorTarget) }
                )
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    UserAvatar(
                        avatarUrl = firstStory.userAvatarUrl,
                        size = 44.dp,
                        hasActiveStory = true,
                        isSeen = isAllSeen
                    )
                    if (userStories.size > 1) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(HundredGramPink)
                                .border(0.8.dp, HundredGramDarkBackground, RoundedCornerShape(4.dp))
                                .padding(horizontal = 2.5.dp, vertical = 0.5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${userStories.size}",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = firstStory.username,
                    color = if (isAllSeen) HundredGramTextSecondary else HundredGramTextPrimary,
                    fontSize = 10.sp,
                    fontWeight = if (isAllSeen) FontWeight.Normal else FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }

    // Modal Sheet when user clicks own active story
    if (showMyStoryOptionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMyStoryOptionsSheet = false },
            containerColor = HundredGramCardBackground,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp, top = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    UserAvatar(avatarUrl = currentUser.avatarUrl, size = 44.dp, hasActiveStory = true)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Your Story (आपकी स्टोरीज़)",
                            color = HundredGramTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "${myStories.size} active ${if (myStories.size == 1) "story" else "stories"}",
                            color = HundredGramTextSecondary,
                            fontSize = 12.5.sp
                        )
                    }
                }

                Divider(color = HundredGramDivider, thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Action 1: View Story
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(HundredGramCardElevated)
                        .clickable {
                            showMyStoryOptionsSheet = false
                            onStoryClick(0, currentUser.userId)
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(HundredGramPink.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "View Story",
                            tint = HundredGramPink,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "View Story (स्टोरी देखें)",
                            color = HundredGramTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp
                        )
                        Text(
                            text = "See your uploaded stories and reactions",
                            color = HundredGramTextSecondary,
                            fontSize = 11.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action 2: Add New Story
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(HundredGramCardElevated)
                        .clickable {
                            showMyStoryOptionsSheet = false
                            onAddStoryClick()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(HundredGramBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Add Story",
                            tint = HundredGramBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Add to Story (+ नई स्टोरी जोड़ें)",
                            color = HundredGramTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp
                        )
                        Text(
                            text = "Capture a photo or video to add another story",
                            color = HundredGramTextSecondary,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostCard(
    post: PostEntity,
    isCurrentUser: Boolean,
    onLike: () -> Unit,
    onBookmark: () -> Unit,
    onCommentClick: () -> Unit,
    onOptionsClick: () -> Unit,
    onShare: () -> Unit = {},
    onAuthorClick: () -> Unit = {},
    onPostClick: () -> Unit = {},
    onLikesClick: () -> Unit = {},
    viewModel: MainViewModel,
    currentUser: UserData
) {
    val coroutineScope = rememberCoroutineScope()
    var showHeartAnim by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (showHeartAnim) 1.25f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "heartScale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(HundredGramCardBackground)
            .border(1.dp, HundredGramBorderSubtle.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .padding(bottom = 12.dp)
    ) {
        // Author Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onAuthorClick
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val displayAvatar = if ((post.authorId == currentUser.userId || post.authorUsername.equals(currentUser.username, ignoreCase = true)) && currentUser.avatarUrl.isNotBlank()) currentUser.avatarUrl else post.authorAvatarUrl
                UserAvatar(avatarUrl = displayAvatar, size = 38.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.authorDisplayName.ifBlank { post.authorUsername },
                            color = HundredGramTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(HundredGramTextSecondary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = com.example.util.TimeUtils.getRelativeTimeAgo(post.timestamp),
                            color = HundredGramTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        com.example.ui.components.PrivacyBadgePill(
                            privacyStr = post.privacy,
                            onClick = if (isCurrentUser) { { onOptionsClick() } } else null
                        )
                    }
                    if (post.location.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 1.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = HundredGramPink,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = post.location,
                                color = HundredGramTextSecondary,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, radius = 16.dp),
                        onClick = onOptionsClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = HundredGramTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Post Image with Double-Tap to Like
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(HundredGramCardElevated)
                .combinedClickable(
                    onClick = onPostClick,
                    onDoubleClick = {
                        if (!post.isLiked) onLike()
                        coroutineScope.launch {
                            showHeartAnim = true
                            delay(700)
                            showHeartAnim = false
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            val isVideo = post.type == "video" || post.imageUrl.endsWith(".mp4", ignoreCase = true) ||
                    post.imageUrl.contains(".mp4", ignoreCase = true) ||
                    post.imageUrl.contains("video", ignoreCase = true)
            val isText = post.type == "text" || post.isTextOnly || post.imageUrl.isBlank()
            val imageModel = remember(post.imageUrl) {
                com.example.util.ImageUtils.getSafeImageModel(post.imageUrl)
            }
            if (isText) {
                com.example.ui.components.TextPostGraphicCard(
                    post = post,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (isVideo) {
                val videoUrl = if (post.mediaUrl.isNotBlank()) post.mediaUrl else post.imageUrl
                com.example.ui.components.VideoPlayer(
                    videoUrl = videoUrl,
                    isMuted = true,
                    fallbackImageUrl = videoUrl,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                coil.compose.SubcomposeAsyncImage(
                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                        .data(imageModel)
                        .crossfade(true)
                        .build(),
                    contentDescription = post.caption,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(com.example.ui.theme.HundredGramCardElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = HundredGramPink,
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 2.5.dp
                            )
                        }
                    }
                )
            }

            // Animated Heart Pop
            androidx.compose.animation.AnimatedVisibility(
                visible = showHeartAnim,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Liked",
                    tint = Color.White.copy(alpha = 0.95f),
                    modifier = Modifier
                        .size(110.dp)
                        .scale(scale)
                )
            }
        }

        // Post Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Like Action with Bounce Feel
            IconButton(
                onClick = onLike,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (post.isLiked) HundredGramLikeRed else HundredGramTextPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Comment Action
            IconButton(
                onClick = onCommentClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = "Comment",
                    tint = HundredGramTextPrimary,
                    modifier = Modifier.size(23.dp)
                )
            }

            // Share Action
            IconButton(
                onClick = onShare,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = HundredGramTextPrimary,
                    modifier = Modifier.size(21.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bookmark Action
            IconButton(
                onClick = onBookmark,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (post.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Save",
                    tint = if (post.isSaved) HundredGramPink else HundredGramTextPrimary,
                    modifier = Modifier.size(25.dp)
                )
            }
        }

        // Likes Count Pill
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 2.dp)
                .clickable { onLikesClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isCreator = post.authorId == currentUser.userId
            var displayedLikesCount by remember(post.likesCount, isCreator) { mutableStateOf(post.likesCount) }

            if (!isCreator) {
                LaunchedEffect(post.likesCount, post.isLiked) {
                    try {
                        val allUsers = viewModel.repository.getAllUsers()
                        val otherUsers = allUsers.filter { it.userId != currentUser.userId }
                        val itemsToTake = (post.likesCount - (if (post.isLiked) 1 else 0)).coerceIn(0, otherUsers.size)
                        val rawOtherLikers = otherUsers.take(itemsToTake)

                        var mutualCount = 0
                        rawOtherLikers.forEach { u ->
                            val curUserFollowsLiker = viewModel.repository.isFollowingUser(u.userId)
                            val likerFollowsCurUser = viewModel.repository.isUserFollowing(currentUser.userId, u.userId)
                            if (curUserFollowsLiker && likerFollowsCurUser) {
                                mutualCount++
                            }
                        }
                        displayedLikesCount = mutualCount + (if (post.isLiked) 1 else 0)
                    } catch (e: Exception) {
                        displayedLikesCount = post.likesCount
                    }
                }
            } else {
                displayedLikesCount = post.likesCount
            }

            Text(
                text = "$displayedLikesCount likes",
                color = HundredGramTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp
            )
        }

        // Caption
        if (post.caption.isNotBlank()) {
            val authorName = post.authorDisplayName.ifBlank { post.authorUsername }
            val captionText = if (post.caption.contains("__ roar 🦁")) {
                post.caption
            } else {
                "$authorName __ roar 🦁 ${post.caption}"
            }
            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 3.dp)) {
                Text(
                    text = captionText,
                    color = HundredGramTextPrimary,
                    fontSize = 13.5.sp,
                    lineHeight = 18.sp
                )
            }
        }

        // Comments Count Callout
        Text(
            text = if (post.commentsCount > 0) "View all ${post.commentsCount} comments" else "Add a comment... 💬",
            color = HundredGramTextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clickable { onCommentClick() }
                .padding(horizontal = 14.dp, vertical = 3.dp)
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    comments: List<CommentEntity>,
    onDismiss: () -> Unit,
    onSendComment: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var commentInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = HundredGramCardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(450.dp)
                .padding(16.dp)
        ) {
            Text(
                text = "Comments",
                color = HundredGramTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (comments.isEmpty()) {
                    item {
                        Text(
                            text = "No comments yet. Be the first to share your thoughts!",
                            color = HundredGramTextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 20.dp)
                        )
                    }
                }
                items(comments) { comment ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        UserAvatar(avatarUrl = comment.authorAvatarUrl, size = 32.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = comment.authorUsername,
                                color = HundredGramTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                            Text(
                                text = comment.text,
                                color = HundredGramTextPrimary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Comment input bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentInput,
                    onValueChange = { commentInput = it },
                    placeholder = { Text("Add a comment...", color = HundredGramTextSecondary, fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = HundredGramTextPrimary,
                        unfocusedTextColor = HundredGramTextPrimary,
                        focusedBorderColor = HundredGramPink,
                        unfocusedBorderColor = HundredGramDivider
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (commentInput.isNotBlank()) {
                            onSendComment(commentInput)
                            commentInput = ""
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Post Comment",
                        tint = HundredGramPink
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPostBottomSheet(
    post: PostEntity,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSave: (newCaption: String, newLocation: String, newPrivacy: PrivacyLevel, tags: List<String>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var captionText by remember { mutableStateOf(post.caption) }
    var locationText by remember { mutableStateOf(post.location) }
    var taggedFriends by remember { mutableStateOf(post.tags) }
    var showTagFriendsDialog by remember { mutableStateOf(false) }
    var isLocating by remember { mutableStateOf(false) }
    var privacy by remember { mutableStateOf(try { PrivacyLevel.valueOf(post.privacy) } catch (e: Exception) { PrivacyLevel.PUBLIC }) }

    var allUsers by remember { mutableStateOf<List<UserData>>(emptyList()) }
    val currentUser by viewModel.currentUser.collectAsState()

    LaunchedEffect(Unit) {
        try {
            allUsers = viewModel.repository.getAllUsers()
        } catch (_: Exception) {}
    }

    val popularLocations = listOf(
        "Connaught Place, New Delhi",
        "Marine Drive, Mumbai",
        "Old Manali, HP",
        "Anjuna Beach, Goa",
        "Koramangala, Bengaluru"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = HundredGramCardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit Post",
                    color = HundredGramTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = {
                        onSave(captionText.trim(), locationText.trim(), privacy, taggedFriends)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HundredGramPink),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Post Thumbnail Preview Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = "Post preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(HundredGramCardElevated)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Editing as @${post.authorUsername}",
                        color = HundredGramTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Update your caption, location or tagged friends",
                        color = HundredGramTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Caption Field
            OutlinedTextField(
                value = captionText,
                onValueChange = { captionText = it },
                label = { Text("Caption") },
                placeholder = { Text("Write something catchy...", color = HundredGramTextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = HundredGramTextPrimary,
                    unfocusedTextColor = HundredGramTextPrimary,
                    focusedBorderColor = HundredGramPink,
                    unfocusedBorderColor = HundredGramDivider
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Location Field with GPS Button
            OutlinedTextField(
                value = locationText,
                onValueChange = { locationText = it },
                label = { Text("Location Tag") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = HundredGramPink
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                isLocating = true
                                val loc = viewModel.getCurrentLocationName()
                                locationText = loc
                                isLocating = false
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Current GPS Location",
                            tint = if (isLocating) HundredGramPink else HundredGramTextSecondary
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = HundredGramTextPrimary,
                    unfocusedTextColor = HundredGramTextPrimary,
                    focusedBorderColor = HundredGramPink,
                    unfocusedBorderColor = HundredGramDivider
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Tag Friends Row
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = HundredGramCardElevated,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTagFriendsDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
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
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (taggedFriends.isEmpty()) "Tap to tag friends" else "${taggedFriends.size} friends tagged",
                            color = if (taggedFriends.isEmpty()) HundredGramTextSecondary else HundredGramPink,
                            fontSize = 11.sp
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
                Spacer(modifier = Modifier.height(6.dp))
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
                                    fontSize = 11.5.sp,
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
            
            PrivacySelector(
                selectedPrivacy = privacy,
                onPrivacySelected = { privacy = it }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showTagFriendsDialog) {
        val availableUsers = remember(allUsers, currentUser) {
            allUsers.filter { it.userId != currentUser.userId }
        }
        TagFriendsDialog(
            initialTags = taggedFriends,
            availableUsers = availableUsers,
            onTagsSelected = { taggedFriends = it },
            onDismiss = { showTagFriendsDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostOptionsSheet(
    post: PostEntity,
    isOwner: Boolean,
    onDismiss: () -> Unit,
    onSaveToggle: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReport: () -> Unit = {},
    onBlock: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = HundredGramCardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Post Options",
                color = HundredGramTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // If user is author, show Edit and Delete options prominently
            if (isOwner) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onEdit() }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Post",
                        tint = HundredGramPink,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Edit Caption & Location",
                            color = HundredGramTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Update description or GPS tag",
                            color = HundredGramTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onDelete()
                            onDismiss()
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Post",
                        tint = com.example.ui.theme.HundredGramLikeRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Delete Post",
                        color = com.example.ui.theme.HundredGramLikeRed,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }

                Divider(
                    color = HundredGramDivider,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        onSaveToggle()
                        onDismiss()
                    }
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (post.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Save Post",
                    tint = HundredGramTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = if (post.isSaved) "Remove from Saved" else "Save to Collection",
                    color = HundredGramTextPrimary,
                    fontSize = 15.sp
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        onShare()
                        onDismiss()
                    }
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = HundredGramTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Share Post Link",
                    color = HundredGramTextPrimary,
                    fontSize = 15.sp
                )
            }

            if (!isOwner) {
                Divider(
                    color = HundredGramDivider,
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                // Report Post Option
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
                        contentDescription = "Report Post",
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Report Post (पोस्ट रिपोर्ट करें)",
                            color = Color(0xFFFF9800),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Flag inappropriate or spam content",
                            color = HundredGramTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                // Block Creator Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onBlock() }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = "Block Creator",
                        tint = com.example.ui.theme.HundredGramLikeRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Block @${post.authorUsername} (क्रिएटर को ब्लॉक करें)",
                            color = com.example.ui.theme.HundredGramLikeRed,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Hide all posts, reels, and stories from creator",
                            color = HundredGramTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestedCreatorsSection(viewModel: MainViewModel) {
    val recommendedCreators by viewModel.recommendedCreators.collectAsState()
    val followedUserIds by viewModel.followedUserIds.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val posts by viewModel.posts.collectAsState()
    val reels by viewModel.reels.collectAsState()
    val nearbyUsers by viewModel.nearbyUsers.collectAsState()

    var showAllSuggestionsSheet by remember { mutableStateOf(false) }
    var dismissedUserIds by remember { mutableStateOf(setOf<String>()) }

    val allSuggestedCreators = remember(recommendedCreators, posts, reels, nearbyUsers, currentUser) {
        val list = mutableListOf<UserData>()
        list.addAll(recommendedCreators)
        nearbyUsers.forEach { nu ->
            if (nu.id != currentUser.userId && list.none { it.userId == nu.id }) {
                list.add(
                    UserData(
                        userId = nu.id,
                        username = nu.username.ifBlank { nu.name.lowercase().replace(" ", "_") },
                        displayName = nu.name,
                        avatarUrl = nu.avatarUrl,
                        bio = "${nu.distanceMeters}m away • ${nu.bio}"
                    )
                )
            }
        }
        posts.forEach { p ->
            if (p.authorId != currentUser.userId && list.none { it.userId == p.authorId }) {
                list.add(
                    UserData(
                        userId = p.authorId,
                        username = p.authorUsername,
                        displayName = p.authorDisplayName.ifBlank { p.authorUsername },
                        avatarUrl = p.authorAvatarUrl,
                        bio = "Post Creator 📸"
                    )
                )
            }
        }
        reels.forEach { r ->
            if (r.authorId != currentUser.userId && list.none { it.userId == r.authorId }) {
                list.add(
                    UserData(
                        userId = r.authorId,
                        username = r.authorUsername,
                        displayName = r.authorDisplayName.ifBlank { r.authorUsername },
                        avatarUrl = r.authorAvatarUrl,
                        bio = "Reels Creator 🎬"
                    )
                )
            }
        }
        list.filter { it.userId != currentUser.userId }.distinctBy { it.userId }
    }

    val visibleCreators = remember(allSuggestedCreators, dismissedUserIds) {
        allSuggestedCreators.filterNot { it.userId in dismissedUserIds }
    }

    if (visibleCreators.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(HundredGramDarkBackground)
                .padding(top = 1.dp, bottom = 2.dp)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val horizontalPadding = 12.dp
                val avatarSpacing = 12.dp // Increased spacing between avatars
                // Calculate item width so exactly 5 items fit on the screen at a time
                val itemWidth = ((maxWidth - (horizontalPadding * 2) - (avatarSpacing * 4)) / 5).coerceAtLeast(50.dp)

                // Ultra-Compact Horizontal Carousel with Creator Cards and integrated "See all" card
                LazyRow(
                    contentPadding = PaddingValues(horizontal = horizontalPadding),
                    horizontalArrangement = Arrangement.spacedBy(avatarSpacing),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(visibleCreators, key = { it.userId }) { creator ->
                        val isFollowed = followedUserIds.contains(creator.userId)

                        Column(
                            modifier = Modifier
                                .width(itemWidth)
                                .clip(RoundedCornerShape(8.dp))
                                .background(HundredGramCardElevated.copy(alpha = 0.85f))
                                .padding(horizontal = 2.dp, vertical = 3.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Compact Creator Avatar
                            UserAvatar(
                                avatarUrl = creator.avatarUrl.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde" },
                                size = 30.dp,
                                isVerified = creator.isVerified,
                                onClick = { viewModel.navigateTo(ScreenDestination.CreatorProfile(creator.userId)) }
                            )

                            Spacer(modifier = Modifier.height(1.dp))

                            Text(
                                text = creator.displayName.ifBlank { creator.username },
                                color = HundredGramTextPrimary,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.clickable { viewModel.navigateTo(ScreenDestination.CreatorProfile(creator.userId)) }
                            )

                            Spacer(modifier = Modifier.height(1.dp))

                            // Compact Mini Follow Button
                            Button(
                                onClick = {
                                    viewModel.toggleFollowUser(creator.userId)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFollowed) HundredGramCardGlass else HundredGramPink
                                ),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 1.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(16.dp)
                            ) {
                                Text(
                                    text = if (isFollowed) "✓" else "Follow",
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFollowed) HundredGramTextPrimary else Color.White
                                )
                            }
                        }
                    }

                    // Integrated "See all" Card
                    item(key = "see_all_creators_card") {
                        Column(
                            modifier = Modifier
                                .width(itemWidth)
                                .clip(RoundedCornerShape(8.dp))
                                .background(HundredGramCardElevated.copy(alpha = 0.85f))
                                .clickable { showAllSuggestionsSheet = true }
                                .padding(horizontal = 2.dp, vertical = 5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(HundredGramPink.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "See All",
                                    tint = HundredGramPink,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "See all",
                                color = HundredGramPink,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            Divider(color = HundredGramDivider.copy(alpha = 0.4f), thickness = 0.5.dp)
        }
    }

    // "See All" Full Suggested Creators List BottomSheet
    if (showAllSuggestionsSheet) {
        AllSuggestedCreatorsSheet(
            creators = allSuggestedCreators,
            followedUserIds = followedUserIds,
            onDismiss = { showAllSuggestionsSheet = false },
            onToggleFollow = { userId -> viewModel.toggleFollowUser(userId) },
            onCreatorClick = { userId ->
                showAllSuggestionsSheet = false
                viewModel.navigateTo(ScreenDestination.CreatorProfile(userId))
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllSuggestedCreatorsSheet(
    creators: List<UserData>,
    followedUserIds: Set<String>,
    onDismiss: () -> Unit,
    onToggleFollow: (String) -> Unit,
    onCreatorClick: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCreators = remember(creators, searchQuery) {
        if (searchQuery.isBlank()) creators
        else creators.filter {
            it.displayName.contains(searchQuery, ignoreCase = true) ||
                    it.username.contains(searchQuery, ignoreCase = true) ||
                    it.bio.contains(searchQuery, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = HundredGramCardBackground,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp, top = 4.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Creators",
                        tint = HundredGramPink,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Suggested Creators (सभी सुझाव)",
                        color = HundredGramTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = HundredGramTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search creators... (क्रिएटर खोजें)", color = HundredGramTextSecondary, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = HundredGramTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = HundredGramTextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HundredGramPink,
                    unfocusedBorderColor = HundredGramBorderSubtle,
                    focusedTextColor = HundredGramTextPrimary,
                    unfocusedTextColor = HundredGramTextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = HundredGramDivider, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(8.dp))

            if (filteredCreators.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No creators found matching \"$searchQuery\"",
                        color = HundredGramTextSecondary,
                        fontSize = 13.5.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredCreators, key = { it.userId }) { creator ->
                        val isFollowed = followedUserIds.contains(creator.userId)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(HundredGramCardElevated)
                                .clickable { onCreatorClick(creator.userId) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(
                                avatarUrl = creator.avatarUrl.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde" },
                                size = 48.dp,
                                isVerified = creator.isVerified,
                                onClick = { onCreatorClick(creator.userId) }
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = creator.displayName.ifBlank { creator.username },
                                        color = HundredGramTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Text(
                                    text = "@${creator.username}",
                                    color = HundredGramTextSecondary,
                                    fontSize = 11.5.sp
                                )
                                if (creator.bio.isNotBlank()) {
                                    Text(
                                        text = creator.bio,
                                        color = HundredGramTextSecondary.copy(alpha = 0.8f),
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = { onToggleFollow(creator.userId) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFollowed) HundredGramCardGlass else HundredGramPink
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(
                                    text = if (isFollowed) "Following" else "Follow",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFollowed) HundredGramTextPrimary else Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FullScreenPostDialog(
    post: PostEntity,
    onDismiss: () -> Unit,
    viewModel: MainViewModel,
    currentUser: com.example.data.UserData
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var showCommentsSheet by remember { mutableStateOf(false) }
    val commentsMap by viewModel.comments.collectAsState()
    val postComments = commentsMap[post.id] ?: emptyList()
    val videoUrl = if (post.mediaUrl.isNotBlank()) post.mediaUrl else post.imageUrl
    val isVideo = post.type == "video" ||
            videoUrl.endsWith(".mp4", ignoreCase = true) ||
            videoUrl.contains(".mp4", ignoreCase = true) ||
            videoUrl.contains("video", ignoreCase = true)

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Main Media Content
            if (post.isTextOnly || (post.imageUrl.isBlank() && post.mediaUrl.isBlank())) {
                com.example.ui.components.TextPostGraphicCard(
                    post = post,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (isVideo) {
                // 9:16 full immersive mobile size
                com.example.ui.components.VideoPlayer(
                    videoUrl = videoUrl,
                    isMuted = false,
                    useController = true,
                    fallbackImageUrl = post.imageUrl,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Photo with Pinch-to-Zoom and Pan
                val imageModel = remember(post.imageUrl) {
                    com.example.util.ImageUtils.getSafeImageModel(post.imageUrl)
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (scale * zoom).coerceIn(1f, 5f)
                                scale = newScale
                                if (newScale > 1f) {
                                    val maxOffset = 400.dp.toPx() * (newScale - 1f)
                                    offset = Offset(
                                        x = (offset.x + pan.x).coerceIn(-maxOffset, maxOffset),
                                        y = (offset.y + pan.y).coerceIn(-maxOffset, maxOffset)
                                    )
                                } else {
                                    offset = Offset.Zero
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (scale > 1.2f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                    } else {
                                        scale = 2.5f
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    coil.compose.SubcomposeAsyncImage(
                        model = coil.request.ImageRequest.Builder(LocalContext.current)
                            .data(imageModel)
                            .crossfade(true)
                            .build(),
                        contentDescription = post.caption,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            },
                        loading = {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    color = HundredGramPink,
                                    modifier = Modifier.size(36.dp),
                                    strokeWidth = 3.dp
                                )
                            }
                        }
                    )
                }
            }

            // Top Overlay (Header)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.75f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            onDismiss()
                            viewModel.navigateTo(ScreenDestination.CreatorProfile(post.authorId))
                        }
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        UserAvatar(avatarUrl = post.authorAvatarUrl, size = 34.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "@${post.authorUsername}",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (post.location.isNotBlank()) {
                                Text(
                                    text = post.location,
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Zoom reset indicator badge if zoomed
                    if (scale > 1.05f) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.25f))
                                .clickable {
                                    scale = 1f
                                    offset = Offset.Zero
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Zoom ${(scale * 100).toInt()}% • Reset",
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Bottom Overlay (Actions & Caption)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
                Column {
                    if (post.caption.isNotBlank()) {
                        val authorName = post.authorDisplayName.ifBlank { post.authorUsername }
                        val captionText = if (post.caption.contains("__ roar 🦁")) {
                            post.caption
                        } else {
                            "$authorName __ roar 🦁 ${post.caption}"
                        }
                        Text(
                            text = captionText,
                            color = Color.White,
                            fontSize = 14.sp,
                            maxLines = 3
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.onLikePost(post.id) }) {
                                Icon(
                                    imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Like",
                                    tint = if (post.isLiked) com.example.ui.theme.HundredGramLikeRed else Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                text = "${post.likesCount}",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            IconButton(onClick = { showCommentsSheet = true }) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = "Comment",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Text(
                                text = "${post.commentsCount}",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { sharePostSystem(context, post, viewModel) }) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            IconButton(onClick = { viewModel.onBookmarkPost(post.id) }) {
                                Icon(
                                    imageVector = if (post.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "Save",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCommentsSheet) {
        CommentsBottomSheet(
            comments = postComments,
            onDismiss = { showCommentsSheet = false },
            onSendComment = { text -> viewModel.onAddComment(post.id, text) }
        )
    }
}

// -------------------------------------------------------------
// SPONSORED ADS ARCHITECTURE & COMPOSABLES FOR HOME FEED (ADMOB INTEGRATED)
// -------------------------------------------------------------

data class FeedAdItem(
    val id: String,
    val brandName: String,
    val brandHandle: String,
    val brandAvatarUrl: String,
    val adTitle: String,
    val adDescription: String,
    val mediaUrl: String,
    val isVideo: Boolean = false,
    val badgeText: String = "Sponsored • प्रायोजित",
    val promoTag: String = "⚡ 50% OFF SPECIAL",
    val ctaText: String = "Shop Now",
    val couponCode: String = "HUNDRED50",
    val rating: Float = 4.9f,
    val reviewsCount: String = "12.4k",
    val ctaUrl: String = "https://hundredgram.app/promo",
    val likesCount: Int = 1420,
    val adUnitId: String = "ca-app-pub-6058721301027431/8718806610"
)

val SampleFeedAds = listOf(
    FeedAdItem(
        id = "ad_pro_suite",
        brandName = "HundredGram Pro",
        brandHandle = "hundredgram_pro",
        brandAvatarUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe",
        adTitle = "HundredGram Creator Studio Pro 🚀",
        adDescription = "Unlock 4K 60FPS Video Export, AI Cinema Filters, Ultra Stabilization, & Unlimited Audio library.",
        mediaUrl = "https://images.unsplash.com/photo-1574717024653-61fd2cf4d44d",
        isVideo = true,
        badgeText = "Sponsored • प्रायोजित",
        promoTag = "🔥 PROMO: 1 MONTH FREE",
        ctaText = "Claim Free Trial →",
        couponCode = "PROTRIAL2026",
        rating = 4.9f,
        reviewsCount = "48.2k",
        ctaUrl = "https://hundredgram.app/pro",
        likesCount = 3840,
        adUnitId = "ca-app-pub-6058721301027431/8718806610"
    ),
    FeedAdItem(
        id = "ad_soundwave",
        brandName = "SoundWave Audio Studio",
        brandHandle = "soundwave_beats",
        brandAvatarUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4",
        adTitle = "Trending Beats & Cinematic Background Music 🎧",
        adDescription = "Thousands of royalty-free Hindi, Punjabi, Bollywood & Global Lo-Fi beats ready for your next viral reel.",
        mediaUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745",
        isVideo = false,
        badgeText = "Sponsored • प्रायोजित",
        promoTag = "🎵 100% ROYALTY FREE",
        ctaText = "Listen & Download →",
        couponCode = "BEATS4CREATORS",
        rating = 4.8f,
        reviewsCount = "29.1k",
        ctaUrl = "https://soundwavebeats.io",
        likesCount = 2190,
        adUnitId = "ca-app-pub-6058721301027431/8718806610"
    ),
    FeedAdItem(
        id = "ad_urbanfit",
        brandName = "UrbanVibe Streetwear",
        brandHandle = "urbanvibe_official",
        brandAvatarUrl = "https://images.unsplash.com/photo-1523381210434-271e8be1f52b",
        adTitle = "Summer 2026 Oversized Drop is Live! 👟🔥",
        adDescription = "Premium 280 GSM French Terry cotton tees & limited edition drip sneakers. Free express shipping across India.",
        mediaUrl = "https://images.unsplash.com/photo-1552374196-1ab2a1c593e8",
        isVideo = false,
        badgeText = "Sponsored • प्रायोजित",
        promoTag = "⚡ FLAT 40% OFF",
        ctaText = "Shop Collection →",
        couponCode = "DRIP40",
        rating = 4.9f,
        reviewsCount = "18.6k",
        ctaUrl = "https://urbanvibestore.com",
        likesCount = 5410,
        adUnitId = "ca-app-pub-6058721301027431/8718806610"
    )
)

@Composable
fun SponsoredFeedAdCard(
    ad: FeedAdItem,
    onCtaClick: () -> Unit,
    onHideAd: () -> Unit,
    onShareAd: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isLiked by remember { mutableStateOf(false) }
    var likesCount by remember { mutableStateOf(ad.likesCount) }
    var isSaved by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var adLoadFailed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(HundredGramCardBackground)
            .border(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        HundredGramPink.copy(alpha = 0.4f),
                        HundredGramBlue.copy(alpha = 0.3f),
                        HundredGramBorderSubtle.copy(alpha = 0.5f)
                    )
                ),
                RoundedCornerShape(20.dp)
            )
            .padding(bottom = 12.dp)
    ) {
        // Sponsor Brand Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, HundredGramPink, CircleShape)
            ) {
                coil.compose.AsyncImage(
                    model = ad.brandAvatarUrl,
                    contentDescription = ad.brandName,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = ad.brandName,
                        color = HundredGramTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Verified Brand",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(14.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = ad.badgeText,
                        color = HundredGramPink,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF3B82F6).copy(alpha = 0.2f))
                            .border(0.6.dp, Color(0xFF3B82F6).copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "AD",
                            color = Color(0xFF60A5FA),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // Options Menu
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Ad Options",
                        tint = HundredGramTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                androidx.compose.material3.DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(HundredGramCardElevated)
                ) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Hide this Ad (यह विज्ञापन छुपाएं)", color = HundredGramTextPrimary, fontSize = 13.sp) },
                        onClick = {
                            showMenu = false
                            onHideAd()
                            android.widget.Toast.makeText(context, "Ad hidden", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Close, contentDescription = null, tint = HundredGramTextSecondary, modifier = Modifier.size(18.dp))
                        }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Copy Promo Link (लिंक कॉपी करें)", color = HundredGramTextPrimary, fontSize = 13.sp) },
                        onClick = {
                            showMenu = false
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Ad URL", ad.ctaUrl)
                            clipboard.setPrimaryClip(clip)
                            android.widget.Toast.makeText(context, "Promo link copied!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = HundredGramTextSecondary, modifier = Modifier.size(18.dp))
                        }
                    )
                }
            }
        }

        // Sponsored Ad Hero Image Card (Rich full-color visual ad display)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(Color(0xFF1E293B))
                .clickable { onCtaClick() }
        ) {
            // High-quality sponsored banner image
            coil.compose.AsyncImage(
                model = if (ad.mediaUrl.isNotBlank()) ad.mediaUrl else "https://images.unsplash.com/photo-1523275335684-37898b6baf30",
                contentDescription = ad.adTitle,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Gradient overlay for crisp contrast
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.82f)
                            )
                        )
                    )
            )

            // Text overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(HundredGramPink)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("SPONSORED", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "★ ${ad.rating} (${ad.reviewsCount})",
                        color = Color(0xFFFBBF24),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = ad.adTitle,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = ad.adDescription,
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    maxLines = 2
                )
            }
        }

        // Interactive Full-Width CTA Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF4338CA), Color(0xFF6D28D9), Color(0xFFDB2777))
                    )
                )
                .clickable { onCtaClick() }
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = ad.ctaText,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                    Text(
                        text = "Code: ${ad.couponCode} • ★ ${ad.rating} (${ad.reviewsCount})",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Action Buttons Row (Like, Comment, Share, Bookmark)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    isLiked = !isLiked
                    likesCount += if (isLiked) 1 else -1
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) HundredGramLikeRed else HundredGramTextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = "$likesCount",
                color = HundredGramTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onCtaClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = "Comment",
                    tint = HundredGramTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onShareAd,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = HundredGramTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = {
                    isSaved = !isSaved
                    val msg = if (isSaved) "Ad Offer saved to bookmarks!" else "Ad removed from bookmarks"
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Save",
                    tint = if (isSaved) HundredGramPink else HundredGramTextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SponsoredAdDetailSheet(
    ad: FeedAdItem,
    onDismiss: () -> Unit,
    onHideAd: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = HundredGramCardBackground,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp, top = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, HundredGramPink, CircleShape)
                ) {
                    coil.compose.AsyncImage(
                        model = ad.brandAvatarUrl,
                        contentDescription = ad.brandName,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = ad.brandName,
                            color = HundredGramTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Verified",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "Official AdMob Partner • ★ ${ad.rating} (${ad.reviewsCount} reviews)",
                        color = HundredGramTextSecondary,
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = HundredGramTextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = ad.adTitle,
                color = HundredGramTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = ad.adDescription,
                color = HundredGramTextSecondary,
                fontSize = 13.5.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Coupon Code Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(HundredGramCardElevated)
                    .border(1.dp, HundredGramBorderSubtle, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "EXCLUSIVE COUPON CODE",
                        color = HundredGramTextSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = ad.couponCode,
                        color = HundredGramPink,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }

                Button(
                    onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Coupon Code", ad.couponCode)
                        clipboard.setPrimaryClip(clip)
                        android.widget.Toast.makeText(context, "Coupon '${ad.couponCode}' copied!", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HundredGramPink.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Copy Code",
                        color = HundredGramPink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(ad.ctaUrl))
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        android.widget.Toast.makeText(context, "Opening ${ad.brandName} Offer...", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HundredGramPink),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = ad.ctaText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(
                onClick = onHideAd,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Hide this advertisement (विज्ञापन छुपाएं)",
                    color = HundredGramTextSecondary,
                    fontSize = 12.5.sp
                )
            }
        }
    }
}
