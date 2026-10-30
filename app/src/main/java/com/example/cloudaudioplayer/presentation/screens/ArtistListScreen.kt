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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.unit.dp
import com.example.cloudaudioplayer.domain.model.MediaItem
import com.example.cloudaudioplayer.domain.model.StorageType
import com.example.cloudaudioplayer.presentation.components.BidiText

@Composable
fun ArtistListScreen(
    artists: List<MediaItem.Folder>,
    storageType: StorageType,
    isLoading: Boolean,
    isSyncing: Boolean,
    onArtistClick: (MediaItem.Folder) -> Unit,
    onSyncClick: () -> Unit,
    onSettingsClick: () -> Unit,
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                BidiText(
                    text = "CD COLLECTION",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                BidiText(
                    text = "ARTISTS",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Provider Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (storageType == StorageType.GOOGLE_DRIVE) "Drive" else "Dropbox",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onSyncClick,
                    enabled = !isSyncing
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Sync Library",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (artists.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BidiText(
                        text = "No artists found in cloud folder",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    BidiText(
                        text = "Tap settings icon to configure auth or tap sync",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(artists, key = { it.id }) { artist ->
                    ArtistCard(
                        artist = artist,
                        onClick = { onArtistClick(artist) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistCard(
    artist: MediaItem.Folder,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                BidiText(
                    text = artist.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
