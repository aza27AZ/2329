package com.worklog.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Blue,
    onPrimary = White,
    primaryContainer = Blue.copy(alpha = 0.1f),
    onPrimaryContainer = Blue,
    secondary = Green,
    onSecondary = White,
    background = GrayBackground,
    onBackground = Black,
    surface = White,
    onSurface = Black,
    surfaceVariant = GrayBackground,
    onSurfaceVariant = DarkGray,
    outline = GrayLight,
    outlineVariant = GrayLight.copy(alpha = 0.3f)
)

@Composable
fun WorkLogTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
