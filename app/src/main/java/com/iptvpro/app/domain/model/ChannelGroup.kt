package com.iptvpro.app.domain.model

data class ChannelGroup(
    val id: Long,
    val playlistId: Long,
    val name: String,
    val channelCount: Int
)
