package com.example.cloudaudioplayer.application

import com.example.cloudaudioplayer.domain.model.MediaItem
import com.example.cloudaudioplayer.domain.model.StorageType
import com.example.cloudaudioplayer.domain.repository.CloudStorageRepository
import com.example.cloudaudioplayer.domain.repository.MetadataRepository
import javax.inject.Inject

class GetFolderContentsUseCase @Inject constructor(
    private val cloudStorageRepository: CloudStorageRepository,
    private val metadataRepository: MetadataRepository
) {
    suspend operator fun invoke(
        folderId: String?,
        storageType: StorageType,
        authData: String,
        rootFolder: String = "",
        forceRefresh: Boolean = false
    ): List<MediaItem> {
        val targetId = folderId ?: "root"

        if (!forceRefresh) {
            val cached = if (folderId == null) {
                metadataRepository.getAllCachedArtists()
            } else {
                metadataRepository.getCachedItems(targetId)
            }
            if (cached.isNotEmpty()) {
                return cached
            }
        }

        val remoteItems = if (folderId == null) {
            cloudStorageRepository.getRootFolders(storageType, authData, rootFolder)
        } else {
            val items = cloudStorageRepository.getFolderContents(folderId, storageType, authData)
            // If items are album folders, fetch their cover art URLs
            items.map { item ->
                if (item is MediaItem.Folder && item.coverArtUrl.isNullOrEmpty()) {
                    val cover = cloudStorageRepository.fetchAlbumCoverUrl(item.id, storageType, authData)
                    item.copy(coverArtUrl = cover)
                } else {
                    item
                }
            }
        }

        if (remoteItems.isNotEmpty()) {
            metadataRepository.cacheFolderContents(targetId, remoteItems)
        }

        return remoteItems
    }
}
