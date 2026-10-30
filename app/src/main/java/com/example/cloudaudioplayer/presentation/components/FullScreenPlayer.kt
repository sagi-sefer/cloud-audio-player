package com.example.cloudaudioplayer.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.cloudaudioplayer.R
import com.example.cloudaudioplayer.domain.model.PlaybackState
import java.util.Locale

@Composable
fun FullScreenPlayer(
    playbackState: PlaybackState,
    onPlayPauseToggle: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playbackState.currentTrack ?: return

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header with dismiss button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Dismiss Player",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                BidiText(
                    text = "NOW PLAYING",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.size(56.dp))
            }

            // Album Art Display
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(track.coverArtUrl)
                        .crossfade(true)
                        .build(),
                    placeholder = painterResource(id = R.drawable.ic_default_album_cover),
                    error = painterResource(id = R.drawable.ic_default_album_cover),
                    contentDescription = "Album Cover",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Metadata Section with BidiText
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                BidiText(
                    text = track.title,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                BidiText(
                    text = track.artist,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                BidiText(
                    text = track.album,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Seek Slider and Timers
            Column(modifier = Modifier.fillMaxWidth()) {
                val currentPos = playbackState.currentPositionMs.toFloat()
                val totalDur = playbackState.durationMs.coerceAtLeast(1L).toFloat()

                Slider(
                    value = currentPos.coerceIn(0f, totalDur),
                    onValueChange = { onSeekTo(it.toLong()) },
                    valueRange = 0f..totalDur,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BidiText(
                        text = formatMs(playbackState.currentPositionMs),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    BidiText(
                        text = formatMs(playbackState.durationMs),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Car-friendly Large Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LargeDrivingButton(
                    icon = Icons.Default.SkipPrevious,
                    contentDescription = "Previous Track",
                    onClick = onSkipPrevious,
                    size = 72.dp,
                    iconSize = 40.dp
                )

                LargeDrivingButton(
                    icon = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                    onClick = onPlayPauseToggle,
                    size = 96.dp,
                    iconSize = 56.dp,
                    backgroundColor = MaterialTheme.colorScheme.primary,
                    tint = MaterialTheme.colorScheme.onPrimary
                )

                LargeDrivingButton(
                    icon = Icons.Default.SkipNext,
                    contentDescription = "Next Track",
                    onClick = onSkipNext,
                    size = 72.dp,
                    iconSize = 40.dp
                )
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}
