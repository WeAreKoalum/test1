package com.iptvpro.app.ui.screens.channels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.iptvpro.app.ui.components.ChannelCard
import com.iptvpro.app.ui.components.EmptyScreen
import com.iptvpro.app.ui.components.LoadingScreen
import com.iptvpro.app.ui.screens.player.PlayerActivity
import com.iptvpro.app.ui.theme.*

@Composable
fun ChannelsScreen(
    navController: NavController,
    playlistId: Long,
    groupId: Long,
    groupName: String,
    viewModel: ChannelsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(groupId) {
        viewModel.init(playlistId, groupId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = TextPrimary
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = groupName,
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (uiState.channels.isNotEmpty()) {
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = "(${uiState.channels.size} canales)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                }
            }

            when {
                uiState.isLoading -> LoadingScreen("Cargando canales...")
                uiState.channels.isEmpty() -> EmptyScreen("No hay canales en esta categoría")
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.channels, key = { it.id }) { channel ->
                            ChannelCard(
                                channel = channel,
                                isFavorite = channel.id in uiState.favoriteIds,
                                nowNext = uiState.nowNextMap[channel.id],
                                onClick = {
                                    PlayerActivity.launch(context, channel, playlistId)
                                },
                                onFavoriteToggle = {
                                    viewModel.toggleFavorite(channel)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
