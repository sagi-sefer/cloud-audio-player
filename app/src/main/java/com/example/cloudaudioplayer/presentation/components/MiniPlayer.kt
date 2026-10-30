package com.example.cloudaudioplayer.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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

@Composable
fun MiniPlayer(
    playbackState: PlaybackState,
    onPlayPauseToggle: () -> Unit,
    onSkipNext: () -> Unit,
    onClickMiniPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playbackState.currentTrack ?: return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClickMiniPlayer),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column {
            if (playbackState.durationMs > 0) {
                val progress = (playbackState.currentPositionMs.toFloat() / playbackState.durationMs).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(track.coverArtUrl)
                        .crossfade(true)
                        .build(),
                    placeholder = painterResource(id = R.drawable.ic_default_album_cover),
                    error = painterResource(id = R.drawable.ic_default_album_cover),
                    contentDescription = "Album Cover",
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    contentScale = ContentScale.Crop
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    BidiText(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    BidiText(
                        text = "${track.artist} • ${track.album}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onPlayPauseToggle,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(
                        onClick = onSkipNext,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Next Track",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}
