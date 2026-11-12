package br.androidports.ui.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.androidports.data.local.AppSettings
import br.androidports.data.local.SettingsRepository
import br.androidports.data.local.ThemeMode
import br.androidports.data.repository.NewsRepository
import br.androidports.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ConnectionState {
    data object Idle : ConnectionState
    data object Testing : ConnectionState
    data class Success(val newsCount: Int, val mode: String) : ConnectionState
    data class Failure(val message: String) : ConnectionState
}

@Immutable
data class SettingsUiState(
    val message: String? = null,
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val repository: NewsRepository,
    private val container: AppContainer,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings(),
    )

    private val _connection = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connection: StateFlow<ConnectionState> = _connection.asStateFlow()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setHaptics(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setHaptics(enabled) }
    }

    fun setReduceMotion(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setReduceMotion(enabled) }
    }

    fun saveServerUrl(url: String) {
        if (!SettingsRepository.isValidUrl(url)) {
            _connection.value = ConnectionState.Failure("Informe uma URL válida, por exemplo http://10.0.2.2:3000")
            return
        }
        viewModelScope.launch {
            settingsRepository.setServerUrl(url)
            _connection.value = ConnectionState.Idle
            _uiState.value = SettingsUiState(message = "Servidor salvo.")
        }
    }

    fun testConnection(url: String) {
        if (!SettingsRepository.isValidUrl(url)) {
            _connection.value = ConnectionState.Failure("URL inválida. Use http://localhost:3000, http://10.0.2.2:3000 ou http://127.0.0.1:3000.")
            return
        }
        _connection.value = ConnectionState.Testing
        viewModelScope.launch {
            val normalized = SettingsRepository.normalizeServerUrl(url)
            repository.testConnection(normalized)
                .onSuccess { health ->
                    _connection.value = ConnectionState.Success(
                        newsCount = health.newsCount,
                        mode = health.mode,
                    )
                }
                .onFailure { error ->
                    _connection.value = ConnectionState.Failure(
                        "Sem resposta do servidor (${error.message ?: "erro de rede"}).",
                    )
                }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            repository.clearCache()
            container.clearHttpCache()
            _uiState.value = SettingsUiState(
                message = "Cache limpo. As notícias salvas foram mantidas.",
            )
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    SettingsViewModel(
                        settingsRepository = container.settingsRepository,
                        repository = container.repository,
                        container = container,
                    )
                }
            }
    }
}
