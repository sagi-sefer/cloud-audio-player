package com.example.cloudaudioplayer.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cloudaudioplayer.domain.model.MediaItem
import com.example.cloudaudioplayer.domain.model.StorageType
import com.example.cloudaudioplayer.presentation.components.BidiText
import com.example.cloudaudioplayer.presentation.theme.CloudAudioPlayerTheme

@Composable
fun ArtistListScreen(
    artists: List<MediaItem.Folder>,
    storageType: StorageType,
    isLoading: Boolean,
    isSyncing: Boolean,
    onArtistClick: (MediaItem.Folder) -> Unit,
    onFlatAlbumsClick: () -> Unit,
    onSyncClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedTab: Int = 0,
    flatAlbums: List<MediaItem.Folder> = emptyList(),
    onAlbumClick: (MediaItem.Folder) -> Unit = {},
    onAlbumPlayClick: (MediaItem.Folder) -> Unit = {},
    onArtistsTabClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // TOP LEVEL ROW: App Name, Provider Badge, Sync Button, Settings Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Name
            BidiText(
                text = "CD COLLECTION",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            // Action Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
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
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // NEXT LEVEL ROW: Navigation Tabs (1 navigate by artist, 1 navigate by album)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab 1: Navigate by Artist
            Column(
                modifier = Modifier.clickable { onArtistsTabClick() }
            ) {
                BidiText(
                    text = "ARTISTS",
                    style = MaterialTheme.typography.headlineLarge,
                    color = if (selectedTab == 0) Color.White else Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .height(3.dp)
                        .width(48.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (selectedTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                )
            }

            // Tab 2: Navigate by Album
            Column(
                modifier = Modifier.clickable { onFlatAlbumsClick() }
            ) {
                BidiText(
                    text = "ALBUMS",
                    style = MaterialTheme.typography.headlineLarge,
                    color = if (selectedTab == 1) Color.White else Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .height(3.dp)
                        .width(48.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (selectedTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // CONTENT AREA (Artists list or Flat Albums grid)
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (selectedTab == 0) {
            // ARTISTS TAB CONTENT
            if (artists.isEmpty()) {
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
        } else {
            // ALBUMS TAB CONTENT
            if (flatAlbums.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        BidiText(
                            text = "No albums found in cloud folder",
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
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(flatAlbums, key = { it.id }) { album ->
                        AlbumCard(
                            album = album,
                            onClick = { onAlbumClick(album) },
                            onPlayClick = { onAlbumPlayClick(album) }
                        )
                    }
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

                Column {
                    BidiText(
                        text = artist.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (artist.storageType == StorageType.GOOGLE_DRIVE) "Drive" else "Dropbox",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
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

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun ArtistListScreenPreview() {
    CloudAudioPlayerTheme {
        ArtistListScreen(
            selectedTab = 0,
            artists = listOf(
                MediaItem.Folder("1", "חיים יבין", "", storageType = StorageType.GOOGLE_DRIVE),
                MediaItem.Folder("2", "תיסלם", "", storageType = StorageType.DROPBOX),
                MediaItem.Folder("3", "חיים יבין - בבר", "", storageType = StorageType.GOOGLE_DRIVE),
                MediaItem.Folder("4", "תיסלם אלבומים", "", storageType = StorageType.DROPBOX)
            ),
            storageType = StorageType.DROPBOX,
            isLoading = false,
            isSyncing = false,
            onArtistClick = {},
            onFlatAlbumsClick = {},
            onSyncClick = {},
            onSettingsClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun AlbumsTabPreview() {
    CloudAudioPlayerTheme {
        ArtistListScreen(
            selectedTab = 1,
            artists = emptyList(),
            flatAlbums = listOf(
                MediaItem.Folder("101", "רדיו חזק", "", storageType = StorageType.GOOGLE_DRIVE),
                MediaItem.Folder("102", "תיסלם 2", "", storageType = StorageType.DROPBOX),
                MediaItem.Folder("103", "שירים נוספים", "", storageType = StorageType.GOOGLE_DRIVE)
            ),
            storageType = StorageType.DROPBOX,
            isLoading = false,
            isSyncing = false,
            onArtistClick = {},
            onFlatAlbumsClick = {},
            onSyncClick = {},
            onSettingsClick = {}
        )
    }
}

