package com.iptvpro.app.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvpro.app.data.repository.ChannelRepository
import com.iptvpro.app.data.repository.EpgRepository
import com.iptvpro.app.domain.model.Channel
import com.iptvpro.app.domain.model.NowNextInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val channel: Channel? = null,
    val isFavorite: Boolean = false,
    val nowNext: NowNextInfo? = null,
    val isOverlayVisible: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val epgRepository: EpgRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    fun init(channel: Channel, playlistId: Long) {
        _uiState.value = _uiState.value.copy(channel = channel)

        // Registrar en historial
        viewModelScope.launch {
            channelRepository.recordHistory(playlistId, channel.id)
        }

        // Observar favorito
        channelRepository.getFavoriteIds(playlistId)
            .onEach { ids ->
                _uiState.value = _uiState.value.copy(isFavorite = channel.id in ids)
            }
            .launchIn(viewModelScope)

        // Cargar EPG
        viewModelScope.launch {
            val nowNext = epgRepository.getNowNext(playlistId, channel.tvgId, channel.name)
            _uiState.value = _uiState.value.copy(nowNext = nowNext)
        }
    }

    fun toggleFavorite() {
        val channel = _uiState.value.channel ?: return
        viewModelScope.launch {
            channelRepository.toggleFavorite(channel.playlistId, channel.id)
        }
    }

    fun showOverlay() {
        _uiState.value = _uiState.value.copy(isOverlayVisible = true)
    }

    fun hideOverlay() {
        _uiState.value = _uiState.value.copy(isOverlayVisible = false)
    }

    fun toggleOverlay() {
        _uiState.value = _uiState.value.copy(
            isOverlayVisible = !_uiState.value.isOverlayVisible
        )
    }

    fun setError(message: String) {
        _uiState.value = _uiState.value.copy(error = message)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
