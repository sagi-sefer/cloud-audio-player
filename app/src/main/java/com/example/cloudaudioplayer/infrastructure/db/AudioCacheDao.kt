package com.example.cloudaudioplayer.infrastructure.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AudioCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<MediaItemEntity>)

    @Query("SELECT * FROM media_items WHERE parentFolderId = :parentFolderId")
    suspend fun getItemsByParent(parentFolderId: String): List<MediaItemEntity>

    @Query("SELECT * FROM media_items WHERE parentFolderId = 'root' AND isFolder = 1")
    suspend fun getAllArtists(): List<MediaItemEntity>

    @Query("SELECT * FROM media_items WHERE parentFolderId = :artistFolderId AND isFolder = 1")
    suspend fun getAlbumsForArtist(artistFolderId: String): List<MediaItemEntity>

    @Query("SELECT * FROM media_items WHERE isFolder = 1 AND parentFolderId != 'root' ORDER BY name ASC")
    suspend fun getAllAlbums(): List<MediaItemEntity>

    @Query("SELECT * FROM media_items WHERE parentFolderId = :albumFolderId AND isFolder = 0")
    suspend fun getTracksForAlbum(albumFolderId: String): List<MediaItemEntity>

    @Query("DELETE FROM media_items")
    suspend fun clearAll()
}
