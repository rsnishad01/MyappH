package com.example.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import coil.compose.SubcomposeAsyncImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.ui.MainViewModel
import com.example.ui.ScreenDestination
import com.example.ui.components.UserAvatar
import com.example.ui.theme.HundredGramCardElevated
import com.example.ui.theme.HundredGramDarkBackground
import com.example.ui.theme.HundredGramDivider
import com.example.ui.theme.HundredGramLikeRed
import com.example.ui.theme.HundredGramPink
import com.example.ui.theme.HundredGramTextPrimary
import com.example.ui.theme.HundredGramTextSecondary
import com.example.util.ShareManager

@Composable
fun ExploreScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryIndex by remember { mutableStateOf(0) }
    val categories = listOf("Trending", "Search Creator 🔍", "Nearby Radar 📡", "Photography", "Travel", "Architecture", "Food")

    val posts by viewModel.posts.collectAsState()
    val nearbyUsers by viewModel.nearbyUsers.collectAsState()
    val nearbyEvents by viewModel.nearbyEvents.collectAsState()

    val searchedUsers by viewModel.searchedUsers.collectAsState()
    val isSearchingUsers by viewModel.isSearchingUsers.collectAsState()
    val recommendedCreators by viewModel.recommendedCreators.collectAsState()
    var selectedUserForDetail by remember { mutableStateOf<com.example.data.UserData?>(null) }
    var selectedPostForPreview by remember { mutableStateOf<com.example.data.PostEntity?>(null) }

    androidx.compose.runtime.LaunchedEffect(searchQuery, selectedCategoryIndex) {
        if (selectedCategoryIndex == 1) {
            viewModel.searchUsers(searchQuery)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HundredGramDarkBackground)
    ) {
        // Search Input Bar (Smart placeholder based on selected category)
        val placeholderText = if (selectedCategoryIndex == 1) {
            "Search creators by username or display name..."
        } else {
            "Search photos, creators, tags..."
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(placeholderText, color = HundredGramTextSecondary, fontSize = 13.5.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = HundredGramTextSecondary) },
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = HundredGramTextPrimary,
                unfocusedTextColor = HundredGramTextPrimary,
                focusedBorderColor = HundredGramPink,
                unfocusedBorderColor = HundredGramDivider
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )

        // Categories Filter Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories.size) { index ->
                val isSelected = selectedCategoryIndex == index
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategoryIndex = index },
                    label = { Text(categories[index], fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = HundredGramPink,
                        selectedLabelColor = Color.White,
                        containerColor = HundredGramCardElevated,
                        labelColor = HundredGramTextSecondary
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Content Display
        if (selectedCategoryIndex == 1) {
            // Live Creator Search Pane
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                if (searchQuery.trim().isBlank()) {
                    // Show Recommended Creators
                    item {
                        Text(
                            text = "✨ Suggested Creators",
                            color = HundredGramTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    if (recommendedCreators.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Loading recommended creators...",
                                    color = HundredGramTextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        items(recommendedCreators) { user ->
                            SearchCreatorCard(user = user) {
                                selectedUserForDetail = user
                            }
                        }
                    }
                } else {
                    // Show search query results
                    item {
                        Text(
                            text = "🔍 Matching Creators",
                            color = HundredGramTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    if (isSearchingUsers) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = HundredGramPink)
                            }
                        }
                    } else if (searchedUsers.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No creators found matching \"$searchQuery\"",
                                    color = HundredGramTextSecondary,
                                    fontSize = 13.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(searchedUsers) { user ->
                            SearchCreatorCard(user = user) {
                                selectedUserForDetail = user
                            }
                        }
                    }
                }
            }
        } else if (selectedCategoryIndex == 2) {
            // Nearby Radar Pane (moved from index 1)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    Text(
                        text = "People in Your Proximity",
                        color = HundredGramTextPrimary,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                if (nearbyUsers.isEmpty()) {
                    item {
                        Text(
                            text = "No creators currently in your immediate radius.",
                            color = HundredGramTextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                } else {
                    items(nearbyUsers) { user ->
                        NearbyUserCard(
                            user = user,
                            onUserClick = { /* View profile */ },
                            onFollowClick = { /* Wave */ }
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Upcoming Local Events",
                        color = HundredGramTextPrimary,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                if (nearbyEvents.isEmpty()) {
                    item {
                        Text(
                            text = "No local events scheduled nearby.",
                            color = HundredGramTextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                } else {
                    items(nearbyEvents) { event ->
                        NearbyEventCard(event = event)
                    }
                }
            }
        } else {
            // Explore Media Grid
            if (posts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = HundredGramTextSecondary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Explore Media Yet",
                            color = HundredGramTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Authentic photos and stories shared by users will appear here.",
                            color = HundredGramTextSecondary,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 1.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    items(posts) { post ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .background(HundredGramCardElevated)
                                .clickable { selectedPostForPreview = post }
                        ) {
                            val safeUrl = remember(post.imageUrl) {
                                com.example.util.ImageUtils.getSafeImageModel(post.imageUrl)
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
                                }
                            )
                        }
                    }
                }
            }
        }

        // Post Preview Dialog with Share & Like
        if (selectedPostForPreview != null) {
            val previewPost = selectedPostForPreview!!
            Dialog(
                onDismissRequest = { selectedPostForPreview = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(HundredGramDarkBackground)
                        .border(1.dp, HundredGramPink.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Header: Author & Close
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    selectedPostForPreview = null
                                    viewModel.navigateTo(ScreenDestination.CreatorProfile(previewPost.authorId))
                                }
                            ) {
                                UserAvatar(avatarUrl = previewPost.authorAvatarUrl, size = 36.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = previewPost.authorUsername,
                                        color = HundredGramTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    if (previewPost.location.isNotBlank()) {
                                        Text(
                                            text = previewPost.location,
                                            color = HundredGramTextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = { selectedPostForPreview = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Media Image
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(HundredGramCardElevated)
                        ) {
                            val safePreviewUrl = remember(previewPost.imageUrl) {
                                com.example.util.ImageUtils.getSafeImageModel(previewPost.imageUrl)
                            }
                            SubcomposeAsyncImage(
                                model = coil.request.ImageRequest.Builder(LocalContext.current)
                                    .data(safePreviewUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = previewPost.caption,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
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

                        Spacer(modifier = Modifier.height(10.dp))

                        // Actions: Like & Share
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewModel.onLikePost(previewPost.id) }) {
                                    Icon(
                                        imageVector = if (previewPost.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Like",
                                        tint = if (previewPost.isLiked) HundredGramLikeRed else Color.White
                                    )
                                }
                                Text(
                                    text = "${previewPost.likesCount} likes",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            androidx.compose.material3.Button(
                                onClick = {
                                    ShareManager.sharePost(context, previewPost)
                                },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = HundredGramPink
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share Post (शेयर करें)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (previewPost.caption.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = previewPost.caption,
                                color = HundredGramTextPrimary,
                                fontSize = 13.sp,
                                maxLines = 3
                            )
                        }
                    }
                }
            }
        }

        // Beautiful Interactive Creator Detail Popup/Dialog
        if (selectedUserForDetail != null) {
            val detailUser = selectedUserForDetail!!
            Dialog(
                onDismissRequest = { selectedUserForDetail = null }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(HundredGramCardElevated)
                        .border(1.5.dp, HundredGramPink, RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header (Close Button)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .clickable { selectedUserForDetail = null }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✕", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Avatar
                        UserAvatar(avatarUrl = detailUser.avatarUrl, size = 80.dp)

                        Spacer(modifier = Modifier.height(12.dp))

                        // Name + Verified Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = detailUser.displayName,
                                color = HundredGramTextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (detailUser.isVerified) {
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("✓", color = HundredGramPink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(
                            text = "@${detailUser.username}",
                            color = HundredGramTextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        // Bio
                        if (detailUser.bio.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = detailUser.bio,
                                color = HundredGramTextPrimary,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }

                        // Stats Grid (Posts, Followers, Following)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(HundredGramDarkBackground)
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "${detailUser.postsCount}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(text = "Posts", color = HundredGramTextSecondary, fontSize = 10.5.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "${detailUser.followersCount}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(text = "Followers", color = HundredGramTextSecondary, fontSize = 10.5.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "${detailUser.followingCount}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(text = "Following", color = HundredGramTextSecondary, fontSize = 10.5.sp)
                            }
                        }

                        // Website & Location Info
                        if (detailUser.location.isNotEmpty() || detailUser.website.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (detailUser.location.isNotEmpty()) {
                                    Text(
                                        text = "📍 ${detailUser.location}",
                                        color = HundredGramTextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                                if (detailUser.website.isNotEmpty()) {
                                    Text(
                                        text = "🔗 ${detailUser.website}",
                                        color = HundredGramPink,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Action Buttons: View Profile, Message, Share Profile
                        Spacer(modifier = Modifier.height(18.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            androidx.compose.material3.Button(
                                onClick = {
                                    selectedUserForDetail = null
                                    viewModel.navigateTo(ScreenDestination.CreatorProfile(detailUser.userId))
                                },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = HundredGramPink
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Profile", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            androidx.compose.material3.Button(
                                onClick = {
                                    ShareManager.shareProfile(context, detailUser)
                                },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = HundredGramCardElevated
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, HundredGramPink),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Share ↗", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchCreatorCard(
    user: com.example.data.UserData,
    onUserClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(HundredGramCardElevated)
            .border(1.dp, HundredGramDivider.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .clickable { onUserClick() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(avatarUrl = user.avatarUrl, size = 48.dp)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.displayName,
                        color = HundredGramTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (user.isVerified) {
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("✓", color = HundredGramPink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    text = "@${user.username}",
                    color = HundredGramTextSecondary,
                    fontSize = 12.sp
                )
                if (user.bio.isNotEmpty()) {
                    Text(
                        text = user.bio,
                        color = HundredGramTextSecondary,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Details",
                tint = HundredGramPink,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

