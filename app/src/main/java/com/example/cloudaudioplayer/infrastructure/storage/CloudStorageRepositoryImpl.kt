package com.example.cloudaudioplayer.infrastructure.storage

import com.example.cloudaudioplayer.domain.model.MediaItem
import com.example.cloudaudioplayer.domain.model.StorageType
import com.example.cloudaudioplayer.domain.repository.CloudStorageRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudStorageRepositoryImpl @Inject constructor(
    private val googleDriveStorageAdapter: GoogleDriveStorageAdapter,
    private val dropboxStorageAdapter: DropboxStorageAdapter
) : CloudStorageRepository {

    private fun getAdapter(storageType: StorageType): CloudStorageRepository {
        return when (storageType) {
            StorageType.GOOGLE_DRIVE -> googleDriveStorageAdapter
            StorageType.DROPBOX -> dropboxStorageAdapter
        }
    }

    override suspend fun getRootFolders(
        storageType: StorageType,
        authData: String,
        rootFolder: String
    ): List<MediaItem.Folder> {
        return getAdapter(storageType).getRootFolders(storageType, authData, rootFolder)
    }

    override suspend fun getFolderContents(
        folderId: String,
        storageType: StorageType,
        authData: String
    ): List<MediaItem> {
        return getAdapter(storageType).getFolderContents(folderId, storageType, authData)
    }

    override suspend fun getStreamUrl(
        track: MediaItem.AudioTrack,
        authData: String
    ): String {
        return getAdapter(track.storageType).getStreamUrl(track, authData)
    }

    override suspend fun fetchAlbumCoverUrl(
        folderId: String,
        storageType: StorageType,
        authData: String
    ): String? {
        return getAdapter(storageType).fetchAlbumCoverUrl(folderId, storageType, authData)
    }
}
