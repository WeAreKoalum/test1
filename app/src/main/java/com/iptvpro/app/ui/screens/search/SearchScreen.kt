package com.iptvpro.app.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
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
fun SearchScreen(
    navController: NavController,
    playlistId: Long,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(playlistId) {
        viewModel.init(playlistId)
    }

    // Dar foco al campo de búsqueda al entrar
    LaunchedEffect(Unit) {
        searchFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Barra de búsqueda
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

                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = { viewModel.onQueryChanged(it) },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(searchFocusRequester),
                    placeholder = { Text("Buscar canales...", color = TextTertiary) },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = Primary)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = SurfaceVariant,
                        cursorColor = Primary
                    )
                )
            }

            // Resultados
            when {
                uiState.isSearching -> {
                    LoadingScreen("Buscando...", modifier = Modifier.weight(1f))
                }
                uiState.query.isBlank() -> {
                    EmptyScreen(
                        "Escribe para buscar canales en esta lista",
                        modifier = Modifier.weight(1f)
                    )
                }
                uiState.results.isEmpty() -> {
                    EmptyScreen(
                        "No se encontraron canales para '${uiState.query}'",
                        modifier = Modifier.weight(1f)
                    )
                }
                else -> {
                    Text(
                        text = "${uiState.results.size} resultado(s)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 48.dp, vertical = 4.dp)
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        items(uiState.results, key = { it.id }) { channel ->
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
