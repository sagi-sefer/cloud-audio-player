package com.example.cloudaudioplayer.infrastructure.di

import com.example.cloudaudioplayer.domain.repository.CloudStorageRepository
import com.example.cloudaudioplayer.infrastructure.storage.CloudStorageRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StorageModule {

    @Binds
    @Singleton
    abstract fun bindCloudStorageRepository(
        impl: CloudStorageRepositoryImpl
    ): CloudStorageRepository
}
