package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LikesListBottomSheet(
    postAuthorId: String,
    likesCount: Int,
    isLikedByMe: Boolean,
    onDismiss: () -> Unit,
    viewModel: com.example.ui.MainViewModel
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentUser by viewModel.currentUser.collectAsState()
    
    var allUsers by remember { mutableStateOf<List<com.example.data.UserData>>(emptyList()) }
    var mutualFollowersMap by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        val users = viewModel.repository.getAllUsers()
        allUsers = users
        
        val map = mutableMapOf<String, Boolean>()
        users.forEach { u ->
            if (u.userId != currentUser.userId) {
                val curUserFollowsLiker = viewModel.repository.isFollowingUser(u.userId)
                val likerFollowsCurUser = viewModel.repository.isUserFollowing(currentUser.userId, u.userId)
                map[u.userId] = curUserFollowsLiker && likerFollowsCurUser
            }
        }
        mutualFollowersMap = map
        isLoading = false
    }

    val filteredLikers = remember(allUsers, mutualFollowersMap, isLoading) {
        if (isLoading) return@remember emptyList()
        
        val rawLikers = mutableListOf<com.example.data.UserData>()
        
        if (isLikedByMe) {
            rawLikers.add(currentUser)
        }
        
        val otherUsers = allUsers.filter { it.userId != currentUser.userId }
        val itemsToTake = (likesCount - (if (isLikedByMe) 1 else 0)).coerceIn(0, otherUsers.size)
        rawLikers.addAll(otherUsers.take(itemsToTake))

        val isCreator = postAuthorId == currentUser.userId
        
        if (isCreator) {
            rawLikers
        } else {
            rawLikers.filter { u ->
                u.userId == currentUser.userId || (mutualFollowersMap[u.userId] == true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = HundredGramCardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Liked By (लाइक करने वाले) 🦁",
                color = HundredGramTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            HorizontalDivider(color = HundredGramDivider.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 12.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = HundredGramPink)
                }
            } else if (filteredLikers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No mutually followed users in the likes list.\n(केवल उन्हीं की लिस्ट दिखेगी जिन्होंने एक दूसरे को फ़ॉलो किया है)",
                        color = HundredGramTextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredLikers) { u ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(avatarUrl = u.avatarUrl, size = 42.dp, isVerified = u.isVerified)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = u.displayName.ifBlank { u.username },
                                    color = HundredGramTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "@${u.username}",
                                    color = HundredGramTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            if (u.userId == currentUser.userId) {
                                Text(
                                    text = "You",
                                    color = HundredGramPink,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            } else {
                                val isMutual = mutualFollowersMap[u.userId] == true
                                if (isMutual) {
                                    Text(
                                        text = "Mutual Follow 🟢",
                                        color = Color(0xFF22C55E),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        modifier = Modifier
                                            .border(1.dp, Color(0xFF22C55E).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
