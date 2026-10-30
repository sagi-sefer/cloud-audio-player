package com.example.cloudaudioplayer.domain.model

data class PlaybackState(
    val currentTrack: MediaItem.AudioTrack? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isBuffering: Boolean = false,
    val errorMessage: String? = null
)
