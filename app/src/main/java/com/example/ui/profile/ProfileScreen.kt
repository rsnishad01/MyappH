package com.example.ui.profile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.launch
import com.example.util.ShareManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Surface
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import coil.compose.SubcomposeAsyncImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import com.example.ui.components.TagFriendsDialog
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeUp
import com.example.ui.feed.FullScreenPostDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.PostEntity
import com.example.ui.MainViewModel
import com.example.ui.components.UserAvatar
import com.example.ui.components.GradientActionButton
import com.example.ui.components.SystemCompatibilityBadgeRow
import com.example.ui.components.PrivacySelector
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import kotlinx.coroutines.delay
import com.example.data.PrivacyLevel
import com.example.ui.feed.EditPostBottomSheet
import com.example.ui.feed.PostOptionsSheet
import com.example.ui.theme.HundredGramCardBackground
import com.example.ui.theme.HundredGramCardElevated
import com.example.ui.theme.HundredGramDarkBackground
import com.example.ui.theme.HundredGramDivider
import com.example.ui.theme.HundredGramPink
import com.example.ui.theme.HundredGramTextPrimary
import com.example.ui.theme.HundredGramTextSecondary

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.ripple.rememberRipple
import com.example.ui.components.GradientActionButton
import com.example.ui.components.SecondaryActionButton
import com.example.ui.ScreenDestination
import com.example.ui.theme.HundredGramBorderSubtle
import com.example.ui.theme.HundredGramButtonGradient
import com.example.ui.theme.HundredGramCardGlass
import com.example.ui.theme.ThemeConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: MainViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val posts by viewModel.posts.collectAsState()
    val reels by viewModel.reels.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val context = LocalContext.current

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showCreateTextPostSheet by remember { mutableStateOf(false) }
    var showProfileMenuSheet by remember { mutableStateOf(false) }
    var showPermissionsDialog by remember { mutableStateOf(false) }
    var showSoundSettingsDialog by remember { mutableStateOf(false) }
    var showBlockedUsersDialog by remember { mutableStateOf(false) }
    var showReportedContentDialog by remember { mutableStateOf(false) }
    var showDashboardDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showMoreAccountSettingsDialog by remember { mutableStateOf(false) }
    var showFollowersListSheet by remember { mutableStateOf(false) }
    var followersListTab by remember { mutableIntStateOf(0) }
    var activeOptionsPost by remember { mutableStateOf<PostEntity?>(null) }
    var activeEditPost by remember { mutableStateOf<PostEntity?>(null) }
    var activePostDetail by remember { mutableStateOf<PostEntity?>(null) }
    var activeReelDetail by remember { mutableStateOf<com.example.data.ReelVideo?>(null) }
    var activeOptionsReel by remember { mutableStateOf<com.example.data.ReelVideo?>(null) }
    var activeEditReel by remember { mutableStateOf<com.example.data.ReelVideo?>(null) }
    var showFullScreenAvatar by remember { mutableStateOf(false) }

    val profilePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onUpdateAvatar(uri)
            showFullScreenAvatar = false
        }
    }

    val userPosts = remember(posts, currentUser.userId) {
        posts.filter { it.authorId == currentUser.userId }
    }

    val userReels = remember(reels, currentUser.userId) {
        reels.filter { it.authorId == currentUser.userId }
    }

    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(HundredGramDarkBackground)
                .verticalScroll(rememberScrollState())
        ) {
        // Profile Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentUser.username,
                color = HundredGramTextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick Sound Settings Button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(HundredGramCardElevated)
                        .border(1.dp, HundredGramBorderSubtle.copy(alpha = 0.5f), CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, radius = 19.dp),
                            onClick = { showSoundSettingsDialog = true }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Sound Settings (साउंड सेटिंग्स)",
                        tint = HundredGramPink,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Options & Settings Menu Button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(HundredGramCardElevated)
                        .border(1.dp, HundredGramBorderSubtle.copy(alpha = 0.5f), CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, radius = 19.dp),
                            onClick = { showProfileMenuSheet = true }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu & Settings",
                        tint = HundredGramTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Avatar and Stats Row inside Elevated Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(HundredGramCardBackground)
                .border(1.dp, HundredGramBorderSubtle.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(88.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .size(86.dp)
                                .align(Alignment.Center)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, radius = 43.dp),
                                    onClick = { showFullScreenAvatar = true }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            UserAvatar(
                                avatarUrl = currentUser.avatarUrl,
                                size = 76.dp,
                                hasActiveStory = true,
                                isVerified = currentUser.isVerified,
                                isOnline = currentUser.isOnline,
                                showOnlineStatus = currentUser.showOnlineStatus,
                                onClick = { showFullScreenAvatar = true }
                            )
                        }
                        IconButton(
                            onClick = { profilePhotoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(HundredGramPink)
                                .border(2.dp, HundredGramDarkBackground, CircleShape)
                                .padding(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Update Profile Photo",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        ProfileStatColumn(count = userPosts.size, label = "Posts")
                        ProfileStatColumn(
                            count = currentUser.followersCount,
                            label = "Followers",
                            onClick = {
                                showFollowersListSheet = true
                                followersListTab = 0
                            }
                        )
                        ProfileStatColumn(
                            count = currentUser.followingCount,
                            label = "Following",
                            onClick = {
                                showFollowersListSheet = true
                                followersListTab = 1
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Small Box right next to profile picture / stats: [ROAR 🦁]
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(HundredGramCardElevated)
                        .border(1.dp, HundredGramPink.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable { showCreateTextPostSheet = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Login,
                                contentDescription = "Roar Login Icon",
                                tint = HundredGramPink,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "[ROAR 🦁]",
                                color = HundredGramPink,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            text = "Post Roar 🦁",
                            color = HundredGramTextSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Bio Section
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
            Text(
                text = currentUser.displayName,
                color = HundredGramTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            if (currentUser.bio.isNotBlank()) {
                Text(
                    text = currentUser.bio,
                    color = HundredGramTextPrimary,
                    fontSize = 13.5.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (currentUser.website.isNotBlank()) {
                Text(
                    text = currentUser.website,
                    color = Color(0xFF3897F0),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SecondaryActionButton(
                text = "Edit Profile",
                onClick = { showEditProfileDialog = true },
                modifier = Modifier.weight(1f),
                height = 38.dp
            )
            SecondaryActionButton(
                text = "Share Profile",
                onClick = { ShareManager.shareProfile(context, currentUser) },
                modifier = Modifier.weight(1f),
                height = 38.dp
            )
            if (isLoggedIn) {
                SecondaryActionButton(
                    text = "Dashboard 📊",
                    onClick = { showDashboardDialog = true },
                    modifier = Modifier.weight(1.2f),
                    height = 38.dp
                )
            } else {
                SecondaryActionButton(
                    text = "Account / Login",
                    onClick = { viewModel.navigateTo(com.example.ui.ScreenDestination.Auth) },
                    modifier = Modifier.weight(1.2f),
                    height = 38.dp
                )
            }
        }



        // Profile Tabs (Posts Grid, Reels, Tagged)
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = HundredGramDarkBackground,
            contentColor = HundredGramPink,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = HundredGramPink,
                    height = 2.5.dp
                )
            }
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                icon = { Icon(Icons.Default.GridOn, contentDescription = "Posts") }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                icon = { Icon(Icons.Default.Movie, contentDescription = "Reels") }
            )
            Tab(
                selected = selectedTabIndex == 2,
                onClick = { selectedTabIndex = 2 },
                icon = { Icon(Icons.Default.PersonPin, contentDescription = "Tagged") }
            )
        }

        // Grid Content
        when (selectedTabIndex) {
            0 -> {
                if (userPosts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp)
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No Posts Yet", color = HundredGramTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("When you share photos, they will appear on your profile.", color = HundredGramTextSecondary, fontSize = 13.sp)
                        }
                    }
                } else {
                    val chunked = remember(userPosts) { userPosts.chunked(3) }
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
                                            .background(HundredGramCardElevated)
                                            .clickable { activePostDetail = post }
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
                                                contentDescription = post.caption,
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

                                        // Quick Edit/Options button on top-right of own post
                                        IconButton(
                                            onClick = { activeOptionsPost = post },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(24.dp)
                                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "Post Options",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
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
            }
            1 -> {
                if (userReels.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp)
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No Reels Yet", color = HundredGramTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Create and watch short fun clips.", color = HundredGramTextSecondary, fontSize = 13.sp)
                        }
                    }
                } else {
                    val chunked = remember(userReels) { userReels.chunked(3) }
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
                                            .aspectRatio(0.65f)
                                            .background(HundredGramCardElevated)
                                            .clickable {
                                                viewModel.recordReelView(reel.id)
                                                activeReelDetail = reel
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
                                            contentDescription = reel.caption,
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

                                        // Reel Views Count Badge with high-contrast pill and deep gradient scrim (Clearly visible on any video)
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

                                        // Quick Edit/Options button on top-right of own reel
                                        IconButton(
                                            onClick = { activeOptionsReel = reel },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(24.dp)
                                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "Reel Options",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
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
            2 -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp)
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No tagged photos yet", color = HundredGramTextSecondary)
                }
            }
        }
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            user = currentUser,
            viewModel = viewModel,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, username, bio, site, isPrivate, showOnlineStatus ->
                viewModel.onUpdateProfile(name, username, bio, site, isPrivate, showOnlineStatus)
                showEditProfileDialog = false
            }
        )
    }

    if (showCreateTextPostSheet) {
        CreateTextPostSheet(
            viewModel = viewModel,
            onDismiss = { showCreateTextPostSheet = false }
        )
    }

    // Post Options Sheet
    activeOptionsPost?.let { post ->
        PostOptionsSheet(
            post = post,
            isOwner = true,
            onDismiss = { activeOptionsPost = null },
            onSaveToggle = { viewModel.onBookmarkPost(post.id) },
            onShare = {
                ShareManager.sharePost(context, post)
                activeOptionsPost = null
            },
            onEdit = {
                activeOptionsPost = null
                activeEditPost = post
            },
            onDelete = { viewModel.onDeletePost(post.id) }
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

    // Reel Options Sheet
    activeOptionsReel?.let { reel ->
        ReelOptionsSheet(
            reel = reel,
            isOwner = true,
            onDismiss = { activeOptionsReel = null },
            onSaveToggle = { viewModel.onBookmarkReel(reel.id) },
            onShare = {
                ShareManager.shareReel(context, reel)
                activeOptionsReel = null
            },
            onEdit = {
                activeOptionsReel = null
                activeEditReel = reel
            },
            onDelete = { viewModel.onDeleteReel(reel.id) }
        )
    }

    // Edit Reel Sheet
    activeEditReel?.let { reel ->
        EditReelBottomSheet(
            reel = reel,
            viewModel = viewModel,
            onDismiss = { activeEditReel = null },
            onSave = { newCaption, newAudioTitle, newAudioArtist, newPrivacy, newTags ->
                viewModel.onEditReel(reel.id, newCaption, newAudioTitle, newAudioArtist, newPrivacy, newTags)
                activeEditReel = null
            },
            onDelete = {
                viewModel.onDeleteReel(reel.id)
                activeEditReel = null
            }
        )
    }

    if (showProfileMenuSheet) {
        AlertDialog(
            onDismissRequest = { showProfileMenuSheet = false },
            containerColor = HundredGramCardBackground,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("Options & Settings", color = HundredGramTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(HundredGramCardElevated)
                            .clickable {
                                showProfileMenuSheet = false
                                showSoundSettingsDialog = true
                            }
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🔊", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Sound & Vibration (साउंड व वाइब्रेशन सेटिंग्स)", color = HundredGramTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Audio alerts, call ringtones & vibration toggles", color = HundredGramTextSecondary, fontSize = 12.sp)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(HundredGramCardElevated)
                            .clickable {
                                showProfileMenuSheet = false
                                showPermissionsDialog = true
                            }
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🔐", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("App Permissions (ऐप अनुमति)", color = HundredGramTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Camera, Audio, Location & Storage", color = HundredGramTextSecondary, fontSize = 12.sp)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(HundredGramCardElevated)
                            .clickable {
                                showProfileMenuSheet = false
                                ThemeConfig.isDark = !ThemeConfig.isDark
                            }
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🌓", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Dark / Light Mode (डार्क / लाइट मोड)", color = HundredGramTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Switch screen appearance", color = HundredGramTextSecondary, fontSize = 12.sp)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(HundredGramCardElevated)
                            .clickable {
                                showProfileMenuSheet = false
                                showBlockedUsersDialog = true
                            }
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🚫", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Blocked Creators (ब्लॉक किए गए क्रिएटर)", color = HundredGramTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("View and unblock restricted creators", color = HundredGramTextSecondary, fontSize = 12.sp)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(HundredGramCardElevated)
                            .clickable {
                                showProfileMenuSheet = false
                                showReportedContentDialog = true
                            }
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚠️", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Reported Content (रिपोर्ट की गई सामग्री)", color = HundredGramTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("View reports submitted by you and status", color = HundredGramTextSecondary, fontSize = 12.sp)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(HundredGramCardElevated)
                            .clickable {
                                showProfileMenuSheet = false
                                viewModel.navigateTo(ScreenDestination.Auth)
                            }
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("👤", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Switch Account / Log In (खाता बदलें)", color = HundredGramTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Google, Facebook or Phone OTP", color = HundredGramTextSecondary, fontSize = 12.sp)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(HundredGramCardElevated)
                            .clickable {
                                showProfileMenuSheet = false
                                showMoreAccountSettingsDialog = true
                            }
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🛡️", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("More settings in account (अकाउंट की और सेटिंग्स)", color = HundredGramTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Manage account deletion and security settings", color = HundredGramTextSecondary, fontSize = 12.sp)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(HundredGramCardElevated)
                            .clickable {
                                showProfileMenuSheet = false
                                viewModel.logout()
                            }
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🚪", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Log Out (लॉगआउट करें)", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Sign out securely from this device", color = HundredGramTextSecondary, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 🛡️ App Info & Copyright Notice
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "HundredGram • v2.4.0",
                            color = HundredGramTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "© 2026 HundredGram. All rights reserved.",
                            color = HundredGramTextSecondary.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfileMenuSheet = false }) {
                    Text("Close", color = HundredGramPink)
                }
            }
        )
    }

    if (showPermissionsDialog) {
        AppPermissionsDialog(
            onDismiss = { showPermissionsDialog = false }
        )
    }

    if (showSoundSettingsDialog) {
        SoundSettingsDialog(
            onDismiss = { showSoundSettingsDialog = false }
        )
    }

    if (showBlockedUsersDialog) {
        BlockedUsersDialog(
            viewModel = viewModel,
            onDismiss = { showBlockedUsersDialog = false }
        )
    }

    if (showReportedContentDialog) {
        ReportedContentDialog(
            viewModel = viewModel,
            onDismiss = { showReportedContentDialog = false }
        )
    }

    if (showMoreAccountSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showMoreAccountSettingsDialog = false },
            containerColor = HundredGramCardBackground,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("More settings in account", color = HundredGramTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "अकाउंट से जुड़ी अन्य सेटिंग्स और खाता प्रबंधन (Account Management & Security)",
                        color = HundredGramTextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF2C1619))
                            .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable {
                                showMoreAccountSettingsDialog = false
                                showDeleteAccountDialog = true
                            }
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🗑️", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Delete Account (खाता डिलीट करें)", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Deactivate account with 15 days grace period", color = HundredGramTextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMoreAccountSettingsDialog = false }) {
                    Text("Close", color = HundredGramPink)
                }
            }
        )
    }

    if (showDeleteAccountDialog) {
        DeleteAccountConfirmationDialog(
            currentUser = currentUser,
            viewModel = viewModel,
            onDismiss = { showDeleteAccountDialog = false }
        )
    }

    if (showDashboardDialog) {
        CreatorDashboardDialog(
            userPosts = userPosts,
            userReels = userReels,
            username = currentUser.username,
            onDismiss = { showDashboardDialog = false }
        )
    }

    activePostDetail?.let { post ->
        FullScreenPostDialog(
            post = post,
            onDismiss = { activePostDetail = null },
            viewModel = viewModel,
            currentUser = currentUser
        )
    }

    activeReelDetail?.let { reel ->
        FullScreenReelDialog(
            reel = reel,
            onDismiss = { activeReelDetail = null },
            viewModel = viewModel
        )
    }

    if (showFullScreenAvatar) {
        FullScreenAvatarDialog(
            avatarUrl = currentUser.avatarUrl,
            username = currentUser.username,
            displayName = currentUser.displayName,
            onDismiss = { showFullScreenAvatar = false },
            onUpdateAvatarClick = {
                profilePhotoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        )
    }

    if (showFollowersListSheet) {
        com.example.ui.components.FollowersListBottomSheet(
            targetUserId = currentUser.userId,
            targetUsername = currentUser.username,
            initialTab = followersListTab,
            isPrivateAccount = currentUser.isPrivate,
            isFollowingTarget = true,
            onDismiss = { showFollowersListSheet = false },
            onUserClick = { clickedUserId ->
                showFollowersListSheet = false
                if (clickedUserId == currentUser.userId) {
                    viewModel.navigateTo(ScreenDestination.Profile)
                } else {
                    viewModel.navigateTo(ScreenDestination.CreatorProfile(clickedUserId))
                }
            },
            viewModel = viewModel
        )
    }
}
}


@Composable
fun BlockedUsersDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val blockedUsers by viewModel.blockedUsers.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HundredGramCardBackground,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Blocked Creators (ब्लॉक सूची)",
                color = HundredGramTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Blocked creators will not appear in your feed, reels, or stories.",
                    color = HundredGramTextSecondary,
                    fontSize = 12.sp
                )
                
                if (blockedUsers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No blocked creators (कोई ब्लॉक क्रिएटर नहीं है)", color = HundredGramTextSecondary, fontSize = 13.sp)
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(blockedUsers.size) { idx ->
                            val user = blockedUsers[idx]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(HundredGramCardElevated)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    UserAvatar(avatarUrl = user.avatarUrl, size = 36.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "@${user.username}",
                                            color = HundredGramTextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        if (user.displayName.isNotBlank()) {
                                            Text(
                                                text = user.displayName,
                                                color = HundredGramTextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                                Button(
                                    onClick = { viewModel.unblockUser(user.userId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = HundredGramPink),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Unblock", fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            GradientActionButton(
                text = "Done",
                onClick = onDismiss,
                height = 38.dp
            )
        }
    )
}

@Composable
fun ReportedContentDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val reportedItems by viewModel.reportedItems.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HundredGramCardBackground,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Reported Content (रिपोर्ट की गई सामग्री)",
                color = HundredGramTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Reports submitted by you and moderation review status.",
                    color = HundredGramTextSecondary,
                    fontSize = 12.sp
                )

                if (reportedItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No reported content (कोई रिपोर्ट नहीं की गई)", color = HundredGramTextSecondary, fontSize = 13.sp)
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(reportedItems.size) { idx ->
                            val item = reportedItems[idx]
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(HundredGramCardElevated)
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${item.targetType} by @${item.authorUsername}",
                                        color = HundredGramTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFE91E63).copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = item.status,
                                            color = HundredGramPink,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Reason: ${item.reason}",
                                    color = HundredGramTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            GradientActionButton(
                text = "Close",
                onClick = onDismiss,
                height = 38.dp
            )
        }
    )
}

@Composable
fun ProfileStatColumn(count: Int, label: String, onClick: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
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
            text = "$count",
            color = HundredGramTextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
        )
        Text(
            text = label,
            color = HundredGramTextSecondary,
            fontSize = 12.sp
        )
    }
}

@Composable
fun EditProfileDialog(
    user: com.example.data.UserData,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSave: (name: String, username: String, bio: String, site: String, isPrivate: Boolean, showOnlineStatus: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(user.displayName) }
    var username by remember { mutableStateOf(user.username) }
    var bio by remember { mutableStateOf(user.bio) }
    var site by remember { mutableStateOf(user.website) }
    var isPrivate by remember { mutableStateOf(user.isPrivate) }
    var showOnlineStatus by remember { mutableStateOf(user.showOnlineStatus) }

    var usernameCheckMessage by remember { mutableStateOf("available ✓") }
    var isUsernameAvailable by remember { mutableStateOf(true) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HundredGramCardBackground,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Edit Profile",
                color = HundredGramTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = HundredGramTextPrimary,
                        unfocusedTextColor = HundredGramTextPrimary,
                        focusedBorderColor = HundredGramPink,
                        unfocusedBorderColor = HundredGramDivider
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { newValue ->
                        val filtered = newValue.filter { it.isLetterOrDigit() }
                        username = filtered
                        scope.launch {
                            if (filtered.isBlank()) {
                                isUsernameAvailable = false
                                usernameCheckMessage = "Username cannot be blank ❌"
                            } else if (filtered.lowercase() == user.username.lowercase()) {
                                isUsernameAvailable = true
                                usernameCheckMessage = "available ✓"
                            } else {
                                val available = viewModel.isUsernameAvailable(filtered)
                                isUsernameAvailable = available
                                usernameCheckMessage = if (available) "available ✓" else "already existed ×"
                            }
                        }
                    },
                    label = { Text("Username (Letters & numbers only)") },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = HundredGramTextPrimary,
                        unfocusedTextColor = HundredGramTextPrimary,
                        focusedBorderColor = HundredGramPink,
                        unfocusedBorderColor = HundredGramDivider
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (username.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            text = usernameCheckMessage,
                            color = if (isUsernameAvailable) Color(0xFF22C55E) else Color.Red,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio") },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = HundredGramTextPrimary,
                        unfocusedTextColor = HundredGramTextPrimary,
                        focusedBorderColor = HundredGramPink,
                        unfocusedBorderColor = HundredGramDivider
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = site,
                    onValueChange = { site = it },
                    label = { Text("Website") },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = HundredGramTextPrimary,
                        unfocusedTextColor = HundredGramTextPrimary,
                        focusedBorderColor = HundredGramPink,
                        unfocusedBorderColor = HundredGramDivider
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                androidx.compose.material3.HorizontalDivider(
                    color = HundredGramDivider.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Text(
                    text = "🔒 Privacy Options (प्राइवेसी ऑप्शंस)",
                    color = HundredGramPink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Private Account (प्राइवेट अकाउंट)", color = HundredGramTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                        Text("Only approved followers can view your posts", color = HundredGramTextSecondary, fontSize = 11.5.sp)
                    }
                    androidx.compose.material3.Switch(
                        checked = isPrivate,
                        onCheckedChange = { isPrivate = it },
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = HundredGramPink,
                            checkedTrackColor = HundredGramPink.copy(alpha = 0.5f)
                        )
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Online Status (🟢 ऑनलाइन स्टेटस)", color = HundredGramTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                        Text("Show green dot when active on HundredGram", color = HundredGramTextSecondary, fontSize = 11.5.sp)
                    }
                    androidx.compose.material3.Switch(
                        checked = showOnlineStatus,
                        onCheckedChange = { showOnlineStatus = it },
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = HundredGramPink,
                            checkedTrackColor = HundredGramPink.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        },
        confirmButton = {
            GradientActionButton(
                text = "Save Changes",
                onClick = { 
                    if (isUsernameAvailable && username.isNotBlank()) {
                        onSave(name, username, bio, site, isPrivate, showOnlineStatus) 
                    }
                },
                height = 38.dp,
                enabled = isUsernameAvailable && username.isNotBlank()
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = HundredGramTextSecondary, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CreateTextPostSheet(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var textInput by remember { mutableStateOf("") }
    var captionInput by remember { mutableStateOf("") }
    
    var selectedGradient by remember { mutableStateOf("Sunset") }
    var selectedFontStyle by remember { mutableStateOf("Bold") }
    var selectedFontColor by remember { mutableStateOf("#FFFFFF") }
    var selectedAlignment by remember { mutableStateOf("Center") }
    var privacy by remember { mutableStateOf(PrivacyLevel.PUBLIC) }

    val gradientOptions = listOf(
        "Sunset" to Pair("Sunset 🌅", listOf(Color(0xFFFF512F), Color(0xFFDD2476))),
        "Midnight" to Pair("Midnight 🌌", listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))),
        "Neon Cyber" to Pair("Neon ⚡", listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0))),
        "Rose Gold" to Pair("Rose 🌸", listOf(Color(0xFFEC6EAD), Color(0xFFC91E63))),
        "Emerald" to Pair("Emerald 🌿", listOf(Color(0xFF0575E6), Color(0xFF00F260))),
        "Ocean Blue" to Pair("Ocean 🌊", listOf(Color(0xFF2193b0), Color(0xFF6dd5ed))),
        "Velvet" to Pair("Velvet 🍇", listOf(Color(0xFF3A1C71), Color(0xFFD76D77), Color(0xFFFFAF7B))),
        "Gold Luxury" to Pair("Gold 💫", listOf(Color(0xFFF2994A), Color(0xFFF2C94C))),
        "Pure Black" to Pair("Dark 🖤", listOf(Color(0xFF181824), Color(0xFF222232)))
    )

    val fontStyles = listOf("Bold", "Italic", "Serif", "Cursive", "Normal")

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = HundredGramCardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            PrivacySelector(selectedPrivacy = privacy, onPrivacySelected = { privacy = it })
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ROAR 🦁 Post",
                    color = HundredGramTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            viewModel.onCreateTextPost(
                                text = textInput,
                                bgGradient = selectedGradient,
                                fontStyle = selectedFontStyle,
                                fontColor = selectedFontColor,
                                alignment = selectedAlignment,
                                caption = captionInput,
                                privacy = privacy
                            )
                            onDismiss()
                        }
                    },
                    enabled = textInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = HundredGramPink),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text("ROAR 🦁", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Styled Graphic Card Live Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                val currentPair = gradientOptions.find { it.first == selectedGradient }?.second
                val brush = Brush.linearGradient(currentPair?.second ?: listOf(Color(0xFFFF512F), Color(0xFFDD2476)))
                val parsedColor = try { Color(android.graphics.Color.parseColor(selectedFontColor)) } catch (_: Exception) { Color.White }
                val fontWeight = if (selectedFontStyle == "Bold") FontWeight.ExtraBold else FontWeight.Normal
                val fontStyle = if (selectedFontStyle == "Italic" || selectedFontStyle == "Cursive") androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal
                val fontFamily = when (selectedFontStyle) {
                    "Serif" -> androidx.compose.ui.text.font.FontFamily.Serif
                    "Cursive" -> androidx.compose.ui.text.font.FontFamily.Cursive
                    else -> androidx.compose.ui.text.font.FontFamily.Default
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(brush)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (textInput.isBlank()) {
                        Text(
                            text = "Type your thoughts, quotes or status here...",
                            color = parsedColor.copy(alpha = 0.6f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = textInput,
                            color = parsedColor,
                            fontSize = if (textInput.length < 50) 20.sp else 16.sp,
                            fontWeight = fontWeight,
                            fontStyle = fontStyle,
                            fontFamily = fontFamily,
                            textAlign = if (selectedAlignment == "Left") TextAlign.Start else TextAlign.Center,
                            lineHeight = 26.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Text Input
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                label = { Text("Your Thought / Status Text (विचार/स्टेटस लिखें)") },
                placeholder = { Text("What's on your mind? Share thoughts...") },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = HundredGramTextPrimary,
                    unfocusedTextColor = HundredGramTextPrimary,
                    focusedBorderColor = HundredGramPink,
                    unfocusedBorderColor = HundredGramDivider
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Background Style Selector
            Text("🎨 Background Color & Gradient:", color = HundredGramTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                gradientOptions.forEach { (key, pair) ->
                    val isSelected = selectedGradient == key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(pair.second))
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) HundredGramPink else Color.Transparent,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { selectedGradient = key }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = pair.first,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Font Style Selector Row
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("✍️ Font Style:", color = HundredGramTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    fontStyles.forEach { style ->
                        val isSelected = selectedFontStyle == style
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) HundredGramPink else HundredGramCardElevated)
                                .clickable { selectedFontStyle = style }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(style, color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Font Color Selector Row
            Text("🎨 Text Color (अक्षर का रंग):", color = HundredGramTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            val fontColors = listOf(
                "White" to "#FFFFFF",
                "Yellow" to "#FFE500",
                "Cyan" to "#00E5FF",
                "Green" to "#00FF66",
                "Pink" to "#FF007F",
                "Orange" to "#FF8000",
                "Black" to "#000000"
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                fontColors.forEach { (cName, hex) ->
                    val isSelected = selectedFontColor == hex
                    val parsedC = try { Color(android.graphics.Color.parseColor(hex)) } catch(_: Exception) { Color.White }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(parsedC)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) HundredGramPink else Color.Gray,
                                shape = CircleShape
                            )
                            .clickable { selectedFontColor = hex }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Text Alignment Selector Row
            Text("📐 Alignment (अलाइनमेंट):", color = HundredGramTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("Left" to "Left ⬅️", "Center" to "Center ↔️", "Right" to "Right ➡️").forEach { (alignVal, label) ->
                    val isSelected = selectedAlignment == alignVal
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) HundredGramPink else HundredGramCardElevated)
                            .clickable { selectedAlignment = alignVal }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Optional Caption Input
            OutlinedTextField(
                value = captionInput,
                onValueChange = { captionInput = it },
                label = { Text("Additional Caption / Hashtags (Optional)") },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = HundredGramTextPrimary,
                    unfocusedTextColor = HundredGramTextPrimary,
                    focusedBorderColor = HundredGramPink,
                    unfocusedBorderColor = HundredGramDivider
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
fun AppPermissionsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshKey++
    }

    val hasCamera = remember(refreshKey) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    val hasAudio = remember(refreshKey) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }
    val hasLocation = remember(refreshKey) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
    val hasMedia = remember(refreshKey) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }
    val hasNotification = remember(refreshKey) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HundredGramCardBackground,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔐 ", fontSize = 18.sp)
                Text(
                    text = "App Permissions (ऐप अनुमति)",
                    color = HundredGramTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "HundredGram requires permissions for camera recording, audio filters, reels, and media uploads:",
                    color = Color.LightGray,
                    fontSize = 12.5.sp,
                    lineHeight = 16.sp
                )

                PermissionStatusItem(
                    emoji = "📸",
                    title = "Camera (कैमरा)",
                    subtitle = "For photos, stories & reels",
                    isGranted = hasCamera
                )

                PermissionStatusItem(
                    emoji = "🎤",
                    title = "Microphone (ऑडियो)",
                    subtitle = "For reel sound & voice calls",
                    isGranted = hasAudio
                )

                PermissionStatusItem(
                    emoji = "📍",
                    title = "Location (लोकेशन)",
                    subtitle = "For geotagging posts & explore",
                    isGranted = hasLocation
                )

                PermissionStatusItem(
                    emoji = "🖼️",
                    title = "Photos & Videos (स्टोरेज)",
                    subtitle = "To select & compress media",
                    isGranted = hasMedia
                )
            }
        },
        confirmButton = {
            GradientActionButton(
                text = "अनुमति दें (Grant All)",
                onClick = {
                    val list = mutableListOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        list.add(Manifest.permission.POST_NOTIFICATIONS)
                        list.add(Manifest.permission.READ_MEDIA_IMAGES)
                        list.add(Manifest.permission.READ_MEDIA_VIDEO)
                    }
                    permissionLauncher.launch(list.toTypedArray())
                },
                height = 38.dp
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = HundredGramTextSecondary)
            }
        }
    )
}

@Composable
private fun PermissionStatusItem(
    emoji: String,
    title: String,
    subtitle: String,
    isGranted: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(HundredGramCardElevated)
            .padding(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(emoji, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = HundredGramTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(subtitle, color = HundredGramTextSecondary, fontSize = 11.sp)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isGranted) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (isGranted) "✓ Granted" else "✕ Denied",
                    color = if (isGranted) Color(0xFF10B981) else Color(0xFFEF4444),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CreatorDashboardDialog(
    userPosts: List<PostEntity>,
    userReels: List<com.example.data.ReelVideo>,
    username: String,
    onDismiss: () -> Unit
) {
    val totalPosts = userPosts.size
    val totalReels = userReels.size
    val totalLikes = userPosts.sumOf { it.likesCount } + userReels.sumOf { it.likesCount }
    val totalComments = userPosts.sumOf { it.commentsCount } + userReels.sumOf { it.commentsCount }
    
    // Dynamic calculation of gifts this month (e.g. 5 gifts per reel and 1.5 gifts per like)
    val totalGifts = ((totalLikes * 1.5) + (totalReels * 5) + 3).toInt()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HundredGramCardBackground,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "📊 Creator Dashboard",
                    color = HundredGramPink,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Analytics for @$username",
                    color = HundredGramTextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "इस महीने की परफॉरमेंस रिपोर्ट (September 2026)",
                    color = HundredGramTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // Row 1: Uploads Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Posts Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(HundredGramCardElevated)
                            .border(androidx.compose.foundation.BorderStroke(1.dp, HundredGramBorderSubtle.copy(alpha = 0.3f)), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("📸 Posts", color = HundredGramTextSecondary, fontSize = 11.sp)
                            Text(
                                text = "$totalPosts",
                                color = HundredGramTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Text("Uploaded", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    // Reels Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(HundredGramCardElevated)
                            .border(androidx.compose.foundation.BorderStroke(1.dp, HundredGramBorderSubtle.copy(alpha = 0.3f)), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("🎥 Reels", color = HundredGramTextSecondary, fontSize = 11.sp)
                            Text(
                                text = "$totalReels",
                                color = HundredGramTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Text("Uploaded", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Row 2: Engagement Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Likes Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(HundredGramCardElevated)
                            .border(androidx.compose.foundation.BorderStroke(1.dp, HundredGramBorderSubtle.copy(alpha = 0.3f)), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("❤️ Likes", color = HundredGramTextSecondary, fontSize = 11.sp)
                            Text(
                                text = "$totalLikes",
                                color = HundredGramTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Text("Received", color = HundredGramPink, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    // Comments Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(HundredGramCardElevated)
                            .border(androidx.compose.foundation.BorderStroke(1.dp, HundredGramBorderSubtle.copy(alpha = 0.3f)), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("💬 Comments", color = HundredGramTextSecondary, fontSize = 11.sp)
                            Text(
                                text = "$totalComments",
                                color = HundredGramTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Text("Received", color = Color(0xFF3B82F6), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Special Gifts Received Section (🎁)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                colors = listOf(
                                    HundredGramPink.copy(alpha = 0.15f),
                                    Color(0xFF8B5CF6).copy(alpha = 0.15f)
                                )
                            )
                        )
                        .border(
                            androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Brush.horizontalGradient(
                                colors = listOf(
                                    HundredGramPink.copy(alpha = 0.5f),
                                    Color(0xFF8B5CF6).copy(alpha = 0.5f)
                                )
                            )), 
                            RoundedCornerShape(16.dp)
                        )
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("🎁", fontSize = 28.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Earned Virtual Gifts",
                                color = HundredGramTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "$totalGifts Received This Month",
                                color = HundredGramTextPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Text(
                                text = "Your audience sent you special stickers, hearts & stars on your Reels!",
                                color = HundredGramTextSecondary,
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = HundredGramPink),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Awesome!", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun FullScreenReelDialog(
    reel: com.example.data.ReelVideo,
    onDismiss: () -> Unit,
    viewModel: MainViewModel
) {
    val playbackInfo by viewModel.audioPlaybackInfo.collectAsState()
    val commentsMap by viewModel.comments.collectAsState()
    var activeCommentReelId by remember { mutableStateOf<String?>(null) }
    var activeShareReel by remember { mutableStateOf<com.example.data.ReelVideo?>(null) }
    val context = LocalContext.current

    LaunchedEffect(reel.id) {
        viewModel.recordReelView(reel.id)
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            com.example.ui.reels.ReelItem(
                reel = reel,
                isMuted = playbackInfo.isMuted,
                onLike = { viewModel.onLikeReel(reel.id) },
                onLikesClick = { },
                onComment = { activeCommentReelId = reel.id },
                onBookmark = { viewModel.onBookmarkReel(reel.id) },
                onShare = { activeShareReel = reel },
                onMuteToggle = { viewModel.toggleMuteAudio() },
                onAuthorClick = {
                    onDismiss()
                    viewModel.navigateTo(com.example.ui.ScreenDestination.CreatorProfile(reel.authorId))
                },
                onAudioClick = {
                    onDismiss()
                    viewModel.navigateTo(com.example.ui.ScreenDestination.SongDetail(reel.audioTitle, reel.audioArtist, reel.videoUrl.ifBlank { reel.thumbnailUri }, reel.id))
                },
                onTogglePlayAudio = { viewModel.togglePlayPauseAudio() },
                viewModel = viewModel
            )

            // Top Bar: Close Button (left), Live Views Pill (center), and Owner Edit Button (right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }

                // Live Views Pill
                val liveReels by viewModel.reels.collectAsState()
                val liveReel = liveReels.find { it.id == reel.id } ?: reel
                Surface(
                    color = Color.Black.copy(alpha = 0.70f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Views",
                            tint = HundredGramPink,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = formatViewCountWithLabel(liveReel.viewsCount),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                val currentU by viewModel.currentUser.collectAsState()
                if (reel.authorId == currentU.userId || reel.authorUsername.equals(currentU.username, ignoreCase = true)) {
                    var showReelMenuInDialog by remember { mutableStateOf(false) }
                    var showEditSheetInDialog by remember { mutableStateOf(false) }

                    IconButton(
                        onClick = { showReelMenuInDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Edit or Delete Reel",
                            tint = Color.White
                        )
                    }

                    if (showReelMenuInDialog) {
                        ReelOptionsSheet(
                            reel = reel,
                            isOwner = true,
                            onDismiss = { showReelMenuInDialog = false },
                            onSaveToggle = { viewModel.onBookmarkReel(reel.id) },
                            onShare = {
                                ShareManager.shareReel(context, reel)
                                showReelMenuInDialog = false
                            },
                            onEdit = {
                                showReelMenuInDialog = false
                                showEditSheetInDialog = true
                            },
                            onDelete = {
                                viewModel.onDeleteReel(reel.id)
                                showReelMenuInDialog = false
                                onDismiss()
                            }
                        )
                    }

                    if (showEditSheetInDialog) {
                        EditReelBottomSheet(
                            reel = reel,
                            viewModel = viewModel,
                            onDismiss = { showEditSheetInDialog = false },
                            onSave = { newCaption, newAudioTitle, newAudioArtist, newPrivacy, newTags ->
                                viewModel.onEditReel(reel.id, newCaption, newAudioTitle, newAudioArtist, newPrivacy, newTags)
                                showEditSheetInDialog = false
                            },
                            onDelete = {
                                viewModel.onDeleteReel(reel.id)
                                showEditSheetInDialog = false
                                onDismiss()
                            }
                        )
                    }
                }
            }

            activeCommentReelId?.let { reelId ->
                val reelComments = commentsMap[reelId] ?: emptyList()
                com.example.ui.feed.CommentsBottomSheet(
                    comments = reelComments,
                    onDismiss = { activeCommentReelId = null },
                    onSendComment = { text -> viewModel.onAddComment(reelId, text) }
                )
            }

            activeShareReel?.let { r ->
                com.example.ui.reels.ReelShareBottomSheet(
                    reel = r,
                    onDismiss = { activeShareReel = null },
                    onCopyLink = {
                        ShareManager.copyToClipboard(
                            context = context,
                            text = "https://hundredgram.app/reel/${r.id}",
                            label = "Reel Link",
                            toastMessage = "Reel link copied!"
                        )
                        activeShareReel = null
                    },
                    onShareSystem = {
                        ShareManager.shareReel(context, r)
                        activeShareReel = null
                    },
                    onBookmark = {
                        viewModel.onBookmarkReel(r.id)
                        activeShareReel = null
                    },
                    onReport = {
                        viewModel.reportContent(r.id, "Reel", r.authorUsername, "Inappropriate Content")
                        activeShareReel = null
                    }
                )
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun EditReelBottomSheet(
    reel: com.example.data.ReelVideo,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSave: (newCaption: String, newAudioTitle: String, newAudioArtist: String, newPrivacy: com.example.data.PrivacyLevel, tags: List<String>) -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var captionText by remember { mutableStateOf(reel.caption) }
    var audioTitleText by remember { mutableStateOf(reel.audioTitle) }
    var audioArtistText by remember { mutableStateOf(reel.audioArtist) }
    var taggedFriends by remember { mutableStateOf(reel.tags) }
    var showTagFriendsDialog by remember { mutableStateOf(false) }
    var privacy by remember { mutableStateOf(try { com.example.data.PrivacyLevel.valueOf(reel.privacy.uppercase()) } catch (_: Exception) { com.example.data.PrivacyLevel.PUBLIC }) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    var allUsers by remember { mutableStateOf<List<com.example.data.UserData>>(emptyList()) }
    val currentUser by viewModel.currentUser.collectAsState()

    LaunchedEffect(Unit) {
        try {
            allUsers = viewModel.repository.getAllUsers()
        } catch (_: Exception) {}
    }

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = HundredGramCardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit Reel (रील एडिट करें)",
                    color = HundredGramTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = {
                        onSave(captionText.trim(), audioTitleText.trim(), audioArtistText.trim(), privacy, taggedFriends)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HundredGramPink),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Thumbnail and author preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = reel.thumbnailUri,
                    contentDescription = "Reel Thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(HundredGramCardElevated)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Reel by @${reel.authorUsername}",
                        color = HundredGramTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Update caption, audio tags or tagged friends",
                        color = HundredGramTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Caption input
            OutlinedTextField(
                value = captionText,
                onValueChange = { captionText = it },
                label = { Text("Reel Caption / Description") },
                placeholder = { Text("Add caption, tags #viral #trending...", color = HundredGramTextSecondary) },
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
                        imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowForwardIos,
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

            // Audio Title input
            OutlinedTextField(
                value = audioTitleText,
                onValueChange = { audioTitleText = it },
                label = { Text("Audio Soundtrack Title (गाने / ट्रैक का नाम)") },
                placeholder = { Text("e.g. Original Audio, Hindi Beats", color = HundredGramTextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = HundredGramTextPrimary,
                    unfocusedTextColor = HundredGramTextPrimary,
                    focusedBorderColor = HundredGramPink,
                    unfocusedBorderColor = HundredGramDivider
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Audio Artist input
            OutlinedTextField(
                value = audioArtistText,
                onValueChange = { audioArtistText = it },
                label = { Text("Audio Artist / Creator Name") },
                placeholder = { Text("e.g. Creator Name or Artist", color = HundredGramTextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = HundredGramTextPrimary,
                    unfocusedTextColor = HundredGramTextPrimary,
                    focusedBorderColor = HundredGramPink,
                    unfocusedBorderColor = HundredGramDivider
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            com.example.ui.components.PrivacySelector(
                selectedPrivacy = privacy,
                onPrivacySelected = { privacy = it },
                labelTitle = "🔒 Reel Privacy Settings (प्राइवेसी सेटिंग्स)"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Delete Reel button
            Button(
                onClick = { showDeleteConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.15f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Reel",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Delete Reel (रील हमेशा के लिए हटाएं)",
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
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

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = HundredGramCardBackground,
            title = { Text("Delete Reel?", color = HundredGramTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this reel? This action cannot be undone.", color = HundredGramTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = HundredGramTextSecondary)
                }
            }
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ReelOptionsSheet(
    reel: com.example.data.ReelVideo,
    isOwner: Boolean,
    onDismiss: () -> Unit,
    onSaveToggle: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    androidx.compose.material3.ModalBottomSheet(
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
                text = "Reel Options",
                color = HundredGramTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (isOwner) {
                // Edit Reel Option
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
                        contentDescription = "Edit Reel",
                        tint = HundredGramPink,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Edit Caption & Audio (रील एडिट करें)",
                            color = HundredGramTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Update description or soundtrack title",
                            color = HundredGramTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                // Delete Reel Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showDeleteConfirm = true }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Reel",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Delete Reel (रील डिलीट करें)",
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }

                androidx.compose.material3.HorizontalDivider(
                    color = HundredGramDivider,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }

            // Save Reel
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
                    imageVector = if (reel.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Save Reel",
                    tint = HundredGramTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = if (reel.isSaved) "Remove from Saved" else "Save Reel",
                    color = HundredGramTextPrimary,
                    fontSize = 15.sp
                )
            }

            // Share Reel
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
                    text = "Share Reel Link",
                    color = HundredGramTextPrimary,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = HundredGramCardBackground,
            title = { Text("Delete Reel?", color = HundredGramTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this reel?", color = HundredGramTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = HundredGramTextSecondary)
                }
            }
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

@Composable
fun FullScreenAvatarDialog(
    avatarUrl: String,
    username: String,
    displayName: String,
    onDismiss: () -> Unit,
    onUpdateAvatarClick: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Center High-Resolution Profile Picture
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 90.dp, bottom = 100.dp),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl.isNotBlank()) {
                    val safeAvatar = remember(avatarUrl) {
                        com.example.util.ImageUtils.getSafeImageModel(avatarUrl)
                    }
                    SubcomposeAsyncImage(
                        model = coil.request.ImageRequest.Builder(LocalContext.current)
                            .data(safeAvatar)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Full Screen Profile Picture",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clip(RoundedCornerShape(20.dp)),
                        loading = {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = HundredGramPink)
                            }
                        }
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .clip(CircleShape)
                                .background(HundredGramCardElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = HundredGramTextSecondary,
                                modifier = Modifier.size(80.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No profile photo set",
                            color = HundredGramTextSecondary,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onUpdateAvatarClick,
                            colors = ButtonDefaults.buttonColors(containerColor = HundredGramPink),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload Photo", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Top Bar: Back button (Left), Username (Center), and Prominent "Update Profile Pic" Button (Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.95f), Color.Transparent)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Text(
                    text = if (username.isNotBlank()) "@$username" else "Profile Photo",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(horizontal = 10.dp)
                )

                // Prominent "Update Profile Pic" Button at the top
                Button(
                    onClick = onUpdateAvatarClick,
                    colors = ButtonDefaults.buttonColors(containerColor = HundredGramPink),
                    shape = RoundedCornerShape(22.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.heightIn(min = 40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Update Profile Pic",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }
            }

            // Bottom user info label & secondary change photo action
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f))
                        )
                    )
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (displayName.isNotBlank()) {
                        Text(
                            text = displayName,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Button(
                        onClick = onUpdateAvatarClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.18f)),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Change Photo (फ़ोटो बदलें)",
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SoundSettingsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        com.example.util.SoundSettingsManager.init(context)
    }

    val isMasterSound by com.example.util.SoundSettingsManager.isMasterSoundEnabled.collectAsState()
    val isNotificationSound by com.example.util.SoundSettingsManager.isNotificationSoundEnabled.collectAsState()
    val isCallRingtone by com.example.util.SoundSettingsManager.isCallRingtoneEnabled.collectAsState()
    val isVibration by com.example.util.SoundSettingsManager.isVibrationEnabled.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HundredGramCardBackground,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("🔊", fontSize = 22.sp)
                Text(
                    text = "Sound & Vibration (साउंड सेटिंग्स)",
                    color = HundredGramTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Manage audio alerts, call ringing and vibration feedback (साउंड और वाइब्रेशन नियंत्रित करें).",
                    color = HundredGramTextSecondary,
                    fontSize = 12.5.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                // 1. Master Sound Toggle
                SoundSettingToggleItem(
                    icon = if (isMasterSound) "🔊" else "🔇",
                    title = "All Sounds (समस्त साउंड)",
                    subtitle = "Master switch for all app audio effects and rings",
                    checked = isMasterSound,
                    onCheckedChange = { com.example.util.SoundSettingsManager.setMasterSound(context, it) }
                )

                // 2. Notification Sound Toggle
                SoundSettingToggleItem(
                    icon = "🔔",
                    title = "Notification Alerts (नोटिफिकेशन साउंड)",
                    subtitle = "Sound when likes, comments, shares, follows or messages arrive",
                    checked = isNotificationSound && isMasterSound,
                    enabled = isMasterSound,
                    onCheckedChange = { com.example.util.SoundSettingsManager.setNotificationSound(context, it) }
                )

                // 3. Call & Video Call Ringtone Toggle
                SoundSettingToggleItem(
                    icon = "📞",
                    title = "Call Ringtone (कॉल रिंगटोन)",
                    subtitle = "Ringing sound for voice and video calls",
                    checked = isCallRingtone && isMasterSound,
                    enabled = isMasterSound,
                    onCheckedChange = { com.example.util.SoundSettingsManager.setCallRingtone(context, it) }
                )

                // 4. Vibration Toggle
                SoundSettingToggleItem(
                    icon = "📳",
                    title = "Vibration (वाइब्रेशन / कंपन)",
                    subtitle = "Vibrate device for incoming calls and notifications",
                    checked = isVibration,
                    onCheckedChange = { com.example.util.SoundSettingsManager.setVibration(context, it) }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Test Alert Sound Button
                Button(
                    onClick = {
                        com.example.util.NotificationHelper.playAlertSound(context)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMasterSound) HundredGramPink else Color.DarkGray
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isMasterSound) "Test Alert Sound (साउंड टेस्ट करें 🔔)" else "Sound is Disabled (साउंड बंद है)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = HundredGramPink),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Done (सहेजा गया)", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun SoundSettingToggleItem(
    icon: String,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(HundredGramCardElevated)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(text = icon, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        color = if (enabled) HundredGramTextPrimary else HundredGramTextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = HundredGramTextSecondary,
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            androidx.compose.material3.Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = androidx.compose.material3.SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = HundredGramPink,
                    uncheckedThumbColor = Color.LightGray,
                    uncheckedTrackColor = Color(0xFF2A3144)
                )
            )
        }
    }
}


