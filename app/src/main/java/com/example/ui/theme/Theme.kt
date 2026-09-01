package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisColorScheme = darkColorScheme(
    primary = JarvisCyan,
    onPrimary = Color.Black,
    primaryContainer = JarvisCardSurfaceVariant,
    onPrimaryContainer = JarvisCyanLight,
    secondary = JarvisBlueGlow,
    onSecondary = Color.Black,
    secondaryContainer = JarvisCardSurface,
    onSecondaryContainer = JarvisTextPrimary,
    tertiary = JarvisAccentGreen,
    onTertiary = Color.Black,
    background = JarvisDarkBackground,
    onBackground = JarvisTextPrimary,
    surface = JarvisCardSurface,
    onSurface = JarvisTextPrimary,
    surfaceVariant = JarvisCardSurfaceVariant,
    onSurfaceVariant = JarvisTextSecondary,
    outline = JarvisBorderColor,
    error = JarvisAccentRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to dark futuristic theme
    dynamicColor: Boolean = false, // Keep high-contrast JARVIS styling
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JarvisColorScheme,
        typography = Typography,
        content = content
    )
}

