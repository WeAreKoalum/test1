package com.iptvpro.app.data.db.dao

import androidx.room.*
import com.iptvpro.app.data.db.entity.PlaylistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY name ASC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: Long): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(playlist: PlaylistEntity): Long

    @Update
    suspend fun update(playlist: PlaylistEntity)

    @Delete
    suspend fun delete(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("""
        UPDATE playlists
        SET channelCount = :count, lastSync = :syncTime, syncError = NULL
        WHERE id = :id
    """)
    suspend fun updateSyncSuccess(id: Long, count: Int, syncTime: Long)

    @Query("UPDATE playlists SET syncError = :error WHERE id = :id")
    suspend fun updateSyncError(id: Long, error: String)

    @Query("UPDATE playlists SET epgUrl = :epgUrl WHERE id = :id")
    suspend fun updateEpgUrl(id: Long, epgUrl: String?)

    @Query("""
        UPDATE playlists
        SET lastEpgSync = :syncTime, epgSyncError = NULL
        WHERE id = :id
    """)
    suspend fun updateEpgSyncSuccess(id: Long, syncTime: Long)

    @Query("UPDATE playlists SET epgSyncError = :error WHERE id = :id")
    suspend fun updateEpgSyncError(id: Long, error: String)

    @Query("""
        UPDATE playlists
        SET autoDetectedEpgUrl = :epgUrl
        WHERE id = :id AND epgUrl IS NULL
    """)
    suspend fun setAutoDetectedEpgUrl(id: Long, epgUrl: String)
}
