package com.example.cloudaudioplayer.infrastructure.di

import com.example.cloudaudioplayer.domain.repository.AudioPlayerRepository
import com.example.cloudaudioplayer.infrastructure.player.Media3PlayerAdapter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerModule {

    @Binds
    @Singleton
    abstract fun bindAudioPlayerRepository(
        impl: Media3PlayerAdapter
    ): AudioPlayerRepository
}
