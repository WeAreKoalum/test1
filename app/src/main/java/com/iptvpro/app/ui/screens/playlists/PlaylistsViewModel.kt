package com.iptvpro.app.ui.screens.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvpro.app.data.remote.RemoteDataSource
import com.iptvpro.app.data.repository.EpgRepository
import com.iptvpro.app.data.repository.PlaylistRepository
import com.iptvpro.app.domain.model.Playlist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistsUiState(
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = false,
    val syncingPlaylistId: Long? = null,
    val syncingEpgPlaylistId: Long? = null,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val epgRepository: EpgRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistsUiState())
    val uiState: StateFlow<PlaylistsUiState> = _uiState.asStateFlow()

    init {
        playlistRepository.getAllPlaylists()
            .onEach { playlists ->
                _uiState.value = _uiState.value.copy(playlists = playlists)
            }
            .launchIn(viewModelScope)
    }

    fun addPlaylist(name: String, url: String, epgUrl: String?) {
        if (!validateInputs(name, url)) return
        viewModelScope.launch {
            try {
                val id = playlistRepository.addPlaylist(name, url, epgUrl)
                syncPlaylist(id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Error al guardar: ${e.message}")
            }
        }
    }

    fun updatePlaylist(id: Long, name: String, url: String, epgUrl: String?) {
        if (!validateInputs(name, url)) return
        viewModelScope.launch {
            try {
                playlistRepository.updatePlaylist(id, name, url, epgUrl)
                syncPlaylist(id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Error al actualizar: ${e.message}")
            }
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(id)
            _uiState.value = _uiState.value.copy(successMessage = "Lista eliminada")
        }
    }

    fun syncPlaylist(playlistId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(syncingPlaylistId = playlistId)
            val result = playlistRepository.syncPlaylist(playlistId)
            _uiState.value = _uiState.value.copy(syncingPlaylistId = null)
            result.onFailure { e ->
                _uiState.value = _uiState.value.copy(error = "Error al sincronizar: ${e.message}")
            }
            result.onSuccess { count ->
                _uiState.value = _uiState.value.copy(successMessage = "Lista actualizada ($count canales)")
            }
        }
    }

    fun syncEpg(playlistId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(syncingEpgPlaylistId = playlistId)
            val result = epgRepository.syncEpg(playlistId)
            _uiState.value = _uiState.value.copy(syncingEpgPlaylistId = null)
            result.onFailure { e ->
                _uiState.value = _uiState.value.copy(error = "Error EPG: ${e.message}")
            }
            result.onSuccess { count ->
                val msg = if (count == 0) "Sin EPG configurada" else "EPG actualizada ($count programas)"
                _uiState.value = _uiState.value.copy(successMessage = msg)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    private fun validateInputs(name: String, url: String): Boolean {
        return when {
            name.isBlank() -> {
                _uiState.value = _uiState.value.copy(error = "El nombre es obligatorio")
                false
            }
            url.isBlank() -> {
                _uiState.value = _uiState.value.copy(error = "La URL es obligatoria")
                false
            }
            !RemoteDataSource.isValidUrl(url) -> {
                _uiState.value = _uiState.value.copy(error = "URL no válida (debe ser http o https)")
                false
            }
            else -> true
        }
    }
}
