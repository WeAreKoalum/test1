package com.iptvpro.app.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "playlists",
    indices = [Index(value = ["url"], unique = true)]
)
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String,
    val epgUrl: String? = null,
    /** URL EPG auto-detectada de la cabecera M3U; se guarda por separado para
     *  poder sobrescribirse manualmente */
    val autoDetectedEpgUrl: String? = null,
    val lastSync: Long? = null,
    val lastEpgSync: Long? = null,
    val channelCount: Int = 0,
    val syncError: String? = null,
    val epgSyncError: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    /** Devuelve la URL EPG efectiva: la manual tiene prioridad sobre la auto-detectada */
    fun effectiveEpgUrl(): String? = epgUrl ?: autoDetectedEpgUrl
}
