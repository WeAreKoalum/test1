package com.iptvpro.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.iptvpro.app.ui.screens.channels.ChannelsScreen
import com.iptvpro.app.ui.screens.home.HomeScreen
import com.iptvpro.app.ui.screens.playlists.PlaylistsScreen
import com.iptvpro.app.ui.screens.search.SearchScreen
import java.net.URLDecoder

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Playlists.route
    ) {

        composable(Screen.Playlists.route) {
            PlaylistsScreen(navController = navController)
        }

        composable(
            route = Screen.Home.route,
            arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: return@composable
            HomeScreen(navController = navController, playlistId = playlistId)
        }

        composable(
            route = Screen.Channels.route,
            arguments = listOf(
                navArgument("playlistId") { type = NavType.LongType },
                navArgument("groupId") { type = NavType.LongType },
                navArgument("groupName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: return@composable
            val groupId = backStackEntry.arguments?.getLong("groupId") ?: return@composable
            val groupName = backStackEntry.arguments?.getString("groupName")
                ?.let { URLDecoder.decode(it, "UTF-8") } ?: ""
            ChannelsScreen(
                navController = navController,
                playlistId = playlistId,
                groupId = groupId,
                groupName = groupName
            )
        }

        composable(
            route = Screen.Search.route,
            arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: return@composable
            SearchScreen(navController = navController, playlistId = playlistId)
        }
    }
}
