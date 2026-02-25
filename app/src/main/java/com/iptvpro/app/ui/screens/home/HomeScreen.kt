package com.iptvpro.app.ui.screens.home

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
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
import com.iptvpro.app.ui.components.GroupCard
import com.iptvpro.app.ui.components.LoadingScreen
import com.iptvpro.app.ui.navigation.Screen
import com.iptvpro.app.ui.screens.player.PlayerActivity
import com.iptvpro.app.ui.theme.*

@Composable
fun HomeScreen(
    navController: NavController,
    playlistId: Long,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(playlistId) {
        viewModel.init(playlistId)
    }

    if (uiState.isLoading && uiState.groups.isEmpty()) {
        LoadingScreen("Cargando lista...")
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Header con nombre de la lista
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp, vertical = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                Icons.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = TextPrimary
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = uiState.playlist?.name ?: "Mi Lista",
                            style = MaterialTheme.typography.headlineLarge,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = {
                            navController.navigate(Screen.Search.createRoute(playlistId))
                        }
                    ) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "Buscar canales",
                            tint = Primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // Fila 1: Favoritos
            if (uiState.favorites.isNotEmpty()) {
                item {
                    SectionHeader("Favoritos")
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 48.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        state = rememberLazyListState()
                    ) {
                        items(uiState.favorites, key = { it.id }) { channel ->
                            ChannelCard(
                                channel = channel,
                                isFavorite = channel.id in uiState.favoriteIds,
                                nowNext = uiState.nowNextMap[channel.id],
                                onClick = {
                                    PlayerActivity.launch(context, channel, playlistId)
                                    viewModel.toggleFavorite(channel) // just to record history elsewhere
                                },
                                onFavoriteToggle = { viewModel.toggleFavorite(channel) }
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }

            // Fila 2: Últimos vistos
            if (uiState.history.isNotEmpty()) {
                item {
                    SectionHeader("Vistos recientemente")
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 48.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        state = rememberLazyListState()
                    ) {
                        items(uiState.history, key = { it.id }) { channel ->
                            ChannelCard(
                                channel = channel,
                                isFavorite = channel.id in uiState.favoriteIds,
                                nowNext = uiState.nowNextMap[channel.id],
                                onClick = {
                                    PlayerActivity.launch(context, channel, playlistId)
                                },
                                onFavoriteToggle = { viewModel.toggleFavorite(channel) }
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }

            // Sección grupos/categorías (alfabético)
            if (uiState.groups.isNotEmpty()) {
                item {
                    SectionHeader("Categorías")
                }
                items(
                    items = uiState.groups.chunked(4),
                    key = { it.first().id }
                ) { rowGroups ->
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 48.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        items(rowGroups, key = { it.id }) { group ->
                            GroupCard(
                                group = group,
                                onClick = {
                                    navController.navigate(
                                        Screen.Channels.createRoute(
                                            playlistId = playlistId,
                                            groupId = group.id,
                                            groupName = group.name
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Estado vacío
            if (uiState.groups.isEmpty() && !uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Esta lista no tiene canales.\nActualízala desde la pantalla principal.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        color = TextPrimary,
        modifier = Modifier.padding(start = 48.dp, bottom = 12.dp, top = 8.dp)
    )
}
