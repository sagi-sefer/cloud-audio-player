package com.example.cloudaudioplayer.domain.repository

import com.example.cloudaudioplayer.domain.model.MediaItem

interface MetadataRepository {
    suspend fun cacheFolderContents(parentFolderId: String, items: List<MediaItem>)
    suspend fun getCachedItems(parentFolderId: String): List<MediaItem>
    suspend fun getAllCachedArtists(): List<MediaItem.Folder>
    suspend fun getCachedAlbumsForArtist(artistFolderId: String): List<MediaItem.Folder>
    suspend fun getCachedTracksForAlbum(albumFolderId: String): List<MediaItem.AudioTrack>
    suspend fun clearCache()
}
