package com.example.cloudaudioplayer.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryAmber,
    onPrimary = OnPrimaryDark,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkCardSurface,
    onBackground = TextHighContrast,
    onSurface = TextHighContrast,
    onSurfaceVariant = TextMediumContrast
)

@Composable
fun CloudAudioPlayerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
