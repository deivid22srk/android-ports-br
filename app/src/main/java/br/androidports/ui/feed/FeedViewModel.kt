package br.androidports.ui.feed

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.androidports.data.repository.NewsRepository
import br.androidports.domain.FeedFilter
import br.androidports.domain.NewsItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class FeedUiState(
    val items: List<NewsItem> = emptyList(),
    val selectedCategory: String? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val nextCursor: String? = null,
    val offline: Boolean = false,
    val error: String? = null,
    val readerId: String? = null,
)

class FeedViewModel(
    private val repository: NewsRepository,
    private val filter: FeedFilter,
) : ViewModel() {

    private val _state = MutableStateFlow(FeedUiState())
    val state: StateFlow<FeedUiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var moreJob: Job? = null

    init {
        val initialCategory = filter.category.value
        _state.update { it.copy(selectedCategory = initialCategory) }
        load(category = initialCategory, seedLocalFirst = true, showRefresh = false)
        viewModelScope.launch {
            filter.category.collectLatest { category ->
                if (category != _state.value.selectedCategory) {
                    selectCategory(category)
                }
            }
        }
    }

    fun selectCategory(category: String?) {
        if (category == _state.value.selectedCategory && _state.value.items.isNotEmpty()) return
        _state.update {
            it.copy(
                selectedCategory = category,
                items = emptyList(),
                isLoading = true,
                isRefreshing = false,
                nextCursor = null,
                error = null,
                readerId = null,
            )
        }
        load(category = category, seedLocalFirst = true, showRefresh = false)
    }

    fun refresh() {
        load(category = _state.value.selectedCategory, seedLocalFirst = false, showRefresh = true)
    }

    private fun load(category: String?, seedLocalFirst: Boolean, showRefresh: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (seedLocalFirst) {
                val local = repository.localFeed(category)
                if (_state.value.selectedCategory != category) return@launch
                _state.update { it.copy(items = local, isLoading = local.isEmpty()) }
            } else {
                _state.update { it.copy(isRefreshing = showRefresh) }
            }

            val result = repository.loadFeed(category, null)
            if (_state.value.selectedCategory != category) return@launch

            if (result.fromNetwork) {
                val items = if (result.items.isEmpty()) repository.localFeed(category) else result.items
                _state.update {
                    it.copy(
                        items = items,
                        nextCursor = result.nextCursor,
                        isLoading = false,
                        isRefreshing = false,
                        offline = false,
                        error = null,
                    )
                }
            } else {
                val fallback = if (_state.value.items.isEmpty()) result.items else _state.value.items
                _state.update {
                    it.copy(
                        items = fallback,
                        isLoading = false,
                        isRefreshing = false,
                        offline = true,
                        error = if (fallback.isEmpty()) {
                            "Sem conexão com o servidor e sem notícias em cache."
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }

    fun loadMore() {
        val snapshot = _state.value
        if (snapshot.isLoading || snapshot.isRefreshing || snapshot.isLoadingMore) return
        if (snapshot.nextCursor == null || snapshot.offline || snapshot.items.isEmpty()) return

        val category = snapshot.selectedCategory
        val cursor = snapshot.nextCursor
        moreJob = viewModelScope.launch {
            _state.update {
                if (it.selectedCategory == category) it.copy(isLoadingMore = true) else it
            }
            val result = repository.loadFeed(category, cursor)
            if (_state.value.selectedCategory != category) return@launch
            if (result.fromNetwork) {
                _state.update { current ->
                    current.copy(
                        items = (current.items + result.items).distinctBy { it.id },
                        nextCursor = result.nextCursor,
                        isLoadingMore = false,
                        offline = false,
                    )
                }
            } else {
                _state.update { it.copy(isLoadingMore = false, offline = true) }
            }
        }
    }

    fun toggleLike(id: String) {
        val item = _state.value.items.firstOrNull { it.id == id } ?: return
        val target = !item.liked
        _state.update { current ->
            current.copy(items = current.items.map { if (it.id == id) it.copy(liked = target) else it })
        }
        viewModelScope.launch { repository.setLiked(item, target) }
    }

    fun toggleSaved(id: String) {
        val item = _state.value.items.firstOrNull { it.id == id } ?: return
        val target = !item.saved
        _state.update { current ->
            current.copy(items = current.items.map { if (it.id == id) it.copy(saved = target) else it })
        }
        viewModelScope.launch { repository.setSaved(item, target) }
    }

    fun openReader(id: String) {
        _state.update { it.copy(readerId = id) }
    }

    fun closeReader() {
        _state.update { it.copy(readerId = null) }
    }

    companion object {
        fun factory(repository: NewsRepository, filter: FeedFilter): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { FeedViewModel(repository, filter) }
            }
    }
}
