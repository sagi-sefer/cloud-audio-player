package com.example.cloudaudioplayer.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cloudaudioplayer.presentation.theme.DrivingAccent
import com.example.cloudaudioplayer.presentation.theme.DrivingButtonBackground

@Composable
fun LargeDrivingButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    iconSize: Dp = 36.dp,
    backgroundColor: Color = DrivingButtonBackground,
    tint: Color = Color.White
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = DrivingAccent),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun DrivingPlaybackBar(
    isPlaying: Boolean,
    onPlayPauseToggle: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onSkipPrevious,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Previous Track",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }

        LargeDrivingButton(
            icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            onClick = onPlayPauseToggle,
            size = 80.dp,
            iconSize = 48.dp,
            backgroundColor = MaterialTheme.colorScheme.primary,
            tint = MaterialTheme.colorScheme.onPrimary
        )

        IconButton(
            onClick = onSkipNext,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Next Track",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}
