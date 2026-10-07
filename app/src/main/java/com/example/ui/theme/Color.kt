package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Singleton for globally managing theme states
object ThemeConfig {
    var isDark by mutableStateOf(true)
}

// Cinematic Deep Dark Backgrounds changing based on theme
val HundredGramDarkBackground: Color
    get() = if (ThemeConfig.isDark) Color(0xFF08090E) else Color(0xFFF4F6F9)

val HundredGramCardBackground: Color
    get() = if (ThemeConfig.isDark) Color(0xFF11141F) else Color(0xFFFFFFFF)

val HundredGramCardElevated: Color
    get() = if (ThemeConfig.isDark) Color(0xFF191D2C) else Color(0xFFEDEFF5)

val HundredGramCardGlass: Color
    get() = if (ThemeConfig.isDark) Color(0xFF22273B) else Color(0xFFE2E4EB)

// Accent & Brand Colors
val HundredGramPink = Color(0xFFFF2D75)
val HundredGramPurple = Color(0xFF8A3FFC)
val HundredGramOrange = Color(0xFFFF6F3C)
val HundredGramYellow = Color(0xFFFFB020)
val HundredGramBlue = Color(0xFF0072F5)
val HundredGramCyan = Color(0xFF00D2D3)

// Text Colors changing based on theme
val HundredGramTextPrimary: Color
    get() = if (ThemeConfig.isDark) Color(0xFFFFFFFF) else Color(0xFF1E2229)

val HundredGramTextSecondary: Color
    get() = if (ThemeConfig.isDark) Color(0xFFA6ACBE) else Color(0xFF62697A)

val HundredGramTextTertiary: Color
    get() = if (ThemeConfig.isDark) Color(0xFF687087) else Color(0xFF8A93A6)

// Borders & Dividers changing based on theme
val HundredGramDivider: Color
    get() = if (ThemeConfig.isDark) Color(0xFF222738) else Color(0xFFE2E4ED)

val HundredGramBorderSubtle: Color
    get() = if (ThemeConfig.isDark) Color(0xFF2D3349) else Color(0xFFD1D5DB)

val HundredGramBorderGlow: Color
    get() = if (ThemeConfig.isDark) Color(0xFF3F4866) else Color(0xFF9CA3AF)


// Social Action Colors
val HundredGramLikeRed = Color(0xFFFF2A55)
val HundredGramSuccessGreen = Color(0xFF10B981)

// Radiant Gradients
val InstagramStoryGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF8A3FFC),
        Color(0xFFFF2D75),
        Color(0xFFFF6F3C),
        Color(0xFFFFB020)
    )
)

val HundredGramButtonGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFFFF2D75),
        Color(0xFF8A3FFC)
    )
)

val HundredGramVibrantGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF6366F1),
        Color(0xFFA855F7),
        Color(0xFFFF2D75)
    )
)

val HundredGramCardGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF161A29),
        Color(0xFF10121D)
    )
)

