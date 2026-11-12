package br.androidports.ui.saved

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.androidports.data.repository.NewsRepository
import br.androidports.domain.NewsItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class SavedUiState(
    val items: List<NewsItem> = emptyList(),
    val isLoading: Boolean = true,
    val readerId: String? = null,
)

class SavedViewModel(private val repository: NewsRepository) : ViewModel() {

    private val _state = MutableStateFlow(SavedUiState())
    val state: StateFlow<SavedUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeSaved().collect { items ->
                _state.update { it.copy(items = items, isLoading = false) }
            }
        }
    }

    fun remove(item: NewsItem) {
        _state.update { state ->
            state.copy(items = state.items.filterNot { it.id == item.id })
        }
        viewModelScope.launch { repository.setSaved(item, false) }
    }

    fun openReader(item: NewsItem) {
        _state.update { it.copy(readerId = item.id) }
    }

    fun closeReader() {
        _state.update { it.copy(readerId = null) }
    }

    fun toggleLike(id: String) {
        val item = _state.value.items.firstOrNull { it.id == id } ?: return
        val target = !item.liked
        _state.update { state ->
            state.copy(items = state.items.map { if (it.id == id) it.copy(liked = target) else it })
        }
        viewModelScope.launch { repository.setLiked(item, target) }
    }

    fun toggleSaved(id: String) {
        val item = _state.value.items.firstOrNull { it.id == id } ?: return
        if (item.saved) remove(item) else {
            _state.update { state ->
                state.copy(items = state.items.map { if (it.id == id) it.copy(saved = true) else it })
            }
            viewModelScope.launch { repository.setSaved(item, true) }
        }
    }

    companion object {
        fun factory(repository: NewsRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { SavedViewModel(repository) }
            }
    }
}
