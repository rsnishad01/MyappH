package com.example.ui.drafts

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.DraftEntity
import com.example.data.DraftType
import com.example.ui.MainViewModel
import com.example.ui.ScreenDestination
import com.example.ui.theme.HundredGramBorderSubtle
import com.example.ui.theme.HundredGramCardBackground
import com.example.ui.theme.HundredGramCardElevated
import com.example.ui.theme.HundredGramDarkBackground
import com.example.ui.theme.HundredGramDivider
import com.example.ui.theme.HundredGramPink
import com.example.ui.theme.HundredGramPurple
import com.example.ui.theme.HundredGramTextPrimary
import com.example.ui.theme.HundredGramTextSecondary

@Composable
fun DraftsScreen(viewModel: MainViewModel) {
    val allDrafts by viewModel.allDrafts.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: All, 1: Posts, 2: Reels
    var draftToDelete by remember { mutableStateOf<DraftEntity?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    val postDrafts = remember(allDrafts) { allDrafts.filter { it.type == DraftType.POST.name } }
    val reelDrafts = remember(allDrafts) { allDrafts.filter { it.type == DraftType.REEL.name } }

    val displayedDrafts = when (selectedTab) {
        1 -> postDrafts
        2 -> reelDrafts
        else -> allDrafts
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HundredGramDarkBackground)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(ScreenDestination.Feed) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = HundredGramTextPrimary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Saved Drafts (ड्राफ्ट्स)",
                    color = HundredGramTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${allDrafts.size} total drafts saved offline",
                    color = HundredGramTextSecondary,
                    fontSize = 12.sp
                )
            }

            if (allDrafts.isNotEmpty()) {
                IconButton(onClick = { showClearAllDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Clear all drafts",
                        tint = Color(0xFFEF4444)
                    )
                }
            }
        }

        // Filter Tabs
        val tabs = listOf(
            "All (${allDrafts.size})",
            "Posts (${postDrafts.size})",
            "Reels (${reelDrafts.size})"
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = HundredGramDarkBackground,
            contentColor = HundredGramPink,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = HundredGramPink,
                        height = 3.dp
                    )
                }
            },
            divider = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(HundredGramDivider)
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) HundredGramPink else HundredGramTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        if (displayedDrafts.isEmpty()) {
            // Empty State
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
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(HundredGramPink.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Drafts,
                            contentDescription = null,
                            tint = HundredGramPink,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = when (selectedTab) {
                            1 -> "No Post Drafts"
                            2 -> "No Reel Drafts"
                            else -> "No Saved Drafts"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = HundredGramTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "When creating a post or reel, click 'Save Draft' to store your edits locally using Room until you're ready to share.",
                        fontSize = 13.sp,
                        color = HundredGramTextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { viewModel.navigateTo(ScreenDestination.Camera) },
                            colors = ButtonDefaults.buttonColors(containerColor = HundredGramPink),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Post")
                        }
                        OutlinedButton(
                            onClick = { viewModel.navigateTo(ScreenDestination.CreateReel) },
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, HundredGramPink)
                        ) {
                            Icon(Icons.Default.Movie, contentDescription = null, tint = HundredGramPink, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Reel", color = HundredGramPink)
                        }
                    }
                }
            }
        } else {
            // Draft Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(displayedDrafts, key = { it.id }) { draft ->
                    DraftCard(
                        draft = draft,
                        onOpenDraft = { viewModel.openDraft(draft) },
                        onDeleteDraft = { draftToDelete = draft }
                    )
                }
            }
        }
    }

    // Delete Single Draft Dialog
    if (draftToDelete != null) {
        AlertDialog(
            onDismissRequest = { draftToDelete = null },
            title = { Text("Delete Draft?", color = HundredGramTextPrimary) },
            text = {
                Text(
                    "Are you sure you want to delete this ${if (draftToDelete?.type == DraftType.POST.name) "post" else "reel"} draft permanently?",
                    color = HundredGramTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        draftToDelete?.let { viewModel.deleteDraft(it.id) }
                        draftToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { draftToDelete = null }) {
                    Text("Cancel", color = HundredGramTextSecondary)
                }
            },
            containerColor = HundredGramCardBackground
        )
    }

    // Clear All Dialog
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Clear All Drafts?", color = HundredGramTextPrimary) },
            text = {
                Text(
                    "This will delete all saved post and reel drafts from this device.",
                    color = HundredGramTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllDrafts()
                        showClearAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Clear All", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel", color = HundredGramTextSecondary)
                }
            },
            containerColor = HundredGramCardBackground
        )
    }
}

@Composable
fun DraftCard(
    draft: DraftEntity,
    onOpenDraft: () -> Unit,
    onDeleteDraft: () -> Unit
) {
    val isPost = draft.type == DraftType.POST.name
    val timeAgo = try {
        DateUtils.getRelativeTimeSpanString(
            draft.updatedAt,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    } catch (_: Exception) {
        "Recently"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDraft() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = HundredGramCardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, HundredGramBorderSubtle)
    ) {
        Column {
            // Media Preview Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(HundredGramCardElevated)
            ) {
                AsyncImage(
                    model = draft.thumbnailUri.ifBlank { draft.mediaUri },
                    contentDescription = "Draft thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Type Badge (POST / REEL)
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isPost) HundredGramPink.copy(alpha = 0.9f) else HundredGramPurple.copy(alpha = 0.9f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isPost) "📸 POST" else "🎬 REEL",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Delete Icon Button Overlay
                IconButton(
                    onClick = onDeleteDraft,
                    modifier = Modifier
                        .padding(6.dp)
                        .size(28.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Draft",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Info Column
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                // Caption
                Text(
                    text = if (draft.caption.isNotBlank()) draft.caption else "(No caption)",
                    color = if (draft.caption.isNotBlank()) HundredGramTextPrimary else HundredGramTextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Location or Audio Track
                if (isPost && draft.location.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = HundredGramPink,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = draft.location,
                            color = HundredGramTextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else if (!isPost && draft.audioTitle.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = HundredGramPurple,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = draft.audioTitle,
                            color = HundredGramTextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text(
                    text = timeAgo,
                    color = HundredGramTextSecondary.copy(alpha = 0.7f),
                    fontSize = 10.5.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Edit/Continue Button
                Button(
                    onClick = onOpenDraft,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HundredGramCardElevated),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = HundredGramPink,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Edit & Post",
                        color = HundredGramTextPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
