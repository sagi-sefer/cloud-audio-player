package com.example.cloudaudioplayer.infrastructure.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [MediaItemEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun audioCacheDao(): AudioCacheDao
}
