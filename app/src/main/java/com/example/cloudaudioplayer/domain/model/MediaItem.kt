package com.example.cloudaudioplayer.domain.model

sealed class MediaItem {
    abstract val id: String
    abstract val name: String
    abstract val path: String
    abstract val parentId: String?
    abstract val coverArtUrl: String?

    data class Folder(
        override val id: String,
        override val name: String,
        override val path: String,
        override val parentId: String? = null,
        override val coverArtUrl: String? = null
    ) : MediaItem()

    data class AudioTrack(
        override val id: String,
        val title: String,
        val artist: String,
        val album: String,
        override val path: String,
        val durationMs: Long = 0L,
        val format: AudioFormat,
        override val coverArtUrl: String? = null,
        override val parentId: String? = null,
        val storageType: StorageType
    ) : MediaItem() {
        override val name: String get() = title
    }
}
