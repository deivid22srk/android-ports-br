package br.androidports.data

import android.content.Context
import br.androidports.data.api.NewsDto
import br.androidports.data.api.toDomain
import br.androidports.domain.NewsItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class SeedFile(
    val items: List<NewsDto> = emptyList(),
)

/**
 * Fonte de verdade offline: o APK sempre carrega as 32 notícias de demonstração.
 */
class SeedLoader(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun load(): List<NewsItem> = withContext(Dispatchers.IO) {
        runCatching {
            val raw = context.assets.open("seed_news.json").bufferedReader().use { it.readText() }
            json.decodeFromString(SeedFile.serializer(), raw).items.map { it.toDomain() }
        }.getOrElse { emptyList() }
    }
}
