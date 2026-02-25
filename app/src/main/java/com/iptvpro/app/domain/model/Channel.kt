package com.iptvpro.app.domain.model

data class Channel(
    val id: Long,
    val playlistId: Long,
    val groupId: Long,
    val name: String,
    val url: String,
    val logoUrl: String?,
    val tvgId: String?,
    val tvgName: String?
)
