package com.iptvpro.app.data.db.dao

import androidx.room.*
import com.iptvpro.app.data.db.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("""
        SELECT * FROM history
        WHERE playlistId = :playlistId
        ORDER BY watchedAt DESC
        LIMIT 20
    """)
    fun getHistory(playlistId: Long): Flow<List<HistoryEntity>>

    @Query("SELECT COUNT(*) FROM history WHERE playlistId = :playlistId")
    suspend fun countHistory(playlistId: Long): Int

    /** Inserta o actualiza el timestamp (si ya existe, el REPLACE actualiza watchedAt) */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(history: HistoryEntity)

    /**
     * Después de insertar, si hay más de 20 entradas, elimina las más antiguas.
     * Se llama tras upsert.
     */
    @Query("""
        DELETE FROM history
        WHERE playlistId = :playlistId
          AND channelId NOT IN (
              SELECT channelId FROM history
              WHERE playlistId = :playlistId
              ORDER BY watchedAt DESC
              LIMIT 20
          )
    """)
    suspend fun trimHistory(playlistId: Long)

    @Query("DELETE FROM history WHERE playlistId = :playlistId")
    suspend fun clearHistory(playlistId: Long)
}
