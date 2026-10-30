package com.example.cloudaudioplayer.infrastructure.storage

import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.FolderMetadata
import com.dropbox.core.v2.files.FileMetadata
import com.example.cloudaudioplayer.domain.model.AudioFormat
import com.example.cloudaudioplayer.domain.model.MediaItem
import com.example.cloudaudioplayer.domain.model.StorageType
import com.example.cloudaudioplayer.domain.repository.CloudStorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DropboxStorageAdapter @Inject constructor() : CloudStorageRepository {

    private fun getDropboxClient(accessToken: String): DbxClientV2 {
        val config = DbxRequestConfig.newBuilder("CloudAudioPlayer").build()
        return DbxClientV2(config, accessToken)
    }

    override suspend fun getRootFolders(
        storageType: StorageType,
        authData: String,
        rootFolder: String
    ): List<MediaItem.Folder> = withContext(Dispatchers.IO) {
        try {
            val client = getDropboxClient(authData)
            val path = if (rootFolder.isNotBlank() && rootFolder != "Cloud Root") {
                if (rootFolder.startsWith("/")) rootFolder else "/$rootFolder"
            } else ""
            val result = client.files().listFolder(path)
            result.entries.filterIsInstance<FolderMetadata>()
                .sortedBy { it.name }
                .map { folder ->
                    MediaItem.Folder(
                        id = folder.id,
                        name = folder.name,
                        path = folder.pathLower ?: "/${folder.name}",
                        parentId = if (path.isEmpty()) "root" else path
                    )
                }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun getFolderContents(
        folderId: String,
        storageType: StorageType,
        authData: String
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        try {
            val client = getDropboxClient(authData)
            val path = if (folderId == "root") "" else folderId
            val result = client.files().listFolder(path)

            val items = mutableListOf<MediaItem>()
            for (entry in result.entries.sortedBy { it.name }) {
                when (entry) {
                    is FolderMetadata -> {
                        items.add(
                            MediaItem.Folder(
                                id = entry.pathLower ?: entry.id,
                                name = entry.name,
                                path = entry.pathLower ?: "/${entry.name}",
                                parentId = folderId
                            )
                        )
                    }
                    is FileMetadata -> {
                        val format = AudioFormat.fromExtension(entry.name)
                        if (format != AudioFormat.UNKNOWN) {
                            items.add(
                                MediaItem.AudioTrack(
                                    id = entry.pathLower ?: entry.id,
                                    title = entry.name.substringBeforeLast('.'),
                                    artist = "Artist",
                                    album = "Album",
                                    path = entry.pathLower ?: "/${entry.name}",
                                    durationMs = 0L,
                                    format = format,
                                    coverArtUrl = null,
                                    parentId = folderId,
                                    storageType = StorageType.DROPBOX
                                )
                            )
                        }
                    }
                }
            }
            items
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun getStreamUrl(
        track: MediaItem.AudioTrack,
        authData: String
    ): String = withContext(Dispatchers.IO) {
        try {
            val client = getDropboxClient(authData)
            val tempLink = client.files().getTemporaryLink(track.path)
            tempLink.link
        } catch (_: Exception) {
            ""
        }
    }

    override suspend fun fetchAlbumCoverUrl(
        folderId: String,
        storageType: StorageType,
        authData: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val client = getDropboxClient(authData)
            val path = if (folderId == "root" || folderId.isEmpty()) "" else folderId
            val result = client.files().listFolder(path)

            val coverEntry = result.entries.filterIsInstance<FileMetadata>().firstOrNull { file ->
                val name = file.name.lowercase()
                name == "cover.jpg" || name == "cover.png" || name == "folder.jpg"
            }

            if (coverEntry != null) {
                val tempLink = client.files().getTemporaryLink(coverEntry.pathLower)
                tempLink.link
            } else null
        } catch (_: Exception) {
            null
        }
    }
}
