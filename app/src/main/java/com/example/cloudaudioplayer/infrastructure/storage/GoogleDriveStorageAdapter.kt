package com.example.cloudaudioplayer.infrastructure.storage

import com.example.cloudaudioplayer.domain.model.AudioFormat
import com.example.cloudaudioplayer.domain.model.MediaItem
import com.example.cloudaudioplayer.domain.model.StorageType
import com.example.cloudaudioplayer.domain.repository.CloudStorageRepository
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleDriveStorageAdapter @Inject constructor() : CloudStorageRepository {

    private fun getDriveService(authData: String): Drive {
        if (authData.isBlank()) {
            throw IllegalArgumentException("OAuth token is empty. Please enter your Google Drive token in Settings.")
        }
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance()
        ) { request ->
            request.headers.authorization = "Bearer $authData"
        }.setApplicationName("CloudAudioPlayer").build()
    }

    override suspend fun getRootFolders(
        storageType: StorageType,
        authData: String,
        rootFolder: String
    ): List<MediaItem.Folder> = withContext(Dispatchers.IO) {
        val service = getDriveService(authData)
        val parentId = if (rootFolder.isNotBlank() && rootFolder != "root" && rootFolder != "Cloud Root") {
            val searchQuery = "name = '$rootFolder' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
            val searchResult = service.files().list()
                .setQ(searchQuery)
                .setFields("files(id, name)")
                .execute()
            searchResult.files?.firstOrNull()?.id ?: throw IllegalArgumentException("Folder '$rootFolder' not found in Google Drive. Please check folder name or use Browse.")
        } else {
            "root"
        }

        val query = "'$parentId' in parents and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
        val result = service.files().list()
            .setQ(query)
            .setOrderBy("name")
            .setFields("files(id, name)")
            .execute()

        result.files?.map { file ->
            MediaItem.Folder(
                id = file.id,
                name = file.name ?: "Unknown Artist",
                path = "/${file.name}",
                parentId = parentId,
                storageType = StorageType.GOOGLE_DRIVE
            )
        }?.sortedBy { it.name } ?: emptyList()
    }

    override suspend fun getFolderContents(
        folderId: String,
        storageType: StorageType,
        authData: String
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        val service = getDriveService(authData)
        val query = "'$folderId' in parents and trashed = false"
        val result = service.files().list()
            .setQ(query)
            .setOrderBy("name")
            .setFields("files(id, name, mimeType, size)")
            .execute()

        val items = mutableListOf<MediaItem>()
        result.files?.forEach { file ->
            val name = file.name ?: "Unknown"
            if (file.mimeType == "application/vnd.google-apps.folder") {
                items.add(
                    MediaItem.Folder(
                        id = file.id,
                        name = name,
                        path = "/$name",
                        parentId = folderId,
                        storageType = StorageType.GOOGLE_DRIVE
                    )
                )
            } else {
                val format = AudioFormat.fromExtension(name)
                if (format != AudioFormat.UNKNOWN) {
                    items.add(
                        MediaItem.AudioTrack(
                            id = file.id,
                            title = name.substringBeforeLast('.'),
                            artist = "Artist",
                            album = "Album",
                            path = "/$name",
                            durationMs = 0L,
                            format = format,
                            coverArtUrl = null,
                            parentId = folderId,
                            storageType = StorageType.GOOGLE_DRIVE
                        )
                    )
                }
            }
        }
        items.sortedBy { it.name }
    }

    override suspend fun getStreamUrl(
        track: MediaItem.AudioTrack,
        authData: String
    ): String {
        return "https://www.googleapis.com/drive/v3/files/${track.id}?alt=media"
    }

    override suspend fun fetchAlbumCoverUrl(
        folderId: String,
        storageType: StorageType,
        authData: String
    ): String? = withContext(Dispatchers.IO) {
        if (authData.isBlank()) return@withContext null
        try {
            val service = getDriveService(authData)
            val query = "'$folderId' in parents and trashed = false"
            val result = service.files().list()
                .setQ(query)
                .setPageSize(1000)
                .setFields("files(id, name, mimeType)")
                .execute()

            val files = result.files ?: emptyList()

            // 1. First look for standard cover art file names (case-insensitive)
            val coverFile = files.firstOrNull { file ->
                val name = file.name?.lowercase() ?: ""
                name == "cover.jpg" || name == "cover.jpeg" || name == "cover.png" || name == "cover.webp" ||
                name == "folder.jpg" || name == "folder.jpeg" || name == "folder.png" || name == "folder.webp" ||
                name == "album.jpg" || name == "album.jpeg" || name == "album.png" || name == "album.webp" ||
                name == "front.jpg" || name == "front.jpeg" || name == "front.png" ||
                name == "art.jpg" || name == "art.png" ||
                name.startsWith("cover") || name.startsWith("folder") || name.startsWith("album")
            } ?: files.firstOrNull { file ->
                // 2. Fallback to any image file in the album folder
                val name = file.name?.lowercase() ?: ""
                val mime = file.mimeType?.lowercase() ?: ""
                mime.startsWith("image/") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp")
            }

            if (coverFile != null) {
                val encodedToken = java.net.URLEncoder.encode(authData, "UTF-8")
                "https://www.googleapis.com/drive/v3/files/${coverFile.id}?alt=media&access_token=$encodedToken"
            } else null
        } catch (e: Exception) {
            android.util.Log.e("GoogleDriveAdapter", "Error fetching album cover url for folder '$folderId': ${e.message}", e)
            null
        }
    }
}
