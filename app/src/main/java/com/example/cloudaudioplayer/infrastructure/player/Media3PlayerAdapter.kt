package com.example.cloudaudioplayer.infrastructure.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem as Media3MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.example.cloudaudioplayer.domain.model.MediaItem
import com.example.cloudaudioplayer.domain.model.PlaybackState
import com.example.cloudaudioplayer.domain.repository.AudioPlayerRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Media3PlayerAdapter @Inject constructor(
    @ApplicationContext private val context: Context
) : AudioPlayerRepository {

    private val _playbackState = MutableStateFlow(PlaybackState())
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var player: ExoPlayer? = null
    private var currentPlaylist: List<MediaItem.AudioTrack> = emptyList()
    private var streamUrlResolver: (suspend (MediaItem.AudioTrack) -> Pair<String, Map<String, String>>)? = null

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressUpdateJob: Job? = null

    init {
        initPlayer()
    }

    private fun initPlayer() {
        if (player != null) return

        val exoPlayer = ExoPlayer.Builder(context).build()
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) startProgressTracker() else stopProgressTracker()
            }

            override fun onPlaybackStateChanged(state: Int) {
                val isBuffering = state == Player.STATE_BUFFERING
                val duration = if (exoPlayer.duration > 0) exoPlayer.duration else 0L
                _playbackState.update {
                    it.copy(
                        isBuffering = isBuffering,
                        durationMs = duration
                    )
                }
                if (state == Player.STATE_ENDED) {
                    val currentTrack = _playbackState.value.currentTrack
                    if (currentTrack != null) {
                        val currentIndex = currentPlaylist.indexOfFirst { it.id == currentTrack.id }
                        if (currentIndex != -1 && currentIndex < currentPlaylist.lastIndex) {
                            skipToNext()
                        } else {
                            exoPlayer.pause()
                            exoPlayer.seekTo(0)
                            _playbackState.update { it.copy(isPlaying = false, currentPositionMs = 0L) }
                        }
                    }
                }
            }

            override fun onMediaItemTransition(mediaItem: Media3MediaItem?, reason: Int) {
                val mediaId = mediaItem?.mediaId
                val track = currentPlaylist.firstOrNull { it.id == mediaId }
                if (track != null) {
                    _playbackState.update { it.copy(currentTrack = track) }
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                _playbackState.update {
                    it.copy(
                        isPlaying = false,
                        isBuffering = false,
                        errorMessage = error.localizedMessage ?: "Playback error"
                    )
                }
            }
        })
        player = exoPlayer
    }

    @OptIn(UnstableApi::class)
    override fun play(
        track: MediaItem.AudioTrack,
        streamUrl: String,
        headers: Map<String, String>
    ) {
        initPlayer()
        val exo = player ?: return

        if (currentPlaylist.none { it.id == track.id }) {
            currentPlaylist = listOf(track)
        }

        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setArtworkUri(track.coverArtUrl?.let { Uri.parse(it) })
            .build()

        val media3Item = Media3MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(Uri.parse(streamUrl))
            .setMediaMetadata(metadata)
            .build()

        if (headers.isNotEmpty()) {
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setDefaultRequestProperties(headers)
                .setAllowCrossProtocolRedirects(true)

            val mediaSourceFactory = ProgressiveMediaSource.Factory(httpDataSourceFactory)
            val mediaSource = mediaSourceFactory.createMediaSource(media3Item)

            exo.setMediaSource(mediaSource)
        } else {
            exo.setMediaItem(media3Item)
        }

        exo.prepare()
        exo.playWhenReady = true

        _playbackState.update {
            it.copy(
                currentTrack = track,
                isPlaying = true,
                errorMessage = null
            )
        }
    }

    override fun playPlaylist(
        tracks: List<MediaItem.AudioTrack>,
        initialIndex: Int,
        streamUrlResolver: suspend (MediaItem.AudioTrack) -> Pair<String, Map<String, String>>
    ) {
        if (tracks.isEmpty()) return
        currentPlaylist = tracks
        this.streamUrlResolver = streamUrlResolver

        val targetIndex = initialIndex.coerceIn(0, tracks.lastIndex)
        val targetTrack = tracks[targetIndex]

        scope.launch {
            val (url, headers) = streamUrlResolver(targetTrack)
            if (url.isNotEmpty()) {
                play(targetTrack, url, headers)
            }
        }
    }

    override fun pause() {
        player?.pause()
    }

    override fun resume() {
        player?.play()
    }

    override fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs)
        _playbackState.update { it.copy(currentPositionMs = positionMs) }
    }

    override fun skipToNext() {
        val currentTrack = _playbackState.value.currentTrack ?: return
        val currentIndex = currentPlaylist.indexOfFirst { it.id == currentTrack.id }
        if (currentIndex != -1 && currentIndex < currentPlaylist.lastIndex) {
            val nextTrack = currentPlaylist[currentIndex + 1]
            val resolver = streamUrlResolver
            if (resolver != null) {
                scope.launch {
                    val (url, headers) = resolver(nextTrack)
                    play(nextTrack, url, headers)
                }
            }
        }
    }

    override fun skipToPrevious() {
        val currentTrack = _playbackState.value.currentTrack ?: return
        val currentIndex = currentPlaylist.indexOfFirst { it.id == currentTrack.id }
        if (currentIndex > 0) {
            val prevTrack = currentPlaylist[currentIndex - 1]
            val resolver = streamUrlResolver
            if (resolver != null) {
                scope.launch {
                    val (url, headers) = resolver(prevTrack)
                    play(prevTrack, url, headers)
                }
            }
        }
    }

    override fun stop() {
        stopProgressTracker()
        player?.stop()
        _playbackState.update { PlaybackState() }
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressUpdateJob = scope.launch {
            while (true) {
                player?.let { p ->
                    val pos = p.currentPosition.coerceAtLeast(0L)
                    val dur = if (p.duration > 0) p.duration else 0L
                    _playbackState.update {
                        it.copy(currentPositionMs = pos, durationMs = dur)
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressUpdateJob?.cancel()
        progressUpdateJob = null
    }
}
