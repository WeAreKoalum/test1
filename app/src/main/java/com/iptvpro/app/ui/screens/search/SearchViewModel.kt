package com.iptvpro.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvpro.app.data.repository.ChannelRepository
import com.iptvpro.app.data.repository.EpgRepository
import com.iptvpro.app.domain.model.Channel
import com.iptvpro.app.domain.model.NowNextInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<Channel> = emptyList(),
    val favoriteIds: Set<Long> = emptySet(),
    val nowNextMap: Map<Long, NowNextInfo> = emptyMap(),
    val isSearching: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val epgRepository: EpgRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var playlistId: Long = 0L

    fun init(playlistId: Long) {
        this.playlistId = playlistId
        channelRepository.getFavoriteIds(playlistId)
            .onEach { ids -> _uiState.value = _uiState.value.copy(favoriteIds = ids) }
            .launchIn(viewModelScope)
    }

    fun onQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(results = emptyList(), isSearching = false)
            return
        }
        searchJob = viewModelScope.launch {
            delay(250) // debounce
            _uiState.value = _uiState.value.copy(isSearching = true)
            val results = channelRepository.searchChannels(playlistId, query)
            _uiState.value = _uiState.value.copy(results = results, isSearching = false)
            loadEpg(results)
        }
    }

    private fun loadEpg(channels: List<Channel>) {
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
