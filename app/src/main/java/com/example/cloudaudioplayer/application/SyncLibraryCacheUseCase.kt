package com.example.cloudaudioplayer.application

import com.example.cloudaudioplayer.domain.model.MediaItem
import com.example.cloudaudioplayer.domain.model.StorageType
import com.example.cloudaudioplayer.domain.repository.CloudStorageRepository
import com.example.cloudaudioplayer.domain.repository.MetadataRepository
import javax.inject.Inject

class SyncLibraryCacheUseCase @Inject constructor(
    private val cloudStorageRepository: CloudStorageRepository,
    private val metadataRepository: MetadataRepository
) {
    suspend operator fun invoke(
        storageType: StorageType,
        authData: String,
        rootFolder: String = "",
        clearCacheFirst: Boolean = true
    ) {
        if (clearCacheFirst) {
            metadataRepository.clearCache()
        }

        val rootArtists = cloudStorageRepository.getRootFolders(storageType, authData, rootFolder)
        metadataRepository.cacheFolderContents("root", rootArtists)

        for (artist in rootArtists) {
            val albumsRaw = cloudStorageRepository.getFolderContents(artist.id, storageType, authData)
            val albums = mutableListOf<MediaItem>()

            for (item in albumsRaw) {
                if (item is MediaItem.Folder) {
                    val coverUrl = cloudStorageRepository.fetchAlbumCoverUrl(item.id, storageType, authData)
                    albums.add(item.copy(coverArtUrl = coverUrl))
                } else {
                    albums.add(item)
                }
            }

            metadataRepository.cacheFolderContents(artist.id, albums)

            for (album in albums.filterIsInstance<MediaItem.Folder>()) {
                val tracks = cloudStorageRepository.getFolderContents(album.id, storageType, authData)
                val coverUrl = album.coverArtUrl

                val updatedTracks = if (coverUrl != null) {
                    tracks.map { trackItem ->
                        if (trackItem is MediaItem.AudioTrack) trackItem.copy(coverArtUrl = coverUrl) else trackItem
                    }
                } else tracks

                metadataRepository.cacheFolderContents(album.id, updatedTracks)
            }
        }
    }
}
