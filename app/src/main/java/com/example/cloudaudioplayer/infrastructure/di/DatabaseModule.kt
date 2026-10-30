package com.example.cloudaudioplayer.infrastructure.di

import android.content.Context
import androidx.room.Room
import com.example.cloudaudioplayer.domain.repository.MetadataRepository
import com.example.cloudaudioplayer.infrastructure.db.AppDatabase
import com.example.cloudaudioplayer.infrastructure.db.AudioCacheDao
import com.example.cloudaudioplayer.infrastructure.db.RoomAudioCacheAdapter
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {

    @Binds
    @Singleton
    abstract fun bindMetadataRepository(
        impl: RoomAudioCacheAdapter
    ): MetadataRepository

    companion object {
        @Provides
        @Singleton
        fun provideAppDatabase(
            @ApplicationContext context: Context
        ): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "cloud_audio_player.db"
            ).fallbackToDestructiveMigration().build()
        }

        @Provides
        @Singleton
        fun provideAudioCacheDao(
            database: AppDatabase
        ): AudioCacheDao {
            return database.audioCacheDao()
        }
    }
}
