package com.example.cloudaudioplayer.infrastructure.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.cloudaudioplayer.domain.model.AudioFormat
import com.example.cloudaudioplayer.domain.model.MediaItem
import com.example.cloudaudioplayer.domain.model.StorageType

@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey val id: String,
    val parentFolderId: String,
    val name: String,
    val path: String,
    val isFolder: Boolean,
    val artist: String = "",
    val album: String = "",
    val title: String = "",
    val durationMs: Long = 0L,
    val format: String = "",
    val coverArtUrl: String? = null,
    val storageType: String = StorageType.GOOGLE_DRIVE.name
) {
    fun toDomain(): MediaItem {
        return if (isFolder) {
            MediaItem.Folder(
                id = id,
                name = name,
                path = path,
                parentId = parentFolderId,
                coverArtUrl = coverArtUrl,
                storageType = runCatching { StorageType.valueOf(storageType) }.getOrDefault(StorageType.GOOGLE_DRIVE)
            )
        } else {
            MediaItem.AudioTrack(
                id = id,
                title = title.ifEmpty { name },
                artist = artist,
                album = album,
                path = path,
                durationMs = durationMs,
                format = AudioFormat.fromExtension(format),
                coverArtUrl = coverArtUrl,
                parentId = parentFolderId,
                storageType = runCatching { StorageType.valueOf(storageType) }.getOrDefault(StorageType.GOOGLE_DRIVE)
            )
        }
    }

    companion object {
        fun fromDomain(item: MediaItem, parentFolderId: String): MediaItemEntity {
            return when (item) {
                is MediaItem.Folder -> MediaItemEntity(
                    id = item.id,
                    parentFolderId = parentFolderId,
                    name = item.name,
                    path = item.path,
                    isFolder = true,
                    coverArtUrl = item.coverArtUrl,
                    storageType = item.storageType.name
                )
                is MediaItem.AudioTrack -> MediaItemEntity(
                    id = item.id,
                    parentFolderId = parentFolderId,
                    name = item.name,
                    path = item.path,
                    isFolder = false,
                    artist = item.artist,
                    album = item.album,
                    title = item.title,
                    durationMs = item.durationMs,
                    format = item.format.extension,
                    coverArtUrl = item.coverArtUrl,
                    storageType = item.storageType.name
                )
            }
        }
    }
}
