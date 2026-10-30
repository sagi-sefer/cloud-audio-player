package com.example.cloudaudioplayer.application

import com.example.cloudaudioplayer.domain.model.MediaItem
import com.example.cloudaudioplayer.domain.model.StorageType
import com.example.cloudaudioplayer.domain.repository.AudioPlayerRepository
import com.example.cloudaudioplayer.domain.repository.CloudStorageRepository
import javax.inject.Inject

class StreamAudioUseCase @Inject constructor(
    private val cloudStorageRepository: CloudStorageRepository,
    private val audioPlayerRepository: AudioPlayerRepository
) {
    operator fun invoke(
        track: MediaItem.AudioTrack,
        playlist: List<MediaItem.AudioTrack>,
        authData: String
    ) {
        val initialIndex = playlist.indexOfFirst { it.id == track.id }.coerceAtLeast(0)

        audioPlayerRepository.playPlaylist(
            tracks = playlist,
            initialIndex = initialIndex,
            streamUrlResolver = { targetTrack ->
                val streamUrl = cloudStorageRepository.getStreamUrl(targetTrack, authData)
                val headers = if (targetTrack.storageType == StorageType.GOOGLE_DRIVE) {
                    mapOf("Authorization" to "Bearer $authData")
                } else {
                    emptyMap()
                }
                Pair(streamUrl, headers)
            }
        )
    }
}
