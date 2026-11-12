package br.androidports.data.local

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

const val DEFAULT_SERVER_URL = "http://10.0.2.2:3000"

private val Context.settingsDataStore by preferencesDataStore(name = "ports_settings")

enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM,
}

@Immutable
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val haptics: Boolean = true,
    val reduceMotion: Boolean = false,
    val serverUrl: String = DEFAULT_SERVER_URL,
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val theme = stringPreferencesKey("theme_mode")
        val haptics = booleanPreferencesKey("haptics")
        val reduceMotion = booleanPreferencesKey("reduce_motion")
        val serverUrl = stringPreferencesKey("server_url")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences ->
            AppSettings(
                themeMode = runCatching {
                    ThemeMode.valueOf(preferences[Keys.theme] ?: ThemeMode.DARK.name)
                }.getOrDefault(ThemeMode.DARK),
                haptics = preferences[Keys.haptics] ?: true,
                reduceMotion = preferences[Keys.reduceMotion] ?: false,
                serverUrl = preferences[Keys.serverUrl] ?: DEFAULT_SERVER_URL,
            )
        }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.theme] = mode.name }
    }

    suspend fun setHaptics(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.haptics] = enabled }
    }

    suspend fun setReduceMotion(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.reduceMotion] = enabled }
    }

    suspend fun setServerUrl(url: String) {
        context.settingsDataStore.edit { it[Keys.serverUrl] = normalizeServerUrl(url) }
    }

    companion object {
        fun normalizeServerUrl(input: String): String {
            var value = input.trim()
            if (value.isEmpty()) return DEFAULT_SERVER_URL
            if (!value.startsWith("http://") && !value.startsWith("https://")) {
                value = "http://$value"
            }
            return value.trimEnd('/')
        }

        fun isValidUrl(input: String): Boolean {
            val value = normalizeServerUrl(input)
            return runCatching { java.net.URI(value).host != null }.getOrDefault(false)
        }
    }
}
