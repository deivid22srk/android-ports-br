package br.androidports.ui.explore

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.androidports.data.repository.NewsRepository
import br.androidports.domain.CategoryCount
import br.androidports.domain.DefaultCategories
import br.androidports.domain.NewsItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class ExploreUiState(
    val query: String = "",
    val results: List<NewsItem> = emptyList(),
    val trending: List<NewsItem> = emptyList(),
    val categories: List<CategoryCount> = emptyList(),
    val isLoading: Boolean = true,
    val isSearching: Boolean = false,
    val offline: Boolean = false,
    val readerId: String? = null,
)

class ExploreViewModel(private val repository: NewsRepository) : ViewModel() {

    private val _state = MutableStateFlow(ExploreUiState())
    val state: StateFlow<ExploreUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val local = repository.localFeed(null)
            _state.update {
                it.copy(
                    trending = local.sortedByDescending { item -> item.likes }.take(10),
                    categories = categoriesFrom(local),
                    isLoading = false,
                )
            }

            val refreshed = repository.explore()
            _state.update {
                it.copy(
                    trending = refreshed.trending,
                    categories = refreshed.categories,
                    isLoading = false,
                    offline = repository.offline.value,
                )
            }
        }
    }

    fun retry() {
        _state.update { it.copy(isLoading = it.trending.isEmpty()) }
        load()
    }

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
        searchJob?.cancel()

        if (value.isBlank()) {
            _state.update { it.copy(results = emptyList(), isSearching = false) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(350)
            _state.update { if (it.query == value) it.copy(isSearching = true) else it }
            val results = repository.search(value)
            if (_state.value.query == value) {
                _state.update {
                    it.copy(
                        results = results,
                        isSearching = false,
                        offline = repository.offline.value,
                    )
                }
            }
        }
    }

    fun openReader(item: NewsItem) {
        _state.update { it.copy(readerId = item.id) }
    }

    fun closeReader() {
        _state.update { it.copy(readerId = null) }
    }

    fun toggleLike(id: String) {
        val item = findItem(id) ?: return
        val target = !item.liked
        updateItem(id) { it.copy(liked = target) }
        viewModelScope.launch { repository.setLiked(item, target) }
    }

    fun toggleSaved(id: String) {
        val item = findItem(id) ?: return
        val target = !item.saved
        updateItem(id) { it.copy(saved = target) }
        viewModelScope.launch { repository.setSaved(item, target) }
    }

    private fun findItem(id: String): NewsItem? =
        _state.value.trending.firstOrNull { it.id == id }
            ?: _state.value.results.firstOrNull { it.id == id }

    private fun updateItem(id: String, transform: (NewsItem) -> NewsItem) {
        _state.update { state ->
            state.copy(
                trending = state.trending.map { if (it.id == id) transform(it) else it },
                results = state.results.map { if (it.id == id) transform(it) else it },
            )
        }
    }

    private fun categoriesFrom(items: List<NewsItem>): List<CategoryCount> {
        val counts = items.groupingBy { it.category }.eachCount()
        return DefaultCategories.map { category ->
            CategoryCount(category.id, category.label, counts[category.id] ?: 0)
        }
    }

    companion object {
        fun factory(repository: NewsRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { ExploreViewModel(repository) }
            }
    }
}
