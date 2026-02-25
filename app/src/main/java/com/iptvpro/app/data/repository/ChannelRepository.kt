package com.iptvpro.app.data.repository

import com.iptvpro.app.data.db.dao.ChannelDao
import com.iptvpro.app.data.db.dao.FavoriteDao
import com.iptvpro.app.data.db.dao.GroupDao
import com.iptvpro.app.data.db.dao.HistoryDao
import com.iptvpro.app.data.db.entity.FavoriteEntity
import com.iptvpro.app.data.db.entity.HistoryEntity
import com.iptvpro.app.data.util.normalize
import com.iptvpro.app.domain.model.Channel
import com.iptvpro.app.domain.model.ChannelGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelRepository @Inject constructor(
    private val channelDao: ChannelDao,
    private val groupDao: GroupDao,
    private val favoriteDao: FavoriteDao,
    private val historyDao: HistoryDao
) {

    fun getGroups(playlistId: Long): Flow<List<ChannelGroup>> =
        groupDao.getGroupsByPlaylist(playlistId).map { list ->
            list.map { ChannelGroup(it.id, it.playlistId, it.name, it.channelCount) }
        }

    fun getChannelsByGroup(groupId: Long): Flow<List<Channel>> =
        channelDao.getChannelsByGroup(groupId).map { list -> list.map { it.toDomain() } }

    fun getFavoriteChannels(playlistId: Long): Flow<List<Channel>> =
        channelDao.getFavoriteChannels(playlistId).map { list -> list.map { it.toDomain() } }

    fun getHistoryChannels(playlistId: Long): Flow<List<Channel>> =
        channelDao.getHistoryChannels(playlistId).map { list -> list.map { it.toDomain() } }

    fun getFavoriteIds(playlistId: Long): Flow<Set<Long>> =
        favoriteDao.getFavoriteIds(playlistId)

    suspend fun searchChannels(playlistId: Long, query: String): List<Channel> {
        if (query.isBlank()) return emptyList()
        val normalized = query.normalize()
        return channelDao.searchChannels(playlistId, normalized).map { it.toDomain() }
    }

    suspend fun getChannelById(id: Long): Channel? =
        channelDao.getChannelById(id)?.toDomain()

    // ---- Favoritos ----

    suspend fun addFavorite(playlistId: Long, channelId: Long) {
        favoriteDao.addFavorite(FavoriteEntity(playlistId = playlistId, channelId = channelId))
    }

    suspend fun removeFavorite(playlistId: Long, channelId: Long) {
        favoriteDao.removeFavorite(playlistId, channelId)
    }

    suspend fun toggleFavorite(playlistId: Long, channelId: Long): Boolean {
        return if (favoriteDao.isFavorite(playlistId, channelId)) {
            favoriteDao.removeFavorite(playlistId, channelId)
            false
        } else {
            favoriteDao.addFavorite(FavoriteEntity(playlistId, channelId))
            true
        }
    }

    // ---- Historial ----

    /**
     * Registra un canal en el historial de la lista.
     * Si ya existía, actualiza el timestamp (sin duplicados).
     * Limita a 20 entradas.
     */
    suspend fun recordHistory(playlistId: Long, channelId: Long) {
        historyDao.upsert(HistoryEntity(playlistId = playlistId, channelId = channelId))
        historyDao.trimHistory(playlistId)
    }

    private fun com.iptvpro.app.data.db.entity.ChannelEntity.toDomain() = Channel(
        id = id,
        playlistId = playlistId,
        groupId = groupId,
        name = name,
        url = url,
        logoUrl = logoUrl,
        tvgId = tvgId,
        tvgName = tvgName
    )
}
