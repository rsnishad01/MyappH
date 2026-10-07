package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserData
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowersListBottomSheet(
    targetUserId: String,
    targetUsername: String,
    initialTab: Int = 0, // 0 = Followers, 1 = Following
    isPrivateAccount: Boolean = false,
    isFollowingTarget: Boolean = false,
    onDismiss: () -> Unit,
    onUserClick: (String) -> Unit,
    viewModel: MainViewModel
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentUser by viewModel.currentUser.collectAsState()
    val followedUserIds by viewModel.followedUserIds.collectAsState()

    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var searchQuery by remember { mutableStateOf("") }
    var allUsers by remember { mutableStateOf<List<UserData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val isSelf = targetUserId == currentUser.userId

    LaunchedEffect(targetUserId, selectedTab, followedUserIds) {
        isLoading = true
        val users = viewModel.getFollowersOrFollowing(targetUserId, isFollowingTab = (selectedTab == 1))
        allUsers = users
        isLoading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = HundredGramCardBackground,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(HundredGramTextSecondary.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "@$targetUsername",
                    color = HundredGramTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = HundredGramTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Check Private Account restriction
            if (isPrivateAccount && !isSelf && !isFollowingTarget) {
                // Account is Private and not followed
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(HundredGramPink.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Private Account",
                                tint = HundredGramPink,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "This Account is Private (प्राइवेट अकाउंट)",
                            color = HundredGramTextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Follow @$targetUsername to see their followers and following.",
                            color = HundredGramTextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                viewModel.toggleFollowUser(targetUserId)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HundredGramPink),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Follow", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Public / Followed / Own Profile -> Show Followers and Following List
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = HundredGramPink,
                    divider = { HorizontalDivider(color = HundredGramDivider.copy(alpha = 0.4f)) }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = "Followers",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 0) HundredGramPink else HundredGramTextSecondary,
                                fontSize = 15.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = "Following",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 1) HundredGramPink else HundredGramTextSecondary,
                                fontSize = 15.sp
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            if (selectedTab == 0) "Search followers..." else "Search following...",
                            color = HundredGramTextSecondary,
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = HundredGramTextSecondary, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = HundredGramTextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HundredGramPink,
                        unfocusedBorderColor = HundredGramDivider,
                        focusedContainerColor = HundredGramDarkBackground,
                        unfocusedContainerColor = HundredGramDarkBackground,
                        focusedTextColor = HundredGramTextPrimary,
                        unfocusedTextColor = HundredGramTextPrimary
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = HundredGramPink, modifier = Modifier.size(32.dp))
                    }
                } else {
                    // Filter user list
                    val filteredUsers = remember(allUsers, searchQuery, selectedTab, followedUserIds, currentUser) {
                        allUsers.filter { u ->
                            // Exclude target user itself from their own followers/following list
                            if (u.userId == targetUserId) return@filter false

                            // Requirement: "jinka account privet. Hai unko chhod kar sabhi"
                            // If user is private AND not current user AND current user is not following them -> filter out
                            if (u.isPrivate && u.userId != currentUser.userId && !followedUserIds.contains(u.userId)) {
                                return@filter false
                            }

                            // Search filter
                            if (searchQuery.isNotBlank()) {
                                val q = searchQuery.lowercase()
                                u.username.lowercase().contains(q) || u.displayName.lowercase().contains(q)
                            } else {
                                true
                            }
                        }
                    }

                    if (filteredUsers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank()) "No matching users found" else "No ${if (selectedTab == 0) "followers" else "following"} to display",
                                color = HundredGramTextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(filteredUsers, key = { it.userId }) { user ->
                                val isFollowingUser = followedUserIds.contains(user.userId)
                                val isSelfItem = user.userId == currentUser.userId

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            onDismiss()
                                            onUserClick(user.userId)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    UserAvatar(
                                        avatarUrl = user.avatarUrl,
                                        size = 46.dp
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = user.displayName.ifBlank { user.username },
                                                color = HundredGramTextPrimary,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (user.isVerified) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Verified,
                                                    contentDescription = "Verified",
                                                    tint = HundredGramBlue,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "@${user.username}",
                                            color = HundredGramTextSecondary,
                                            fontSize = 12.5.sp
                                        )
                                    }

                                    if (!isSelfItem) {
                                        Button(
                                            onClick = {
                                                viewModel.toggleFollowUser(user.userId)
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isFollowingUser) Color(0xFF272D3B) else HundredGramPink
                                            ),
                                            shape = RoundedCornerShape(18.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            if (isFollowingUser) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Following",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Following", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            } else {
                                                Text("Follow", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(HundredGramDarkBackground)
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("You", color = HundredGramTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
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
