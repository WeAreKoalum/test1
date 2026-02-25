package com.iptvpro.app.domain.model

data class Playlist(
    val id: Long,
    val name: String,
    val url: String,
    val epgUrl: String?,
    val lastSync: Long?,
    val lastEpgSync: Long?,
    val channelCount: Int,
    val syncError: String?,
    val epgSyncError: String?
)
