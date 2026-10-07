package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ripple
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.ui.graphics.SolidColor
import coil.compose.AsyncImage
import com.example.ui.theme.HundredGramBlue
import com.example.ui.theme.HundredGramDarkBackground
import com.example.ui.theme.InstagramStoryGradient
import coil.compose.AsyncImage
import com.example.ui.theme.HundredGramBlue
import com.example.ui.theme.HundredGramBorderSubtle
import com.example.ui.theme.HundredGramButtonGradient
import com.example.ui.theme.HundredGramCardBackground
import com.example.ui.theme.HundredGramCardElevated
import com.example.ui.theme.HundredGramCardGlass
import com.example.ui.theme.HundredGramDarkBackground
import com.example.ui.theme.HundredGramPink
import com.example.ui.theme.HundredGramTextPrimary
import com.example.ui.theme.HundredGramTextSecondary
import com.example.ui.theme.HundredGramDivider
import com.example.ui.theme.InstagramStoryGradient

@Composable
fun UserAvatar(
    avatarUrl: String,
    size: Dp = 42.dp,
    hasActiveStory: Boolean = false,
    isSeen: Boolean = false,
    isVerified: Boolean = false,
    isOnline: Boolean = true,
    showOnlineStatus: Boolean = true,
    onClick: () -> Unit = {}
) {
    val safeAvatarUrl = remember(avatarUrl) {
        com.example.util.ImageUtils.getSafeImageModel(avatarUrl)
    }
    val totalSize = if (hasActiveStory) size + 8.dp else size
    Box(
        modifier = Modifier.size(totalSize),
        contentAlignment = Alignment.Center
    ) {
        if (hasActiveStory) {
            // Radiant Story Ring
            Box(
                modifier = Modifier
                    .size(totalSize)
                    .clip(CircleShape)
                    .background(if (isSeen) SolidColor(Color(0xFF383D4F)) else InstagramStoryGradient)
                    .padding(2.5.dp)
                    .clip(CircleShape)
                    .background(HundredGramDarkBackground)
                    .padding(2.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, radius = totalSize / 2),
                        onClick = onClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = safeAvatarUrl,
                        contentDescription = "User Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(size)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(size)
                            .clip(CircleShape)
                            .background(HundredGramCardElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User Avatar Placeholder",
                            tint = HundredGramTextSecondary,
                            modifier = Modifier.size(size * 0.65f)
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(HundredGramCardElevated)
                    .border(1.dp, HundredGramBorderSubtle.copy(alpha = 0.6f), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, radius = size / 2),
                        onClick = onClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = safeAvatarUrl,
                        contentDescription = "User Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(size)
                            .clip(CircleShape)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User Avatar Placeholder",
                        tint = HundredGramTextSecondary,
                        modifier = Modifier.size(size * 0.65f)
                    )
                }
            }
        }

        if (isVerified) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Verified Badge",
                tint = HundredGramBlue,
                modifier = Modifier
                    .size(17.dp)
                    .align(Alignment.BottomEnd)
                    .background(HundredGramDarkBackground, CircleShape)
                    .border(1.5.dp, HundredGramDarkBackground, CircleShape)
            )
        }

        if (isOnline && showOnlineStatus) {
            val dotSize = (size * 0.28f).coerceIn(9.dp, 16.dp)
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(Color(0xFF22C55E))
                    .border(1.5.dp, HundredGramDarkBackground, CircleShape)
                    .align(if (isVerified) Alignment.BottomStart else Alignment.BottomEnd)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SystemCompatibilityBadgeRow(
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    showDetailedRequirements: Boolean = true
) {
    val badges = listOf(
        CompatibilityBadgeData(
            label = "ARM64-v8a",
            subtext = "64-bit ABI Native",
            icon = Icons.Default.DeveloperBoard,
            badgeColor = Color(0xFF6C5CE7)
        ),
        CompatibilityBadgeData(
            label = "RAM 4 GB+ Rec.",
            subtext = "2 GB Min / 256MB Heap",
            icon = Icons.Default.Memory,
            badgeColor = Color(0xFF00CEC9)
        ),
        CompatibilityBadgeData(
            label = "Android 8.0+",
            subtext = "API 26–35 Compatible",
            icon = Icons.Default.PhoneAndroid,
            badgeColor = Color(0xFF0984E3)
        ),
        CompatibilityBadgeData(
            label = "H.264 / HEVC",
            subtext = "Hardware Enc/Dec",
            icon = Icons.Default.Videocam,
            badgeColor = Color(0xFFE84393)
        ),
        CompatibilityBadgeData(
            label = "100 MB Free",
            subtext = "High-speed Storage",
            icon = Icons.Default.Storage,
            badgeColor = Color(0xFFFD9644)
        )
    )

    if (scrollable) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            badges.forEach { badge ->
                CompatibilityBadgeItem(badge = badge, compact = !showDetailedRequirements)
            }
        }
    } else {
        FlowRow(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            badges.forEach { badge ->
                CompatibilityBadgeItem(badge = badge, compact = !showDetailedRequirements)
            }
        }
    }
}

data class CompatibilityBadgeData(
    val label: String,
    val subtext: String,
    val icon: ImageVector,
    val badgeColor: Color
)

@Composable
fun CompatibilityBadgeItem(
    badge: CompatibilityBadgeData,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(HundredGramCardElevated)
            .border(1.dp, badge.badgeColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(badge.badgeColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = badge.icon,
                    contentDescription = badge.label,
                    tint = badge.badgeColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(7.dp))
            Column {
                Text(
                    text = badge.label,
                    color = HundredGramTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                if (!compact) {
                    Text(
                        text = badge.subtext,
                        color = HundredGramTextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun GradientActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 46.dp
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) HundredGramButtonGradient else Brush.linearGradient(listOf(Color.DarkGray, Color.Gray)))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color.White),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.5.sp,
                letterSpacing = 0.2.sp
            )
        }
    }
}

@Composable
fun SecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = 46.dp
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .background(HundredGramCardElevated)
            .border(1.dp, HundredGramBorderSubtle, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color.White),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = HundredGramTextPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = HundredGramTextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun AnimatedLoadingDots(
    modifier: Modifier = Modifier,
    dotColor: Color = HundredGramPink,
    dotSize: Dp = 10.dp,
    spacing: Dp = 6.dp
) {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "dots_anim")

    val scales = (0..2).map { index ->
        infiniteTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1.0f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = androidx.compose.animation.core.tween(
                    durationMillis = 600,
                    delayMillis = index * 180,
                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                ),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
            ),
            label = "dot_scale_$index"
        )
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        scales.forEach { scaleAnim ->
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .scale(scaleAnim.value)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

@Composable
fun TextPostGraphicCard(
    post: com.example.data.PostEntity,
    modifier: Modifier = Modifier
) {
    val colors = when (post.textBgGradient) {
        "Midnight" -> listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
        "Neon Cyber", "Neon" -> listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0))
        "Rose Gold", "Rose" -> listOf(Color(0xFFEC6EAD), Color(0xFFC91E63))
        "Emerald" -> listOf(Color(0xFF0575E6), Color(0xFF00F260))
        "Ocean Blue", "Ocean" -> listOf(Color(0xFF2193b0), Color(0xFF6dd5ed))
        "Velvet" -> listOf(Color(0xFF3A1C71), Color(0xFFD76D77), Color(0xFFFFAF7B))
        "Gold Luxury", "Gold" -> listOf(Color(0xFFF2994A), Color(0xFFF2C94C))
        "Pure Black", "Dark" -> listOf(Color(0xFF181824), Color(0xFF222232))
        "Pastel Violet" -> listOf(Color(0xFFA88FEB), Color(0xFF6A82FB))
        "Crimson Red" -> listOf(Color(0xFF800020), Color(0xFFE50914))
        "Sunny Orange" -> listOf(Color(0xFFFF8008), Color(0xFFFFC837))
        "Deep Blue" -> listOf(Color(0xFF000046), Color(0xFF1CB5E0))
        "Mint Green" -> listOf(Color(0xFF11998e), Color(0xFF38ef7d))
        else -> listOf(Color(0xFFFF512F), Color(0xFFDD2476)) // Sunset
    }
    val brush = Brush.linearGradient(colors)
    val fontColor = try { Color(android.graphics.Color.parseColor(post.textFontColor)) } catch (_: Exception) { Color.White }
    val fontWeight = if (post.textFontStyle.contains("Bold", ignoreCase = true)) FontWeight.ExtraBold else FontWeight.Normal
    val fontStyle = if (post.textFontStyle.contains("Italic", ignoreCase = true) || post.textFontStyle.contains("Cursive", ignoreCase = true)) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal
    val fontFamily = when {
        post.textFontStyle.contains("Serif", ignoreCase = true) -> androidx.compose.ui.text.font.FontFamily.Serif
        post.textFontStyle.contains("Cursive", ignoreCase = true) -> androidx.compose.ui.text.font.FontFamily.Cursive
        post.textFontStyle.contains("Monospace", ignoreCase = true) -> androidx.compose.ui.text.font.FontFamily.Monospace
        else -> androidx.compose.ui.text.font.FontFamily.Default
    }
    val textAlign = when (post.textAlignment) {
        "Left" -> androidx.compose.ui.text.style.TextAlign.Start
        "Right" -> androidx.compose.ui.text.style.TextAlign.End
        else -> androidx.compose.ui.text.style.TextAlign.Center
    }

    val authorName = post.authorDisplayName.ifBlank { post.authorUsername }
    val displayContent = if (post.text.isNotBlank()) {
        post.text
    } else if (post.caption.contains("__ roar 🦁")) {
        post.caption.ifBlank { "$authorName __ roar 🦁 Status" }
    } else {
        "$authorName __ roar 🦁 ${post.caption.ifBlank { "Status" }}"
    }

    Box(
        modifier = modifier
            .background(brush)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayContent,
            color = fontColor,
            fontSize = if (displayContent.length < 50) 20.sp else if (displayContent.length < 100) 16.sp else 13.5.sp,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontFamily = fontFamily,
            textAlign = textAlign,
            lineHeight = 26.sp
        )
    }
}



