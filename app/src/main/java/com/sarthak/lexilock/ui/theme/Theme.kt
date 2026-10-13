package com.sarthak.lexilock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = LexiDarkAccent,
    onPrimary = Color.Black,
    secondary = LexiDarkTextSecondary,
    onSecondary = Color.Black,
    tertiary = LexiDarkAccent,
    background = LexiDarkBg,
    onBackground = LexiDarkTextPrimary,
    surface = LexiDarkSurfaceSubtle,
    onSurface = LexiDarkTextPrimary,
    surfaceVariant = Color(0xFF202020),
    onSurfaceVariant = LexiDarkTextSecondary,
    outline = LexiDarkDivider,
    outlineVariant = LexiDarkDivider
)

private val LightColorScheme = lightColorScheme(
    primary = LexiLightAccent,
    onPrimary = Color.White,
    secondary = LexiLightTextSecondary,
    onSecondary = Color.White,
    tertiary = LexiLightAccent,
    background = LexiLightBg,
    onBackground = LexiLightTextPrimary,
    surface = Color.White,
    onSurface = LexiLightTextPrimary,
    surfaceVariant = LexiLightSurfaceSubtle,
    onSurfaceVariant = LexiLightTextSecondary,
    outline = LexiLightDivider,
    outlineVariant = LexiLightDivider
)

@Composable
fun LexiLockTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}