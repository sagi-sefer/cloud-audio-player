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
            val path = if (rootFolder.isNotBlank() && rootFolder != "Cloud Root" && rootFolder != "root") {
                if (rootFolder.startsWith("/")) rootFolder else "/$rootFolder"
            } else ""
            
            android.util.Log.d("DropboxStorageAdapter", "Listing folder path: '$path' (rootFolder parameter: '$rootFolder')")
            val result = client.files().listFolder(path)
            val folders = result.entries.filterIsInstance<FolderMetadata>()
                .sortedBy { it.name }
                .map { folder ->
                    val folderPath = folder.pathLower ?: "/${folder.name}"
                    MediaItem.Folder(
                        id = folderPath,
                        name = folder.name,
                        path = folderPath,
                        parentId = if (path.isEmpty()) "root" else path,
                        storageType = StorageType.DROPBOX
                    )
                }
            android.util.Log.d("DropboxStorageAdapter", "Found ${folders.size} folders at '$path'")
            folders
        } catch (e: Exception) {
            android.util.Log.e("DropboxStorageAdapter", "Error listing folder path '$rootFolder': ${e.javaClass.simpleName} - ${e.message}", e)
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
            val path = if (folderId == "root" || folderId.isEmpty()) "" else {
                if (folderId.startsWith("/")) folderId else "/$folderId"
            }
            val result = client.files().listFolder(path)

            val items = mutableListOf<MediaItem>()
            for (entry in result.entries.sortedBy { it.name }) {
                when (entry) {
                    is FolderMetadata -> {
                        val folderPath = entry.pathLower ?: "/${entry.name}"
                        items.add(
                            MediaItem.Folder(
                                id = folderPath,
                                name = entry.name,
                                path = folderPath,
                                parentId = folderId,
                                storageType = StorageType.DROPBOX
                            )
                        )
                    }
                    is FileMetadata -> {
                        val format = AudioFormat.fromExtension(entry.name)
                        if (format != AudioFormat.UNKNOWN) {
                            val filePath = entry.pathLower ?: "/${entry.name}"
                            items.add(
                                MediaItem.AudioTrack(
                                    id = filePath,
                                    title = entry.name.substringBeforeLast('.'),
                                    artist = "Artist",
                                    album = "Album",
                                    path = filePath,
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
        } catch (e: Exception) {
            android.util.Log.e("DropboxStorageAdapter", "Error getting folder contents for path '$folderId': ${e.message}", e)
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

            val files = result.entries.filterIsInstance<FileMetadata>()

            val coverEntry = files.firstOrNull { file ->
                val name = file.name.lowercase()
                name == "cover.jpg" || name == "cover.jpeg" || name == "cover.png" || name == "cover.webp" ||
                name == "folder.jpg" || name == "folder.jpeg" || name == "folder.png" || name == "folder.webp" ||
                name == "album.jpg" || name == "album.jpeg" || name == "album.png" || name == "album.webp" ||
                name == "front.jpg" || name == "front.jpeg" || name == "front.png" ||
                name == "art.jpg" || name == "art.png" ||
                name.startsWith("cover") || name.startsWith("folder") || name.startsWith("album")
            } ?: files.firstOrNull { file ->
                val name = file.name.lowercase()
                name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp")
            }

            if (coverEntry != null) {
                val tempLink = client.files().getTemporaryLink(coverEntry.pathLower)
                tempLink.link
            } else null
        } catch (e: Exception) {
            android.util.Log.e("DropboxStorageAdapter", "Error fetching album cover url for folder '$folderId': ${e.message}", e)
            null
        }
    }
}
