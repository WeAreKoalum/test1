package com.iptvpro.app.data.db.dao

import androidx.room.*
import com.iptvpro.app.data.db.entity.ChannelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {

    @Query("""
        SELECT * FROM channels
        WHERE groupId = :groupId
        ORDER BY name ASC COLLATE NOCASE
    """)
    fun getChannelsByGroup(groupId: Long): Flow<List<ChannelEntity>>

    @Query("""
        SELECT * FROM channels
        WHERE groupId = :groupId
        ORDER BY name ASC COLLATE NOCASE
    """)
    suspend fun getChannelsByGroupOnce(groupId: Long): List<ChannelEntity>

    @Query("SELECT * FROM channels WHERE id = :channelId")
    suspend fun getChannelById(channelId: Long): ChannelEntity?

    @Query("""
        SELECT * FROM channels
        WHERE playlistId = :playlistId
          AND (normalizedName LIKE '%' || :query || '%'
               OR normalizedName LIKE :query || '%')
        ORDER BY name ASC COLLATE NOCASE
        LIMIT 200
    """)
    suspend fun searchChannels(playlistId: Long, query: String): List<ChannelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(channels: List<ChannelEntity>)

    @Query("DELETE FROM channels WHERE playlistId = :playlistId")
    suspend fun deleteByPlaylist(playlistId: Long)

    @Query("SELECT COUNT(*) FROM channels WHERE playlistId = :playlistId")
    suspend fun countByPlaylist(playlistId: Long): Int

    @Query("""
        SELECT channels.* FROM channels
        INNER JOIN favorites ON favorites.channelId = channels.id
            AND favorites.playlistId = channels.playlistId
        WHERE channels.playlistId = :playlistId
        ORDER BY favorites.addedAt ASC
    """)
    fun getFavoriteChannels(playlistId: Long): Flow<List<ChannelEntity>>

    @Query("""
        SELECT channels.* FROM channels
        INNER JOIN history ON history.channelId = channels.id
            AND history.playlistId = channels.playlistId
        WHERE channels.playlistId = :playlistId
        ORDER BY history.watchedAt DESC
        LIMIT 20
    """)
    fun getHistoryChannels(playlistId: Long): Flow<List<ChannelEntity>>
}
