package com.iptvpro.app.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "channels",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("playlistId"),
        Index("groupId"),
        Index(value = ["playlistId", "normalizedName"]),
        Index(value = ["playlistId", "tvgId"])
    ]
)
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val groupId: Long,
    val name: String,
    val url: String,
    val logoUrl: String? = null,
    val tvgId: String? = null,
    val tvgName: String? = null,
    /** Nombre normalizado (minúsculas, sin acentos) para búsqueda eficiente */
    val normalizedName: String
)
