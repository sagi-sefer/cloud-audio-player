package com.example.cloudaudioplayer.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cloudaudioplayer.domain.model.MediaItem
import com.example.cloudaudioplayer.presentation.components.BidiText

@Composable
fun TrackListScreen(
    artist: MediaItem.Folder,
    album: MediaItem.Folder,
    tracks: List<MediaItem.AudioTrack>,
    isLoading: Boolean,
    currentTrackId: String?,
    onTrackClick: (MediaItem.AudioTrack) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Albums",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                BidiText(
                    text = "${artist.name} • ${album.name}".uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                BidiText(
                    text = "TRACKS",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (tracks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                BidiText(
                    text = "No audio tracks found in this album",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                    TrackRow(
                        trackNumber = index + 1,
                        track = track,
                        isCurrentTrack = track.id == currentTrackId,
                        onClick = { onTrackClick(track) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackRow(
    trackNumber: Int,
    track: MediaItem.AudioTrack,
    isCurrentTrack: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentTrack) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = String.format("%02d", trackNumber),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isCurrentTrack) MaterialTheme.colorScheme.primary else Color.Gray,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    BidiText(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrentTrack) MaterialTheme.colorScheme.primary else Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    BidiText(
                        text = track.format.extension.uppercase(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isCurrentTrack) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = if (isCurrentTrack) MaterialTheme.colorScheme.onPrimary else Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
