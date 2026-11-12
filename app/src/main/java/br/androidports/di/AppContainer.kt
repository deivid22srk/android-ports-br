package br.androidports.di

import android.content.Context
import androidx.room.Room
import br.androidports.data.SeedLoader
import br.androidports.data.api.NewsApiFactory
import br.androidports.data.local.PortsDatabase
import br.androidports.data.local.SettingsRepository
import br.androidports.data.repository.NewsRepository
import br.androidports.domain.FeedFilter
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Injeção de dependência manual: poucas peças, inicialização preguiçosa,
 * nenhum framework de DI no APK.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(12, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .cache(Cache(File(appContext.cacheDir, "http_cache"), 8L * 1024 * 1024))
        .build()

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }

    val database: PortsDatabase by lazy {
        Room.databaseBuilder(appContext, PortsDatabase::class.java, "ports.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    val apiFactory: NewsApiFactory by lazy { NewsApiFactory(okHttpClient) }

    val seedLoader: SeedLoader by lazy { SeedLoader(appContext) }

    val repository: NewsRepository by lazy {
        NewsRepository(
            dao = database.newsDao(),
            settingsRepository = settingsRepository,
            apiFactory = apiFactory,
            seedLoader = seedLoader,
        )
    }

    val feedFilter = FeedFilter()

    fun clearHttpCache() {
        runCatching { okHttpClient.cache?.evictAll() }
    }
}
