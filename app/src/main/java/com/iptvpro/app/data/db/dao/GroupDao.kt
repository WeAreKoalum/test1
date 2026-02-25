package com.iptvpro.app.data.db.dao

import androidx.room.*
import com.iptvpro.app.data.db.entity.GroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {

    @Query("SELECT * FROM groups WHERE playlistId = :playlistId ORDER BY name ASC")
    fun getGroupsByPlaylist(playlistId: Long): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE playlistId = :playlistId ORDER BY name ASC")
    suspend fun getGroupsByPlaylistOnce(playlistId: Long): List<GroupEntity>

    @Query("SELECT * FROM groups WHERE id = :groupId")
    suspend fun getGroupById(groupId: Long): GroupEntity?

    @Query("SELECT id FROM groups WHERE playlistId = :playlistId AND name = :name")
    suspend fun getGroupIdByName(playlistId: Long, name: String): Long?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(groups: List<GroupEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(group: GroupEntity): Long

    @Query("DELETE FROM groups WHERE playlistId = :playlistId")
    suspend fun deleteByPlaylist(playlistId: Long)

    @Query("""
        UPDATE groups SET channelCount = (
            SELECT COUNT(*) FROM channels WHERE channels.groupId = groups.id
        ) WHERE playlistId = :playlistId
    """)
    suspend fun refreshChannelCounts(playlistId: Long)
}
