package com.iptvpro.app.ui.screens.channels

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

data class ChannelsUiState(
    val channels: List<Channel> = emptyList(),
    val favoriteIds: Set<Long> = emptySet(),
    val nowNextMap: Map<Long, NowNextInfo> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class ChannelsViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val epgRepository: EpgRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChannelsUiState())
    val uiState: StateFlow<ChannelsUiState> = _uiState.asStateFlow()

    fun init(playlistId: Long, groupId: Long) {
        channelRepository.getChannelsByGroup(groupId)
            .onEach { channels ->
                _uiState.value = _uiState.value.copy(channels = channels, isLoading = false)
                loadEpg(playlistId, channels)
            }
            .launchIn(viewModelScope)

        channelRepository.getFavoriteIds(playlistId)
            .onEach { ids ->
                _uiState.value = _uiState.value.copy(favoriteIds = ids)
            }
            .launchIn(viewModelScope)
    }

    private fun loadEpg(playlistId: Long, channels: List<Channel>) {
        viewModelScope.launch {
            val map = _uiState.value.nowNextMap.toMutableMap()
            channels.forEach { ch ->
                if (!map.containsKey(ch.id)) {
                    val info = epgRepository.getNowNext(playlistId, ch.tvgId, ch.name)
                    if (info != null) map[ch.id] = info
                }
            }
            _uiState.value = _uiState.value.copy(nowNextMap = map)
        }
    }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch {
            channelRepository.toggleFavorite(channel.playlistId, channel.id)
        }
    }
}
