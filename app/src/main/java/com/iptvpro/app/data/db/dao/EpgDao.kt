package com.iptvpro.app.data.db.dao

import androidx.room.*
import com.iptvpro.app.data.db.entity.EpgChannelEntity
import com.iptvpro.app.data.db.entity.EpgProgramEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EpgDao {

    // ---- Canales EPG ----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<EpgChannelEntity>)

    @Query("SELECT * FROM epg_channels WHERE playlistId = :playlistId AND epgId = :epgId")
    suspend fun getChannelByEpgId(playlistId: Long, epgId: String): EpgChannelEntity?

    @Query("""
        SELECT * FROM epg_channels
        WHERE playlistId = :playlistId
          AND normalizedDisplayName LIKE :normalizedName || '%'
        LIMIT 1
    """)
    suspend fun getChannelByNormalizedName(playlistId: Long, normalizedName: String): EpgChannelEntity?

    @Query("DELETE FROM epg_channels WHERE playlistId = :playlistId")
    suspend fun deleteChannelsByPlaylist(playlistId: Long)

    // ---- Programas EPG ----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrograms(programs: List<EpgProgramEntity>)

    /**
     * Programa actual: startTime <= now < endTime
     */
    @Query("""
        SELECT * FROM epg_programs
        WHERE epgChannelId = :epgChannelId
          AND startTime <= :nowMillis
          AND endTime > :nowMillis
        LIMIT 1
    """)
    suspend fun getCurrentProgram(epgChannelId: Long, nowMillis: Long): EpgProgramEntity?

    /**
     * Siguiente programa: startTime > now, el más próximo
     */
    @Query("""
        SELECT * FROM epg_programs
        WHERE epgChannelId = :epgChannelId
          AND startTime > :nowMillis
        ORDER BY startTime ASC
        LIMIT 1
    """)
    suspend fun getNextProgram(epgChannelId: Long, nowMillis: Long): EpgProgramEntity?

    /**
     * Próximas emisiones para una vista de guía básica
     */
    @Query("""
        SELECT * FROM epg_programs
        WHERE epgChannelId = :epgChannelId
          AND endTime > :nowMillis
        ORDER BY startTime ASC
        LIMIT :limit
    """)
    fun getUpcomingPrograms(epgChannelId: Long, nowMillis: Long, limit: Int = 10): Flow<List<EpgProgramEntity>>

    @Query("DELETE FROM epg_programs WHERE epgChannelId IN (SELECT id FROM epg_channels WHERE playlistId = :playlistId)")
    suspend fun deleteProgramsByPlaylist(playlistId: Long)

    @Query("DELETE FROM epg_programs WHERE endTime < :beforeMillis")
    suspend fun deletePastPrograms(beforeMillis: Long)
}
