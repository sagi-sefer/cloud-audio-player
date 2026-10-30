package com.example.cloudaudioplayer.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.cloudaudioplayer.domain.model.MediaItem
import com.example.cloudaudioplayer.presentation.components.BidiText

@Composable
fun AlbumGridScreen(
    artist: MediaItem.Folder,
    albums: List<MediaItem.Folder>,
    isLoading: Boolean,
    onAlbumClick: (MediaItem.Folder) -> Unit,
    onAlbumPlayClick: (MediaItem.Folder) -> Unit,
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
                    contentDescription = "Back to Artists",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                BidiText(
                    text = artist.name.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                BidiText(
                    text = "ALBUMS",
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
        } else if (albums.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                BidiText(
                    text = "No albums found for this artist",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Gray
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(albums, key = { it.id }) { album ->
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

@Composable
private fun AlbumCard(
    album: MediaItem.Folder,
    onClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Album cover image with Coil + fallback vector placeholder + Play FAB overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(album.coverArtUrl)
                        .crossfade(true)
                        .build(),
                    placeholder = painterResource(id = R.drawable.ic_default_album_cover),
                    error = painterResource(id = R.drawable.ic_default_album_cover),
                    contentDescription = album.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Spotify-style Play Button Overlay on album art
                FloatingActionButton(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(48.dp),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Album",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            BidiText(
                text = album.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
