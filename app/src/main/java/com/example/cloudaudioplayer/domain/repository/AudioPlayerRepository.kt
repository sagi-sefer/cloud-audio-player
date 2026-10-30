package com.example.cloudaudioplayer.domain.repository

import com.example.cloudaudioplayer.domain.model.MediaItem
import com.example.cloudaudioplayer.domain.model.PlaybackState
import kotlinx.coroutines.flow.StateFlow

interface AudioPlayerRepository {
    val playbackState: StateFlow<PlaybackState>

    fun play(
        track: MediaItem.AudioTrack,
        streamUrl: String,
        headers: Map<String, String> = emptyMap()
    )
    fun playPlaylist(
        tracks: List<MediaItem.AudioTrack>,
        initialIndex: Int,
        streamUrlResolver: suspend (MediaItem.AudioTrack) -> Pair<String, Map<String, String>>
    )
    fun pause()
    fun resume()
    fun seekTo(positionMs: Long)
    fun skipToNext()
    fun skipToPrevious()
    fun stop()
}
