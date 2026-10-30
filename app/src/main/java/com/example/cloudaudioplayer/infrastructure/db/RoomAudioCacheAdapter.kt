package com.example.cloudaudioplayer.infrastructure.db

import com.example.cloudaudioplayer.domain.model.MediaItem
import com.example.cloudaudioplayer.domain.repository.MetadataRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomAudioCacheAdapter @Inject constructor(
    private val dao: AudioCacheDao
) : MetadataRepository {

    override suspend fun cacheFolderContents(
        parentFolderId: String,
        items: List<MediaItem>
    ) {
        val entities = items.map { MediaItemEntity.fromDomain(it, parentFolderId) }
        dao.insertItems(entities)
    }

    override suspend fun getCachedItems(parentFolderId: String): List<MediaItem> {
        return dao.getItemsByParent(parentFolderId).map { it.toDomain() }
    }

    override suspend fun getAllCachedArtists(): List<MediaItem.Folder> {
        return dao.getAllArtists().map { it.toDomain() }.filterIsInstance<MediaItem.Folder>()
    }

    override suspend fun getCachedAlbumsForArtist(artistFolderId: String): List<MediaItem.Folder> {
        return dao.getAlbumsForArtist(artistFolderId).map { it.toDomain() }.filterIsInstance<MediaItem.Folder>()
    }

    override suspend fun getAllCachedAlbums(): List<MediaItem.Folder> {
        return dao.getAllAlbums().map { it.toDomain() }.filterIsInstance<MediaItem.Folder>()
    }

    override suspend fun getCachedTracksForAlbum(albumFolderId: String): List<MediaItem.AudioTrack> {
        return dao.getTracksForAlbum(albumFolderId).map { it.toDomain() }.filterIsInstance<MediaItem.AudioTrack>()
    }

    override suspend fun clearCache() {
        dao.clearAll()
    }
}
