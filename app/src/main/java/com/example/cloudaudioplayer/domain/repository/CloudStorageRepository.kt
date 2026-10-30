package com.example.cloudaudioplayer.domain.repository

import com.example.cloudaudioplayer.domain.model.MediaItem
import com.example.cloudaudioplayer.domain.model.StorageType

interface CloudStorageRepository {
    suspend fun getRootFolders(storageType: StorageType, authData: String, rootFolder: String = ""): List<MediaItem.Folder>
    suspend fun getFolderContents(folderId: String, storageType: StorageType, authData: String): List<MediaItem>
    suspend fun getStreamUrl(track: MediaItem.AudioTrack, authData: String): String
    suspend fun fetchAlbumCoverUrl(folderId: String, storageType: StorageType, authData: String): String?
}
