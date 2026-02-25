package com.iptvpro.app.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Historial de reproducción por lista (máx. 20 entradas, sin duplicados).
 * La clave primaria compuesta evita duplicados de canal por lista.
 */
@Entity(
    tableName = "history",
    primaryKeys = ["playlistId", "channelId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChannelEntity::class,
            parentColumns = ["id"],
            childColumns = ["channelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("channelId")]
)
data class HistoryEntity(
    val playlistId: Long,
    val channelId: Long,
    /** Timestamp de la última vez que se vio (epoch millis) — se actualiza al re-ver */
    val watchedAt: Long = System.currentTimeMillis()
)
