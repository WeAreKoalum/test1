package com.iptvpro.app.ui.screens.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.iptvpro.app.domain.model.Playlist
import com.iptvpro.app.ui.components.EmptyScreen
import com.iptvpro.app.ui.components.ErrorScreen
import com.iptvpro.app.ui.components.LoadingScreen
import com.iptvpro.app.ui.navigation.Screen
import com.iptvpro.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlaylistsScreen(
    navController: NavController,
    viewModel: PlaylistsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var deletingPlaylist by remember { mutableStateOf<Playlist?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Mostrar mensajes
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccessMessage()
        }
    }

    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp, vertical = 32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "IPTV Pro",
                        style = MaterialTheme.typography.displayMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Mis Listas M3U",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextSecondary
                    )
                }

                // Lista de playlists
                when {
                    uiState.playlists.isEmpty() -> {
                        EmptyScreen(
                            message = "No hay listas configuradas.\nPulsa el botón '+' para agregar tu primera lista M3U.",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            state = rememberLazyListState(),
                            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.playlists, key = { it.id }) { playlist ->
                                PlaylistItem(
                                    playlist = playlist,
                                    isSyncing = uiState.syncingPlaylistId == playlist.id,
                                    isSyncingEpg = uiState.syncingEpgPlaylistId == playlist.id,
                                    onOpen = {
                                        navController.navigate(Screen.Home.createRoute(playlist.id))
                                    },
                                    onEdit = { editingPlaylist = playlist },
                                    onDelete = { deletingPlaylist = playlist },
                                    onRefresh = { viewModel.syncPlaylist(playlist.id) },
                                    onRefreshEpg = { viewModel.syncEpg(playlist.id) }
                                )
                            }
                        }
                    }
                }

                // Botón agregar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    FocusableButton(
                        text = "+ Agregar lista M3U",
                        onClick = { showAddDialog = true }
                    )
                }
            }
        }
    }

    // Diálogo agregar
    if (showAddDialog) {
        PlaylistDialog(
            title = "Agregar lista M3U",
            onDismiss = { showAddDialog = false },
            onConfirm = { name, url, epgUrl ->
                viewModel.addPlaylist(name, url, epgUrl)
                showAddDialog = false
            }
        )
    }

    // Diálogo editar
    editingPlaylist?.let { playlist ->
        PlaylistDialog(
            title = "Editar lista",
            initialName = playlist.name,
            initialUrl = playlist.url,
            initialEpgUrl = playlist.epgUrl ?: "",
            onDismiss = { editingPlaylist = null },
            onConfirm = { name, url, epgUrl ->
                viewModel.updatePlaylist(playlist.id, name, url, epgUrl)
                editingPlaylist = null
            }
        )
    }

    // Diálogo confirmar borrado
    deletingPlaylist?.let { playlist ->
        AlertDialog(
            onDismissRequest = { deletingPlaylist = null },
            containerColor = SurfaceColor,
            title = {
                Text("Eliminar lista", color = TextPrimary)
            },
            text = {
                Text(
                    "¿Eliminar '${playlist.name}'? Se perderán los favoritos e historial de esta lista.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePlaylist(playlist.id)
                    deletingPlaylist = null
                }) {
                    Text("Eliminar", color = Accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingPlaylist = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun PlaylistItem(
    playlist: Playlist,
    isSyncing: Boolean,
    isSyncingEpg: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRefresh: () -> Unit,
    onRefreshEpg: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        onClick = onOpen,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .scale(if (isFocused) 1.02f else 1f)
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) FocusBorder else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) CardFocused else CardBackground
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono
            Icon(
                imageVector = Icons.Filled.PlaylistPlay,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(40.dp)
            )

            Spacer(Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = playlist.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    if (playlist.channelCount > 0) {
                        Text(
                            text = "${playlist.channelCount} canales",
                            style = MaterialTheme.typography.labelSmall,
                            color = EpgNow
                        )
                    }
                    playlist.lastSync?.let {
                        Text(
                            text = "Sync: ${formatDate(it)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary
                        )
                    }
                    if (playlist.epgUrl != null) {
                        Text(
                            text = "EPG: ${if (playlist.lastEpgSync != null) formatDate(playlist.lastEpgSync) else "Sin sync"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary
                        )
                    }
                }
                playlist.syncError?.let {
                    Text(
                        text = "Error: $it",
                        style = MaterialTheme.typography.labelSmall,
                        color = ErrorColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Acciones
            if (isFocused || isSyncing || isSyncingEpg) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            color = Primary,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Filled.Refresh, "Actualizar lista", tint = Primary)
                        }
                    }
                    if (isSyncingEpg) {
                        CircularProgressIndicator(
                            color = EpgNow,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = onRefreshEpg) {
                            Icon(Icons.Filled.Schedule, "Actualizar EPG", tint = EpgNow)
                        }
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, "Editar", tint = TextSecondary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, "Eliminar", tint = ErrorColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistDialog(
    title: String,
    initialName: String = "",
    initialUrl: String = "",
    initialEpgUrl: String = "",
    onDismiss: () -> Unit,
    onConfirm: (name: String, url: String, epgUrl: String?) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var url by remember { mutableStateOf(initialUrl) }
    var epgUrl by remember { mutableStateOf(initialEpgUrl) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceColor),
            modifier = Modifier.width(500.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la lista") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = Primary,
                        unfocusedLabelColor = TextSecondary,
                        focusedBorderColor = Primary
                    )
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL de la lista M3U") },
                    placeholder = { Text("http://...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = Primary,
                        unfocusedLabelColor = TextSecondary,
                        focusedBorderColor = Primary
                    )
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = epgUrl,
                    onValueChange = { epgUrl = it },
                    label = { Text("URL de la guía EPG (opcional)") },
                    placeholder = { Text("http://.../epg.xml") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = EpgNow,
                        unfocusedLabelColor = TextSecondary,
                        focusedBorderColor = EpgNow
                    )
                )

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = TextSecondary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(
                                name.trim(),
                                url.trim(),
                                epgUrl.trim().ifBlank { null }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusableButton(
    text: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Button(
        onClick = onClick,
        modifier = Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .scale(if (isFocused) 1.05f else 1f)
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) FocusBorder else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ),
        colors = ButtonDefaults.buttonColors(containerColor = Primary)
    ) {
        Icon(Icons.Filled.Add, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}

private fun formatDate(epochMillis: Long): String {
    val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    return sdf.format(Date(epochMillis))
}
