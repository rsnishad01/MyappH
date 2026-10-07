package com.example.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.data.PostEntity
import com.example.data.ReelVideo
import com.example.data.UserData
import com.example.util.ShareManager
import com.example.ui.MainViewModel
import com.example.ui.ScreenDestination
import com.example.ui.components.UserAvatar
import com.example.ui.feed.FeedScreen
import com.example.ui.feed.PostCard
import com.example.ui.theme.HundredGramBorderSubtle
import com.example.ui.theme.HundredGramCardBackground
import com.example.ui.theme.HundredGramCardElevated
import com.example.ui.theme.HundredGramDarkBackground
import com.example.ui.theme.HundredGramPink
import com.example.ui.theme.HundredGramTextPrimary
import com.example.ui.theme.HundredGramTextSecondary
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorProfileScreen(viewModel: MainViewModel, userId: String) {
    var creator by remember { mutableStateOf<UserData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    val posts by viewModel.posts.collectAsState()
    val reels by viewModel.reels.collectAsState()
    val followedUserIds by viewModel.followedUserIds.collectAsState()
    val isFollowingMap by viewModel.isFollowingMap.collectAsState()
    val isFollowing = isFollowingMap[userId] ?: followedUserIds.contains(userId)

    // Load creator profile & check following status
    LaunchedEffect(userId) {
        isLoading = true
        var foundUser: UserData? = null

        try {
            val allUsers = viewModel.repository.getAllUsers()
            foundUser = allUsers.firstOrNull { it.userId == userId || it.username.equals(userId, ignoreCase = true) }
        } catch (_: Exception) {}

        if (foundUser == null) {
            val matchingPost = posts.firstOrNull { it.authorId == userId || it.authorUsername.equals(userId, ignoreCase = true) }
            if (matchingPost != null) {
                foundUser = UserData(
                    userId = matchingPost.authorId,
                    username = matchingPost.authorUsername,
                    displayName = matchingPost.authorDisplayName.ifBlank { matchingPost.authorUsername },
                    avatarUrl = matchingPost.authorAvatarUrl,
                    bio = "Creator on HundredGram ✨",
                    followersCount = 0,
                    followingCount = 0,
                    postsCount = posts.count { it.authorId == matchingPost.authorId }
                )
            } else {
                val matchingReel = reels.firstOrNull { it.authorId == userId || it.authorUsername.equals(userId, ignoreCase = true) }
                if (matchingReel != null) {
                    foundUser = UserData(
                        userId = matchingReel.authorId,
                        username = matchingReel.authorUsername,
                        displayName = matchingReel.authorDisplayName.ifBlank { matchingReel.authorUsername },
                        avatarUrl = matchingReel.authorAvatarUrl,
                        bio = "Reels Creator on HundredGram 🎬",
                        followersCount = 0,
                        followingCount = 0,
                        postsCount = reels.count { it.authorId == matchingReel.authorId }
                    )
                }
            }
        }

        if (foundUser == null) {
            val cleanName = if (userId.startsWith("u_") || userId.startsWith("user_")) userId.substringAfter("_") else userId
            foundUser = UserData(
                userId = userId,
                username = cleanName.ifBlank { "creator" },
                displayName = cleanName.replaceFirstChar { it.uppercase() }.ifBlank { "Creator" },
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb",
                bio = "Creator on HundredGram • Welcome to my profile! ✨",
                followersCount = 0,
                followingCount = 0,
                postsCount = 0
            )
        }

        creator = foundUser
        isLoading = false
        viewModel.checkIfFollowing(userId)

        // 2. Non-blocking Firestore refresh in background
        coroutineScope.launch {
            try {
                kotlinx.coroutines.withTimeoutOrNull(1500L) {
                    val remote = viewModel.repository.firestoreRepository.getUserFromFirestore(userId)
                    if (remote != null) {
                        creator = remote
                    }
                }
            } catch (_: Exception) {}
        }
    }

    val creatorPosts = remember(posts, userId) {
        posts.filter { it.authorId == userId }
    }

    val creatorReels = remember(reels, userId) {
        reels.filter { it.authorId == userId }
    }

    var selectedPreviewPost by remember { mutableStateOf<PostEntity?>(null) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showBlockConfirmDialog by remember { mutableStateOf(false) }
    var showReportCreatorDialog by remember { mutableStateOf(false) }
    var showFollowersListSheet by remember { mutableStateOf(false) }
    var followersListTab by remember { mutableIntStateOf(0) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HundredGramDarkBackground)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.navigateTo(ScreenDestination.Feed) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = HundredGramTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = creator?.username ?: "Profile",
                        color = HundredGramTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Three line / menu system on top right
                Box {
                    IconButton(onClick = { showOptionsMenu = true }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Menu,
                            contentDescription = "Options Menu",
                            tint = HundredGramTextPrimary
                        )
                    }

                    DropdownMenu(
                        expanded = showOptionsMenu,
                        onDismissRequest = { showOptionsMenu = false },
                        modifier = Modifier.background(HundredGramCardBackground)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Share Profile (प्रोफाइल शेयर करें)",
                                    color = HundredGramTextPrimary
                                )
                            },
                            onClick = {
                                showOptionsMenu = false
                                creator?.let { ShareManager.shareProfile(context, it) }
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Block Creator (क्रिएटर को ब्लॉक करें)",
                                    color = com.example.ui.theme.HundredGramLikeRed,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            onClick = {
                                showOptionsMenu = false
                                showBlockConfirmDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Report Creator (क्रिएटर को रिपोर्ट करें)",
                                    color = HundredGramTextPrimary
                                )
                            },
                            onClick = {
                                showOptionsMenu = false
                                showReportCreatorDialog = true
                            }
                        )
                    }
                }
            }
        },
        containerColor = HundredGramDarkBackground
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                coroutineScope.launch {
                    isRefreshing = true
                    val loaded = viewModel.repository.firestoreRepository.getUserFromFirestore(userId)
                    if (loaded != null) {
                        creator = loaded
                    }
                    viewModel.refreshFeed()
                    delay(600)
                    isRefreshing = false
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = HundredGramPink)
                }
            } else if (creator == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Creator profile not found.", color = HundredGramTextSecondary)
                }
            } else {
                val user = creator!!
                var showFullScreenAvatar by remember { mutableStateOf(false) }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                // Profile stats panel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(HundredGramCardBackground)
                        .border(1.dp, HundredGramBorderSubtle.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(
                            avatarUrl = user.avatarUrl,
                            size = 76.dp,
                            isVerified = user.isVerified,
                            onClick = { showFullScreenAvatar = true }
                        )

                        if (showFullScreenAvatar) {
                            com.example.ui.components.FullScreenImageDialog(
                                imageUrl = user.avatarUrl,
                                isEditable = false,
                                onDismiss = { showFullScreenAvatar = false },
                                onUpdateProfilePic = {}
                            )
                        }
                        Spacer(modifier = Modifier.width(24.dp))
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            CreatorProfileStatColumn(count = creatorPosts.size, label = "Posts")
                            CreatorProfileStatColumn(
                                count = user.followersCount,
                                label = "Followers",
                                onClick = {
                                    showFollowersListSheet = true
                                    followersListTab = 0
                                }
                            )
                            CreatorProfileStatColumn(
                                count = user.followingCount,
                                label = "Following",
                                onClick = {
                                    showFollowersListSheet = true
                                    followersListTab = 1
                                }
                            )
                        }
                    }
                }

                // Profile Info
                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.displayName,
                            color = HundredGramTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        if (user.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified",
                                tint = Color(0xFF3897F0),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    if (user.bio.isNotBlank()) {
                        Text(
                            text = user.bio,
                            color = HundredGramTextPrimary,
                            fontSize = 13.5.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    if (user.website.isNotBlank()) {
                        Text(
                            text = user.website,
                            color = Color(0xFF3897F0),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Action Buttons: Follow and Message
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.toggleFollowUser(user.userId) { nowFollowing ->
                                creator = creator?.copy(
                                    followersCount = if (nowFollowing) user.followersCount + 1 else (user.followersCount - 1).coerceAtLeast(0)
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFollowing) Color.DarkGray else HundredGramPink
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isFollowing) "Following" else "Follow",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.navigateTo(
                                ScreenDestination.ChatDetail(
                                    threadId = user.userId,
                                    recipientUsername = user.displayName.ifBlank { user.username },
                                    recipientAvatar = user.avatarUrl
                                )
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HundredGramCardElevated
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HundredGramBorderSubtle),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "Message",
                            tint = HundredGramTextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Message",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = HundredGramTextPrimary
                        )
                    }

                    IconButton(
                        onClick = { ShareManager.shareProfile(context, user) },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(HundredGramCardElevated)
                            .border(1.dp, HundredGramBorderSubtle, RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Profile",
                            tint = HundredGramTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Divider(color = Color.Gray.copy(alpha = 0.2f), thickness = 0.8.dp)

                // Grid tabs
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = HundredGramPink,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = HundredGramPink
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Posts")
                        }},
                        selectedContentColor = HundredGramPink,
                        unselectedContentColor = HundredGramTextSecondary
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reels")
                        }},
                        selectedContentColor = HundredGramPink,
                        unselectedContentColor = HundredGramTextSecondary
                    )
                }

                // Grid content
                if (selectedTabIndex == 0) {
                    if (creatorPosts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No posts shared yet", color = HundredGramTextSecondary)
                        }
                    } else {
                        val chunked = remember(creatorPosts) { creatorPosts.chunked(3) }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(2.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            chunked.forEach { rowPosts ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    rowPosts.forEach { post ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .padding(1.dp)
                                                .background(HundredGramCardElevated)
                                                .clickable { selectedPreviewPost = post }
                                        ) {
                                            if (post.isTextOnly || (post.imageUrl.isBlank() && post.mediaUrl.isBlank())) {
                                                com.example.ui.components.TextPostGraphicCard(
                                                    post = post,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                val safeUrl = remember(post.imageUrl, post.mediaUrl, post.id) {
                                                    com.example.util.ImageUtils.getPostThumbnailUrl(post)
                                                }
                                                SubcomposeAsyncImage(
                                                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                                                        .data(safeUrl)
                                                        .crossfade(true)
                                                        .build(),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize(),
                                                    loading = {
                                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                            CircularProgressIndicator(
                                                                color = HundredGramPink,
                                                                modifier = Modifier.size(24.dp),
                                                                strokeWidth = 2.dp
                                                            )
                                                        }
                                                    },
                                                    error = {
                                                        com.example.ui.components.TextPostGraphicCard(
                                                            post = post,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    if (rowPosts.size < 3) {
                                        repeat(3 - rowPosts.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (creatorReels.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No reels shared yet", color = HundredGramTextSecondary)
                        }
                    } else {
                        val chunked = remember(creatorReels) { creatorReels.chunked(3) }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(2.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            chunked.forEach { rowReels ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    rowReels.forEach { reel ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(0.6f)
                                                .padding(1.dp)
                                                .background(HundredGramCardElevated)
                                                .clickable {
                                                    viewModel.openReelInPlayer(reel.id)
                                                }
                                        ) {
                                            val safeThumb = remember(reel.thumbnailUri, reel.videoUrl, reel.id) {
                                                com.example.util.ImageUtils.getReelThumbnailUrl(reel.thumbnailUri, reel.videoUrl, reel.id)
                                            }
                                            SubcomposeAsyncImage(
                                                model = coil.request.ImageRequest.Builder(LocalContext.current)
                                                    .data(safeThumb)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize(),
                                                loading = {
                                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                        CircularProgressIndicator(
                                                            color = HundredGramPink,
                                                            modifier = Modifier.size(24.dp),
                                                            strokeWidth = 2.dp
                                                        )
                                                    }
                                                },
                                                error = {
                                                    com.example.ui.components.ReelFallbackThumbnailGraphic(
                                                        reel = reel,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                            )

                                            // Reel Views Count Badge with high-contrast pill and deep gradient scrim
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .align(Alignment.BottomStart)
                                                    .background(
                                                        Brush.verticalGradient(
                                                            colors = listOf(
                                                                Color.Transparent,
                                                                Color.Black.copy(alpha = 0.5f),
                                                                Color.Black.copy(alpha = 0.95f)
                                                            ),
                                                            startY = 0f
                                                        )
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 6.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .background(
                                                            color = Color.Black.copy(alpha = 0.78f),
                                                            shape = RoundedCornerShape(6.dp)
                                                        )
                                                        .border(
                                                            width = 0.8.dp,
                                                            color = Color.White.copy(alpha = 0.35f),
                                                            shape = RoundedCornerShape(6.dp)
                                                        )
                                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = "Reel Views",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        text = formatViewCountWithLabel(reel.viewsCount),
                                                        color = Color.White,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        letterSpacing = 0.2.sp,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    if (rowReels.size < 3) {
                                        repeat(3 - rowReels.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }

    // Block Confirmation Dialog
    if (showBlockConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showBlockConfirmDialog = false },
            containerColor = HundredGramCardBackground,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Block @${creator?.username ?: "Creator"}?",
                    color = HundredGramTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "They will not be able to interact with you, and their posts, reels, and stories will be removed from your feed.",
                    color = HundredGramTextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBlockConfirmDialog = false
                        viewModel.blockUser(
                            userId = userId,
                            username = creator?.username ?: "Creator",
                            displayName = creator?.displayName ?: "",
                            avatarUrl = creator?.avatarUrl ?: ""
                        )
                        android.widget.Toast.makeText(
                            context,
                            "User @${creator?.username ?: "creator"} blocked (ब्लॉक कर दिया गया)",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        viewModel.navigateTo(ScreenDestination.Feed)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.HundredGramLikeRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Block (ब्लॉक करें)", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockConfirmDialog = false }) {
                    Text("Cancel", color = HundredGramTextSecondary)
                }
            }
        )
    }

    // Report Creator Dialog
    if (showReportCreatorDialog) {
        val reasons = listOf(
            "Spam or fake profile (स्पैम या फर्जी प्रोफ़ाइल)",
            "Hate speech or harassment (घृणास्पद भाषण / उत्पीड़न)",
            "Inappropriate content (अनुचित सामग्री)",
            "Impersonation (किसी अन्य व्यक्ति की नकल)",
            "Violence or dangerous content (हिंसा / खतरनाक सामग्री)"
        )
        var selectedReason by remember { mutableStateOf(reasons[0]) }

        AlertDialog(
            onDismissRequest = { showReportCreatorDialog = false },
            containerColor = HundredGramCardBackground,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Report @${creator?.username ?: "Creator"}",
                    color = HundredGramTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select a reason for reporting:", color = HundredGramTextSecondary, fontSize = 13.sp)
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
                        showReportCreatorDialog = false
                        viewModel.reportContent(
                            targetId = userId,
                            targetType = "Creator",
                            authorUsername = creator?.username ?: "Creator",
                            reason = selectedReason
                        )
                        android.widget.Toast.makeText(
                            context,
                            "Report submitted for review (रिपोर्ट दर्ज कर ली गई)",
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
                TextButton(onClick = { showReportCreatorDialog = false }) {
                    Text("Cancel", color = HundredGramTextSecondary)
                }
            }
        )
    }

    if (showFollowersListSheet && creator != null) {
        val u = creator!!
        com.example.ui.components.FollowersListBottomSheet(
            targetUserId = u.userId,
            targetUsername = u.username,
            initialTab = followersListTab,
            isPrivateAccount = u.isPrivate,
            isFollowingTarget = isFollowing,
            onDismiss = { showFollowersListSheet = false },
            onUserClick = { clickedUserId ->
                showFollowersListSheet = false
                if (clickedUserId == viewModel.currentUser.value.userId) {
                    viewModel.navigateTo(com.example.ui.ScreenDestination.Profile)
                } else {
                    viewModel.navigateTo(com.example.ui.ScreenDestination.CreatorProfile(clickedUserId))
                }
            },
            viewModel = viewModel
        )
    }
}

@Composable
fun CreatorProfileStatColumn(count: Int, label: String, onClick: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = if (onClick != null) {
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onClick() }
                .padding(horizontal = 6.dp, vertical = 2.dp)
        } else {
            Modifier
        }
    ) {
        Text(
            text = count.toString(),
            color = HundredGramTextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = HundredGramTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", count / 1_000_000.0)
        count >= 10_000 -> String.format(java.util.Locale.US, "%.1fK", count / 1_000.0)
        count >= 1_000 -> String.format(java.util.Locale.US, "%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}

private fun formatViewCountWithLabel(count: Int): String {
    val formatted = formatCount(count)
    return when (count) {
        1 -> "$formatted view"
        else -> "$formatted views"
    }
}
