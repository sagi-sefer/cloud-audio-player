package com.example.cloudaudioplayer.infrastructure.player

import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AudioPlayerService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)

        val mediaSourceFactory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(httpDataSourceFactory)

        val exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        player = exoPlayer

        mediaSession = MediaSession.Builder(this, exoPlayer).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        player?.let { p ->
            if (!p.playWhenReady || p.mediaItemCount == 0 || p.playbackState == Player.STATE_ENDED) {
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        player = null
        super.onDestroy()
    }
}
