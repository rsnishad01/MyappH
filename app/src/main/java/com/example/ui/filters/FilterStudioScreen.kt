package com.example.ui.filters

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.PrivacyLevel
import com.example.data.UserData
import androidx.compose.material3.Surface
import com.example.ui.MainViewModel
import com.example.ui.ScreenDestination
import com.example.ui.theme.HundredGramCardElevated
import com.example.ui.theme.HundredGramDarkBackground
import com.example.ui.theme.HundredGramDivider
import com.example.ui.theme.HundredGramPink
import com.example.ui.theme.HundredGramTextPrimary
import com.example.ui.theme.HundredGramTextSecondary
import kotlinx.coroutines.launch

@Composable
fun FilterStudioScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val selectedImageUri by viewModel.selectedFilterImageUri.collectAsState()
    val draftsCount by viewModel.draftsCount.collectAsState()
    val editingDraftId by viewModel.currentEditingDraftId.collectAsState()
    val initialCaption by viewModel.draftInitialCaption.collectAsState()
    val initialLocation by viewModel.draftInitialLocation.collectAsState()
    val initialFilterId by viewModel.draftInitialFilterId.collectAsState()

    val filters = remember { PhotoFilter.getAllFilters() }
    var selectedFilter by remember {
        mutableStateOf(filters.find { it.id == initialFilterId } ?: filters.first())
    }

    // Beauty Settings State Sliders
    var skinSmoothness by remember { mutableStateOf(65f) }
    var skinWhitening by remember { mutableStateOf(45f) }
    var lipTint by remember { mutableStateOf(50f) }
    var eyeBright by remember { mutableStateOf(40f) }

    // Double-tap state for Beauty Camera Controls inside Filter
    var showBeautyControlsPanel by remember { mutableStateOf(false) }
    var lastFilterTapTimestamp by remember { mutableStateOf(0L) }
    var lastTappedFilterId by remember { mutableStateOf("") }

    var captionInput by remember { mutableStateOf(initialCaption) }
    var locationInput by remember { mutableStateOf(initialLocation) }
    var taggedFriends by remember { mutableStateOf<List<String>>(emptyList()) }
    var showTagFriendsDialog by remember { mutableStateOf(false) }
    var isLocating by remember { mutableStateOf(false) }
    var showSaveDraftDialog by remember { mutableStateOf(false) }
    var privacy by remember { mutableStateOf(PrivacyLevel.PUBLIC) }

    var allUsers by remember { mutableStateOf<List<UserData>>(emptyList()) }
    val currentUser by viewModel.currentUser.collectAsState()

    LaunchedEffect(Unit) {
        try {
            allUsers = viewModel.repository.getAllUsers()
        } catch (_: Exception) {}
    }

    LaunchedEffect(initialCaption) {
        if (initialCaption.isNotBlank() && captionInput.isBlank()) {
            captionInput = initialCaption
        }
    }
    LaunchedEffect(initialLocation) {
        if (initialLocation.isNotBlank() && locationInput.isBlank()) {
            locationInput = initialLocation
        }
    }

    val imageModel = selectedImageUri

    BackHandler(enabled = true) {
        if (imageModel != null) {
            showSaveDraftDialog = true
        } else {
            viewModel.navigateTo(ScreenDestination.Feed)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.setSelectedFilterImage(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HundredGramDarkBackground)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (imageModel != null) {
                    showSaveDraftDialog = true
                } else {
                    viewModel.navigateTo(ScreenDestination.Camera)
                }
            }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = HundredGramTextPrimary)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (editingDraftId != null) "Edit Draft Post" else "Beauty Filter Studio",
                    color = HundredGramTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                if (draftsCount > 0) {
                    Text(
                        text = "$draftsCount saved drafts",
                        color = HundredGramPink,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { viewModel.navigateTo(ScreenDestination.Drafts) }
                    )
                }
            }

            if (imageModel != null) {
                OutlinedButton(
                    onClick = {
                        viewModel.savePostDraft(
                            mediaUri = imageModel,
                            caption = captionInput,
                            location = locationInput,
                            filterId = selectedFilter.id,
                            onSaved = {
                                Toast.makeText(context, "Post Draft Saved to Room!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, HundredGramPink.copy(alpha = 0.7f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save Draft",
                        tint = HundredGramPink,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save Draft", color = HundredGramPink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Button(
                onClick = {
                    if (imageModel != null) {
                        viewModel.createPost(
                            type = "image",
                            caption = captionInput,
                            location = locationInput,
                            uri = imageModel,
                            privacy = privacy,
                            tags = taggedFriends
                        )
                    }
                },
                enabled = imageModel != null,
                colors = ButtonDefaults.buttonColors(containerColor = HundredGramPink),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text("Share", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // Filtered Image Preview Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(HundredGramCardElevated)
                .clickable {
                    photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            contentAlignment = Alignment.Center
        ) {
            if (imageModel != null) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = "Filtered Preview",
                    colorFilter = ColorFilter.colorMatrix(selectedFilter.getInterpolatedMatrix(skinWhitening)),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Change Photo Overlay Button
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Change photo",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Change",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(HundredGramPink.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Select Photo",
                            tint = HundredGramPink,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Select Photo for Beauty Filter",
                        color = HundredGramTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Choose any photo to apply Bilateral Blur, Whitening, Lip Tint & LUT Filters",
                        color = HundredGramTextSecondary,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        colors = ButtonDefaults.buttonColors(containerColor = HundredGramPink),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose from Gallery", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Preset Filters Carousel (Beauty + 8 LUT Filters)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Preset Filters (2 बार टैप करके कंट्रोल्स खोलें)",
                color = HundredGramTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            if (showBeautyControlsPanel) {
                Text(
                    text = "Controls Open ⚙️",
                    color = HundredGramPink,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { showBeautyControlsPanel = false }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filters) { filter ->
                val isSelected = selectedFilter.id == filter.id
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        val now = System.currentTimeMillis()
                        if (lastTappedFilterId == filter.id && (now - lastFilterTapTimestamp) < 450L) {
                            // Double-tap detected on filter! Open/Toggle Beauty Camera Controls
                            selectedFilter = filter
                            showBeautyControlsPanel = !showBeautyControlsPanel
                        } else {
                            // Single tap: Select filter
                            selectedFilter = filter
                        }
                        lastFilterTapTimestamp = now
                        lastTappedFilterId = filter.id
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) HundredGramPink else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        val filterGradient = when (filter.type) {
                            FilterPresetType.BEAUTY -> Brush.linearGradient(listOf(Color(0xFFFF2A6D), Color(0xFFFF758C)))
                            FilterPresetType.NATURAL_GLAM -> Brush.linearGradient(listOf(Color(0xFFFFB6C1), Color(0xFFFFA07A)))
                            FilterPresetType.VINTAGE_FILM -> Brush.linearGradient(listOf(Color(0xFF8D6E63), Color(0xFF4E342E)))
                            FilterPresetType.CYBERPUNK_NEON -> Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFFFF007F)))
                            FilterPresetType.SUNSET_GLOW -> Brush.linearGradient(listOf(Color(0xFFFF8C00), Color(0xFFFFD700)))
                            FilterPresetType.TOKYO_CHIC -> Brush.linearGradient(listOf(Color(0xFF87CEEB), Color(0xFF4682B4)))
                            FilterPresetType.CINEMA_DARK -> Brush.linearGradient(listOf(Color(0xFF333333), Color(0xFF111111)))
                            FilterPresetType.MONOCHROME_SOFT -> Brush.linearGradient(listOf(Color(0xFFE0E0E0), Color(0xFF424242)))
                            FilterPresetType.ROSY_ROMANCE -> Brush.linearGradient(listOf(Color(0xFFE91E63), Color(0xFFF48FB1)))
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(filterGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (filter.type == FilterPresetType.BEAUTY) Icons.Default.Face else Icons.Default.AutoAwesome,
                                contentDescription = filter.name,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = filter.name,
                        color = if (isSelected) HundredGramPink else HundredGramTextSecondary,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Beauty Filter Sliders Panel (Opened ONLY when double-tapping on filter)
        AnimatedVisibility(
            visible = showBeautyControlsPanel,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(HundredGramCardElevated)
                    .border(1.dp, HundredGramPink.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Face, contentDescription = null, tint = HundredGramPink, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Beauty Camera Controls (${selectedFilter.name})",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = { showBeautyControlsPanel = false },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = HundredGramTextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 1. Skin Smoothness (Bilateral Blur)
                BeautySliderRow(
                    label = "Skin Smoothness (Bilateral Blur)",
                    value = skinSmoothness,
                    onValueChange = { skinSmoothness = it }
                )

                // 2. Skin Whitening (Luminous Brightness)
                BeautySliderRow(
                    label = "Skin Whitening & Glow",
                    value = skinWhitening,
                    onValueChange = { skinWhitening = it }
                )

                // 3. Lip Tint (Pink Overlay)
                BeautySliderRow(
                    label = "Lip Tint Overlay",
                    value = lipTint,
                    onValueChange = { lipTint = it }
                )

                // 4. Eye Brightening
                BeautySliderRow(
                    label = "Eye Brightening & Contrast",
                    value = eyeBright,
                    onValueChange = { eyeBright = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Caption & Location Inputs
        Column(modifier = Modifier.padding(horizontal = 14.dp)) {
            OutlinedTextField(
                value = captionInput,
                onValueChange = { captionInput = it },
                label = { Text("Write a caption...") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = HundredGramTextPrimary,
                    unfocusedTextColor = HundredGramTextPrimary,
                    focusedBorderColor = HundredGramPink,
                    unfocusedBorderColor = HundredGramDivider
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = locationInput,
                onValueChange = { locationInput = it },
                label = { Text("Add Location") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = HundredGramPink
                    )
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
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
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
                            fontSize = 13.5.sp
                        )
                        Text(
                            text = if (taggedFriends.isEmpty()) "Tap to tag friends on this post" else "${taggedFriends.size} friends tagged",
                            color = if (taggedFriends.isEmpty()) HundredGramTextSecondary else HundredGramPink,
                            fontSize = 11.5.sp
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
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(taggedFriends) { tag ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = HundredGramPink.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, HundredGramPink.copy(alpha = 0.5f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "@$tag",
                                    color = HundredGramTextPrimary,
                                    fontSize = 12.sp,
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
        }

        Spacer(modifier = Modifier.height(24.dp))
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

    if (showSaveDraftDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSaveDraftDialog = false },
            title = { Text("Save Draft? (ड्राफ़्ट सेव करें)", color = HundredGramTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Do you want to save your edited post as a draft or discard changes?", color = HundredGramTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showSaveDraftDialog = false
                        if (imageModel != null) {
                            viewModel.savePostDraft(
                                mediaUri = imageModel,
                                caption = captionInput,
                                location = locationInput,
                                filterId = selectedFilter.id,
                                onSaved = {
                                    Toast.makeText(context, "Draft Saved to Room!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        viewModel.navigateTo(ScreenDestination.Feed)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HundredGramPink)
                ) {
                    Text("Save Draft", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSaveDraftDialog = false
                        viewModel.navigateTo(ScreenDestination.Feed)
                    }
                ) {
                    Text("Discard", color = com.example.ui.theme.HundredGramLikeRed, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = com.example.ui.theme.HundredGramCardBackground
        )
    }
}

@Composable
fun BeautySliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = HundredGramTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${value.toInt()}%",
                color = HundredGramPink,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = HundredGramPink,
                activeTrackColor = HundredGramPink,
                inactiveTrackColor = Color.DarkGray
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
