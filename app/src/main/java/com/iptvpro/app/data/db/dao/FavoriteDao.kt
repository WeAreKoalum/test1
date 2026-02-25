package com.iptvpro.app.data.db.dao

import androidx.room.*
import com.iptvpro.app.data.db.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT channelId FROM favorites WHERE playlistId = :playlistId")
    fun getFavoriteIds(playlistId: Long): Flow<Set<Long>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE playlistId = :playlistId AND channelId = :channelId)")
    suspend fun isFavorite(playlistId: Long, channelId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE playlistId = :playlistId AND channelId = :channelId")
    suspend fun removeFavorite(playlistId: Long, channelId: Long)

    @Query("DELETE FROM favorites WHERE playlistId = :playlistId")
    suspend fun clearFavorites(playlistId: Long)
}
