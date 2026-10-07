package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PrivacyLevel
import com.example.ui.theme.HundredGramCardBackground
import com.example.ui.theme.HundredGramCardElevated
import com.example.ui.theme.HundredGramPink
import com.example.ui.theme.HundredGramTextPrimary
import com.example.ui.theme.HundredGramTextSecondary

data class PrivacyOptionDetail(
    val level: PrivacyLevel,
    val titleEnglish: String,
    val titleHindi: String,
    val description: String,
    val icon: ImageVector,
    val iconColor: Color
)

fun getPrivacyOptionDetails(): List<PrivacyOptionDetail> {
    return listOf(
        PrivacyOptionDetail(
            level = PrivacyLevel.PUBLIC,
            titleEnglish = "Public",
            titleHindi = "पब्लिक",
            description = "Anyone on or off HundredGram can view this post/reel.",
            icon = Icons.Default.Public,
            iconColor = Color(0xFF4CAF50)
        ),
        PrivacyOptionDetail(
            level = PrivacyLevel.FRIENDS,
            titleEnglish = "Friends & Followers",
            titleHindi = "मित्र व फॉलोअर्स",
            description = "Only your approved followers & friends can view this.",
            icon = Icons.Default.Group,
            iconColor = Color(0xFF2196F3)
        ),
        PrivacyOptionDetail(
            level = PrivacyLevel.ONLY_ME,
            titleEnglish = "Only Me",
            titleHindi = "केवल मैं (Private)",
            description = "Private - Only you can view this post/reel.",
            icon = Icons.Default.Lock,
            iconColor = Color(0xFFFF5252)
        )
    )
}

@Composable
fun PrivacySelector(
    selectedPrivacy: PrivacyLevel,
    onPrivacySelected: (PrivacyLevel) -> Unit,
    modifier: Modifier = Modifier,
    labelTitle: String = "🔒 Post & Reel Privacy (प्राइवेसी सेटिंग्स)"
) {
    var showSheet by remember { mutableStateOf(false) }
    val activeDetail = remember(selectedPrivacy) {
        getPrivacyOptionDetails().find { it.level == selectedPrivacy } ?: getPrivacyOptionDetails()[0]
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (labelTitle.isNotBlank()) {
            Text(
                text = labelTitle,
                color = HundredGramTextPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Clickable Button Pill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(HundredGramCardElevated)
                .border(1.2.dp, HundredGramPink.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                .clickable { showSheet = true }
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(activeDetail.iconColor.copy(alpha = 0.20f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = activeDetail.icon,
                            contentDescription = activeDetail.titleEnglish,
                            tint = activeDetail.iconColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${activeDetail.titleEnglish} • ${activeDetail.titleHindi}",
                            color = HundredGramTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = activeDetail.description,
                            color = HundredGramTextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Change Privacy",
                    tint = HundredGramPink,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    if (showSheet) {
        PrivacySelectionBottomSheet(
            selectedPrivacy = selectedPrivacy,
            onPrivacySelected = { level ->
                onPrivacySelected(level)
                showSheet = false
            },
            onDismiss = { showSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySelectionBottomSheet(
    selectedPrivacy: PrivacyLevel,
    onPrivacySelected: (PrivacyLevel) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                text = "🔒 Select Privacy (प्राइवेसी चुनें)",
                color = HundredGramTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Select who can see your post or reel on HundredGram",
                color = HundredGramTextSecondary,
                fontSize = 12.5.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            getPrivacyOptionDetails().forEach { option ->
                val isSelected = (selectedPrivacy == option.level)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) HundredGramPink.copy(alpha = 0.18f) else HundredGramCardElevated)
                        .border(
                            width = if (isSelected) 1.8.dp else 0.8.dp,
                            color = if (isSelected) HundredGramPink else Color.White.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onPrivacySelected(option.level) }
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(option.iconColor.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = option.icon,
                                    contentDescription = option.titleEnglish,
                                    tint = option.iconColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "${option.titleEnglish} (${option.titleHindi})",
                                    color = HundredGramTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = option.description,
                                    color = HundredGramTextSecondary,
                                    fontSize = 11.5.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(HundredGramPink),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun PrivacyBadgePill(
    privacyStr: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val level = remember(privacyStr) {
        try { PrivacyLevel.valueOf(privacyStr.uppercase()) } catch (_: Exception) { PrivacyLevel.PUBLIC }
    }
    val option = remember(level) {
        getPrivacyOptionDetails().find { it.level == level } ?: getPrivacyOptionDetails()[0]
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .border(0.8.dp, option.iconColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = option.titleEnglish,
                tint = option.iconColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = option.titleEnglish,
                color = Color.White,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
