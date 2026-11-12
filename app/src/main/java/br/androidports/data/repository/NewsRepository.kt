package br.androidports.data.repository

import br.androidports.data.SeedLoader
import br.androidports.data.api.NewsApiFactory
import br.androidports.data.api.toDomain
import br.androidports.data.local.NewsDao
import br.androidports.data.local.SettingsRepository
import br.androidports.data.local.toDomain
import br.androidports.data.local.toEntity
import br.androidports.domain.CategoryCount
import br.androidports.domain.DefaultCategories
import br.androidports.domain.ExploreData
import br.androidports.domain.NewsItem
import br.androidports.data.api.HealthResponseDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class FeedResult(
    val items: List<NewsItem>,
    val nextCursor: String?,
    val fromNetwork: Boolean,
    val error: String? = null,
)

/**
 * Ordem de leitura: servidor -> cache Room -> seed embutido.
 * Nenhuma chamada de rede bloqueia a primeira pintura do feed.
 */
class NewsRepository(
    private val dao: NewsDao,
    private val settingsRepository: SettingsRepository,
    private val apiFactory: NewsApiFactory,
    private val seedLoader: SeedLoader,
) {
    private val _offline = MutableStateFlow(false)
    val offline: StateFlow<Boolean> = _offline.asStateFlow()

    private val seedLock = Mutex()
    private var seedCache: List<NewsItem>? = null

    private suspend fun seedItems(): List<NewsItem> =
        seedCache ?: seedLock.withLock { seedCache ?: seedLoader.load().also { seedCache = it } }

    private suspend fun ensureSeeded() {
        if (dao.count() > 0) return
        val seed = seedItems()
        if (seed.isNotEmpty()) {
            dao.upsert(seed.map { it.toEntity() })
        }
    }

    suspend fun localFeed(category: String?): List<NewsItem> = withContext(Dispatchers.IO) {
        localFeedInternal(category)
    }

    private suspend fun localFeedInternal(category: String?): List<NewsItem> {
        ensureSeeded()
        val entities = dao.all()
        val items = if (entities.isEmpty()) seedItems() else entities.map { it.toDomain() }
        return if (category == null) items else items.filter { it.category == category }
    }

    suspend fun loadFeed(category: String?, cursor: String?, limit: Int = 12): FeedResult =
        withContext(Dispatchers.IO) {
            val baseUrl = settingsRepository.current().serverUrl
            try {
                val page = apiFactory.create(baseUrl).news(category, cursor, limit)
                val mapped = page.items.map { it.toDomain() }
                if (mapped.isNotEmpty()) {
                    upsertPreservingState(mapped)
                }
                _offline.value = false
                FeedResult(
                    items = applyFlags(mapped),
                    nextCursor = page.nextCursor,
                    fromNetwork = true,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _offline.value = true
                FeedResult(
                    items = localFeedInternal(category),
                    nextCursor = null,
                    fromNetwork = false,
                    error = error.message,
                )
            }
        }

    suspend fun refreshCache(): List<NewsItem> = withContext(Dispatchers.IO) {
        val baseUrl = settingsRepository.current().serverUrl
        runCatching { apiFactory.create(baseUrl).news(null, null, 40) }
            .onSuccess { page ->
                val mapped = page.items.map { it.toDomain() }
                if (mapped.isNotEmpty()) upsertPreservingState(mapped)
                _offline.value = false
            }
            .onFailure { if (it !is CancellationException) _offline.value = true }
        localFeedInternal(null)
    }

    suspend fun explore(): ExploreData = withContext(Dispatchers.IO) {
        val baseUrl = settingsRepository.current().serverUrl
        runCatching { apiFactory.create(baseUrl).news(null, null, 40) }
            .onSuccess { page ->
                val mapped = page.items.map { it.toDomain() }
                if (mapped.isNotEmpty()) upsertPreservingState(mapped)
                _offline.value = false
            }
            .onFailure { if (it !is CancellationException) _offline.value = true }

        val all = localFeedInternal(null)
        val counts = all.groupingBy { it.category }.eachCount()
        ExploreData(
            trending = all.sortedByDescending { it.likes }.take(10),
            categories = DefaultCategories.map { category ->
                CategoryCount(category.id, category.label, counts[category.id] ?: 0)
            },
        )
    }

    suspend fun search(query: String): List<NewsItem> = withContext(Dispatchers.IO) {
        val term = query.trim()
        if (term.length < 2) return@withContext emptyList()

        val local = localFeedInternal(null).filter {
            it.title.contains(term, ignoreCase = true) || it.summary.contains(term, ignoreCase = true)
        }

        val remote = try {
            val baseUrl = settingsRepository.current().serverUrl
            val items = apiFactory.create(baseUrl).search(term).items.map { it.toDomain() }
            if (items.isNotEmpty()) upsertPreservingState(items)
            _offline.value = false
            items
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            _offline.value = true
            emptyList()
        }

        applyFlags((remote + local).distinctBy { it.id })
            .sortedByDescending { it.publishedAt }
    }

    suspend fun setLiked(item: NewsItem, liked: Boolean) = withContext(Dispatchers.IO) {
        if (dao.byId(item.id) == null) {
            dao.upsert(listOf(item.toEntity(saved = item.saved, liked = liked)))
        } else {
            dao.setLiked(item.id, liked)
        }
    }

    suspend fun setSaved(item: NewsItem, saved: Boolean) = withContext(Dispatchers.IO) {
        if (dao.byId(item.id) == null) {
            dao.upsert(listOf(item.toEntity(saved = saved, liked = item.liked)))
        } else {
            dao.setSaved(item.id, saved)
        }
    }

    fun observeSaved(): Flow<List<NewsItem>> =
        dao.observeSaved().map { entities -> entities.map { it.toDomain() } }

    fun observeAll(): Flow<List<NewsItem>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun testConnection(url: String): Result<HealthResponseDto> = withContext(Dispatchers.IO) {
        runCatching { apiFactory.create(url).health() }
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        dao.clearUnsaved()
    }

    private suspend fun upsertPreservingState(items: List<NewsItem>) {
        val flags = dao.flags().associateBy { it.id }
        val now = System.currentTimeMillis()
        dao.upsert(
            items.map { item ->
                val flag = flags[item.id]
                item.toEntity(
                    saved = flag?.saved ?: false,
                    liked = flag?.liked ?: false,
                    cachedAt = now,
                )
            },
        )
    }

    private suspend fun applyFlags(items: List<NewsItem>): List<NewsItem> {
        if (items.isEmpty()) return items
        val flags = dao.flags().associateBy { it.id }
        return items.map { item ->
            val flag = flags[item.id] ?: return@map item
            item.copy(saved = flag.saved, liked = flag.liked)
        }
    }
}
