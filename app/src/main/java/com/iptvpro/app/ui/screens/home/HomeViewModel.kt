package com.iptvpro.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvpro.app.data.repository.ChannelRepository
import com.iptvpro.app.data.repository.EpgRepository
import com.iptvpro.app.data.repository.PlaylistRepository
import com.iptvpro.app.domain.model.Channel
import com.iptvpro.app.domain.model.ChannelGroup
import com.iptvpro.app.domain.model.NowNextInfo
import com.iptvpro.app.domain.model.Playlist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val playlist: Playlist? = null,
    val favorites: List<Channel> = emptyList(),
    val history: List<Channel> = emptyList(),
    val groups: List<ChannelGroup> = emptyList(),
    val favoriteIds: Set<Long> = emptySet(),
    val nowNextMap: Map<Long, NowNextInfo> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val channelRepository: ChannelRepository,
    private val epgRepository: EpgRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun init(playlistId: Long) {
        // Observar playlist
        viewModelScope.launch {
            playlistRepository.getAllPlaylists()
                .map { list -> list.firstOrNull { it.id == playlistId } }
                .collect { playlist ->
                    _uiState.value = _uiState.value.copy(playlist = playlist)
                }
        }

        // Observar grupos
        channelRepository.getGroups(playlistId)
            .onEach { groups ->
                _uiState.value = _uiState.value.copy(groups = groups, isLoading = false)
            }
            .launchIn(viewModelScope)

        // Observar favoritos
        channelRepository.getFavoriteChannels(playlistId)
            .onEach { favs ->
                _uiState.value = _uiState.value.copy(favorites = favs)
                loadEpgForChannels(playlistId, favs)
            }
            .launchIn(viewModelScope)

        // Observar historial
        channelRepository.getHistoryChannels(playlistId)
            .onEach { hist ->
                _uiState.value = _uiState.value.copy(history = hist)
                loadEpgForChannels(playlistId, hist)
            }
            .launchIn(viewModelScope)

        // Observar IDs de favoritos para el estado del icono
        channelRepository.getFavoriteIds(playlistId)
            .onEach { ids ->
                _uiState.value = _uiState.value.copy(favoriteIds = ids)
            }
            .launchIn(viewModelScope)
    }

    private fun loadEpgForChannels(playlistId: Long, channels: List<Channel>) {
        viewModelScope.launch {
            val nowNextMap = _uiState.value.nowNextMap.toMutableMap()
            channels.forEach { channel ->
                if (!nowNextMap.containsKey(channel.id)) {
                    val info = epgRepository.getNowNext(playlistId, channel.tvgId, channel.name)
                    if (info != null) nowNextMap[channel.id] = info
                }
            }
            _uiState.value = _uiState.value.copy(nowNextMap = nowNextMap)
        }
    }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch {
            channelRepository.toggleFavorite(channel.playlistId, channel.id)
        }
    }
}
