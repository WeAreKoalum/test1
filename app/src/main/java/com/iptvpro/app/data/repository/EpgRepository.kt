package com.iptvpro.app.data.repository

import com.iptvpro.app.data.db.dao.EpgDao
import com.iptvpro.app.data.db.dao.PlaylistDao
import com.iptvpro.app.data.db.entity.EpgChannelEntity
import com.iptvpro.app.data.db.entity.EpgProgramEntity
import com.iptvpro.app.data.parser.XmltvParser
import com.iptvpro.app.data.remote.RemoteDataSource
import com.iptvpro.app.data.util.normalize
import com.iptvpro.app.domain.model.EpgProgram
import com.iptvpro.app.domain.model.NowNextInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpgRepository @Inject constructor(
    private val epgDao: EpgDao,
    private val playlistDao: PlaylistDao
) {

    /**
     * Sincroniza la EPG de una lista.
     * Usa la URL EPG efectiva (manual > auto-detectada).
     * Si no hay URL EPG, devuelve éxito sin hacer nada.
     */
    suspend fun syncEpg(playlistId: Long): Result<Int> = withContext(Dispatchers.IO) {
        val playlist = playlistDao.getPlaylistById(playlistId)
            ?: return@withContext Result.failure(Exception("Lista no encontrada"))

        val epgUrl = playlist.effectiveEpgUrl()
        if (epgUrl.isNullOrBlank()) {
            return@withContext Result.success(0) // Sin URL EPG configurada
        }

        try {
            // Limpiar EPG anterior de esta lista
            epgDao.deleteChannelsByPlaylist(playlistId)

            var channelCount = 0
            var programCount = 0

            // Batches para inserción eficiente
            val channelBatch = mutableListOf<EpgChannelEntity>()
            val programBatch = mutableListOf<EpgProgramEntity>()
            val channelIdMap = mutableMapOf<String, Long>() // epgId -> DB id

            RemoteDataSource.fetch(epgUrl) { stream ->
                XmltvParser.parse(
                    inputStream = stream,
                    onChannel = { parsed ->
                        channelBatch.add(
                            EpgChannelEntity(
                                playlistId = playlistId,
                                epgId = parsed.id,
                                displayName = parsed.displayName,
                                normalizedDisplayName = parsed.displayName.normalize(),
                                iconUrl = parsed.iconUrl
                            )
                        )
                        if (channelBatch.size >= 500) {
                            // Insertar en lote
                            // No se puede llamar suspend desde lambda no-suspend; acumular y procesar después
                        }
                        channelCount++
                    },
                    onProgram = { parsed ->
                        programBatch.add(
                            EpgProgramEntity(
                                epgChannelId = 0L, // se resolverá después
                                startTime = parsed.startTime,
                                endTime = parsed.endTime,
                                title = parsed.title,
                                description = parsed.description,
                                // Guardamos el channelId como negativo para resolverlo
                            ).let { it } // placeholder, ver estrategia abajo
                        )
                        programCount++
                    }
                )
            }

            // Estrategia: dado que XmlPullParser no es suspend-friendly para inserción incremental,
            // acumulamos todo y luego insertamos. Para XMLTV muy grandes (>500k programas)
            // se recomendaría un worker separado con chunking. En v1 esta estrategia es suficiente.

            // Re-parsear con estrategia de dos pasadas para resolver IDs
            epgDao.deleteChannelsByPlaylist(playlistId) // limpiar parciales

            val channelEntities = mutableListOf<EpgChannelEntity>()
            val rawPrograms = mutableListOf<XmltvParser.ParsedProgram>()

            RemoteDataSource.fetch(epgUrl) { stream ->
                XmltvParser.parse(
                    inputStream = stream,
                    onChannel = { parsed ->
                        channelEntities.add(
                            EpgChannelEntity(
                                playlistId = playlistId,
                                epgId = parsed.id,
                                displayName = parsed.displayName,
                                normalizedDisplayName = parsed.displayName.normalize(),
                                iconUrl = parsed.iconUrl
                            )
                        )
                    },
                    onProgram = { parsed ->
                        rawPrograms.add(parsed)
                    }
                )
            }

            // Insertar canales y obtener IDs
            epgDao.insertChannels(channelEntities)

            // Construir mapa epgId -> DB id
            channelEntities.forEach { ch ->
                val saved = epgDao.getChannelByEpgId(playlistId, ch.epgId)
                if (saved != null) channelIdMap[ch.epgId] = saved.id
            }

            // Insertar programas en lotes de 1000
            val programEntities = rawPrograms.mapNotNull { raw ->
                val epgChannelId = channelIdMap[raw.channelId] ?: return@mapNotNull null
                EpgProgramEntity(
                    epgChannelId = epgChannelId,
                    startTime = raw.startTime,
                    endTime = raw.endTime,
                    title = raw.title,
                    description = raw.description
                )
            }

            programEntities.chunked(1000).forEach { chunk ->
                epgDao.insertPrograms(chunk)
            }

            // Limpiar programas caducados (más de 24h en el pasado)
            val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
            epgDao.deletePastPrograms(cutoff)

            playlistDao.updateEpgSyncSuccess(playlistId, System.currentTimeMillis())
            Result.success(programEntities.size)

        } catch (e: Exception) {
            val msg = e.message ?: "Error desconocido"
            playlistDao.updateEpgSyncError(playlistId, msg)
            Result.failure(e)
        }
    }

    /**
     * Obtiene Now/Next para un canal dado su tvg-id o nombre normalizado.
     * Devuelve null si no hay EPG o no hay coincidencia.
     */
    suspend fun getNowNext(playlistId: Long, tvgId: String?, channelName: String): NowNextInfo? {
        val now = System.currentTimeMillis()

        val epgChannel = when {
            !tvgId.isNullOrBlank() -> epgDao.getChannelByEpgId(playlistId, tvgId)
                ?: epgDao.getChannelByNormalizedName(playlistId, channelName.normalize())
            else -> epgDao.getChannelByNormalizedName(playlistId, channelName.normalize())
        } ?: return null

        val current = epgDao.getCurrentProgram(epgChannel.id, now)
        val next = epgDao.getNextProgram(epgChannel.id, now)

        if (current == null && next == null) return null

        return NowNextInfo(
            now = current?.let {
                EpgProgram(
                    title = it.title,
                    description = it.description,
                    startTime = it.startTime,
                    endTime = it.endTime
                )
            },
            next = next?.let {
                EpgProgram(
                    title = it.title,
                    description = it.description,
                    startTime = it.startTime,
                    endTime = it.endTime
                )
            }
        )
    }
}
