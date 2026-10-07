package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = HundredGramPink,
    onPrimary = Color.White,
    primaryContainer = HundredGramCardGlass,
    onPrimaryContainer = Color.White,
    secondary = HundredGramPurple,
    onSecondary = Color.White,
    secondaryContainer = HundredGramCardElevated,
    onSecondaryContainer = Color.White,
    tertiary = HundredGramOrange,
    onTertiary = Color.White,
    background = HundredGramDarkBackground,
    onBackground = HundredGramTextPrimary,
    surface = HundredGramCardBackground,
    onSurface = HundredGramTextPrimary,
    surfaceVariant = HundredGramCardElevated,
    onSurfaceVariant = HundredGramTextSecondary,
    surfaceTint = HundredGramPink,
    outline = HundredGramBorderSubtle,
    outlineVariant = HundredGramDivider
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun HundredGramTheme(
    content: @Composable () -> Unit
) {
    val dynamicColorScheme = if (ThemeConfig.isDark) {
        darkColorScheme(
            primary = HundredGramPink,
            onPrimary = Color.White,
            primaryContainer = HundredGramCardGlass,
            onPrimaryContainer = Color.White,
            secondary = HundredGramPurple,
            onSecondary = Color.White,
            secondaryContainer = HundredGramCardElevated,
            onSecondaryContainer = Color.White,
            tertiary = HundredGramOrange,
            onTertiary = Color.White,
            background = HundredGramDarkBackground,
            onBackground = HundredGramTextPrimary,
            surface = HundredGramCardBackground,
            onSurface = HundredGramTextPrimary,
            surfaceVariant = HundredGramCardElevated,
            onSurfaceVariant = HundredGramTextSecondary,
            surfaceTint = HundredGramPink,
            outline = HundredGramBorderSubtle,
            outlineVariant = HundredGramDivider
        )
    } else {
        androidx.compose.material3.lightColorScheme(
            primary = HundredGramPink,
            onPrimary = Color.White,
            primaryContainer = HundredGramCardGlass,
            onPrimaryContainer = Color.Black,
            secondary = HundredGramPurple,
            onSecondary = Color.White,
            secondaryContainer = HundredGramCardElevated,
            onSecondaryContainer = Color.Black,
            tertiary = HundredGramOrange,
            onTertiary = Color.White,
            background = HundredGramDarkBackground,
            onBackground = HundredGramTextPrimary,
            surface = HundredGramCardBackground,
            onSurface = HundredGramTextPrimary,
            surfaceVariant = HundredGramCardElevated,
            onSurfaceVariant = HundredGramTextSecondary,
            surfaceTint = HundredGramPink,
            outline = HundredGramBorderSubtle,
            outlineVariant = HundredGramDivider
        )
    }

    MaterialTheme(
        colorScheme = dynamicColorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}

