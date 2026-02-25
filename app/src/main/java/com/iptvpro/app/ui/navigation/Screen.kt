package com.iptvpro.app.ui.navigation

sealed class Screen(val route: String) {
    object Playlists : Screen("playlists")

    object Home : Screen("home/{playlistId}") {
        fun createRoute(playlistId: Long) = "home/$playlistId"
    }

    object Channels : Screen("channels/{playlistId}/{groupId}/{groupName}") {
        fun createRoute(playlistId: Long, groupId: Long, groupName: String) =
            "channels/$playlistId/$groupId/${groupName.encodeForRoute()}"
    }

    object Search : Screen("search/{playlistId}") {
        fun createRoute(playlistId: Long) = "search/$playlistId"
    }
}

private fun String.encodeForRoute(): String =
    java.net.URLEncoder.encode(this, "UTF-8")
