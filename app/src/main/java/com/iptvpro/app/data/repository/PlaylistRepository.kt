package com.iptvpro.app.data.repository

import com.iptvpro.app.data.db.dao.ChannelDao
import com.iptvpro.app.data.db.dao.GroupDao
import com.iptvpro.app.data.db.dao.PlaylistDao
import com.iptvpro.app.data.db.entity.ChannelEntity
import com.iptvpro.app.data.db.entity.GroupEntity
import com.iptvpro.app.data.db.entity.PlaylistEntity
import com.iptvpro.app.data.parser.M3uParser
import com.iptvpro.app.data.remote.RemoteDataSource
import com.iptvpro.app.data.util.normalize
import com.iptvpro.app.domain.model.Playlist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val groupDao: GroupDao,
    private val channelDao: ChannelDao
) {

    fun getAllPlaylists(): Flow<List<Playlist>> =
        playlistDao.getAllPlaylists().map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getPlaylistById(id: Long): Playlist? =
        playlistDao.getPlaylistById(id)?.toDomain()

    suspend fun addPlaylist(name: String, url: String, epgUrl: String?): Long {
        val entity = PlaylistEntity(name = name, url = url, epgUrl = epgUrl?.takeIf { it.isNotBlank() })
        return playlistDao.insert(entity)
    }

    suspend fun updatePlaylist(id: Long, name: String, url: String, epgUrl: String?) {
        val existing = playlistDao.getPlaylistById(id) ?: return
        playlistDao.update(
            existing.copy(
                name = name,
                url = url,
                epgUrl = epgUrl?.takeIf { it.isNotBlank() }
            )
        )
    }

    suspend fun deletePlaylist(id: Long) {
        playlistDao.deleteById(id)
    }

    suspend fun updateEpgUrl(playlistId: Long, epgUrl: String?) {
        playlistDao.updateEpgUrl(playlistId, epgUrl)
    }

    /**
     * Descarga y parsea el M3U, luego guarda grupos y canales en Room.
     * Elimina los datos anteriores de la lista antes de insertar.
     */
    suspend fun syncPlaylist(playlistId: Long): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val playlist = playlistDao.getPlaylistById(playlistId)
                ?: return@withContext Result.failure(Exception("Lista no encontrada"))

            val parsed = RemoteDataSource.fetch(playlist.url) { stream ->
                M3uParser.parse(stream)
            }

            // Auto-detectar URL EPG si la tiene el M3U y no hay una manual
            if (!parsed.epgUrl.isNullOrBlank()) {
                playlistDao.setAutoDetectedEpgUrl(playlistId, parsed.epgUrl)
            }

            // Limpiar datos anteriores
            groupDao.deleteByPlaylist(playlistId)
            channelDao.deleteByPlaylist(playlistId)

            // Agrupar canales por grupo
            val groupNames = parsed.channels.map { it.groupTitle }.distinct().sorted()

            // Insertar grupos
            val groupEntities = groupNames.map { name ->
                GroupEntity(playlistId = playlistId, name = name)
            }
            val groupIds = groupDao.insertAll(groupEntities)
            val groupNameToId = groupNames.zip(groupIds).toMap()

            // Insertar canales
            val channelEntities = parsed.channels.mapNotNull { ch ->
                val groupId = groupNameToId[ch.groupTitle] ?: return@mapNotNull null
                ChannelEntity(
                    playlistId = playlistId,
                    groupId = groupId,
                    name = ch.name,
                    url = ch.url,
                    logoUrl = ch.logoUrl,
                    tvgId = ch.tvgId,
                    tvgName = ch.tvgName,
                    normalizedName = ch.name.normalize()
                )
            }
            channelDao.insertAll(channelEntities)
            groupDao.refreshChannelCounts(playlistId)

            val count = channelEntities.size
            playlistDao.updateSyncSuccess(playlistId, count, System.currentTimeMillis())
            Result.success(count)

        } catch (e: Exception) {
            val msg = e.message ?: "Error desconocido"
            playlistDao.updateSyncError(playlistId, msg)
            Result.failure(e)
        }
    }

    private fun PlaylistEntity.toDomain() = Playlist(
        id = id,
        name = name,
        url = url,
        epgUrl = effectiveEpgUrl(),
        lastSync = lastSync,
        lastEpgSync = lastEpgSync,
        channelCount = channelCount,
        syncError = syncError,
        epgSyncError = epgSyncError
    )
}
