package com.iptvpro.app.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "epg_programs",
    foreignKeys = [
        ForeignKey(
            entity = EpgChannelEntity::class,
            parentColumns = ["id"],
            childColumns = ["epgChannelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("epgChannelId"),
        Index(value = ["epgChannelId", "startTime", "endTime"])
    ]
)
data class EpgProgramEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epgChannelId: Long,
    /** Epoch millis UTC */
    val startTime: Long,
    /** Epoch millis UTC */
    val endTime: Long,
    val title: String,
    val description: String? = null
)
