package br.androidports.data.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class NewsDto(
    val id: String,
    val title: String,
    val summary: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val category: String = "lancamentos",
    val sourceName: String = "",
    val sourceUrl: String = "",
    val publishedAt: String = "",
    val likes: Int = 0,
)

@Serializable
data class FeedResponseDto(
    val items: List<NewsDto> = emptyList(),
    val nextCursor: String? = null,
    val total: Int = 0,
    val updatedAt: String? = null,
    val offline: Boolean = false,
)

@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    val count: Int = 0,
)

@Serializable
data class CategoriesResponseDto(
    val categories: List<CategoryDto> = emptyList(),
)

@Serializable
data class SearchResponseDto(
    val query: String = "",
    val items: List<NewsDto> = emptyList(),
)

@Serializable
data class HealthResponseDto(
    val status: String = "unknown",
    val newsCount: Int = 0,
    val mode: String = "seed",
)

interface NewsApi {
    @GET("api/v1/news")
    suspend fun news(
        @Query("category") category: String?,
        @Query("cursor") cursor: String?,
        @Query("limit") limit: Int,
    ): FeedResponseDto

    @GET("api/v1/news/{id}")
    suspend fun detail(@Path("id") id: String): NewsDto

    @GET("api/v1/categories")
    suspend fun categories(): CategoriesResponseDto

    @GET("api/v1/search")
    suspend fun search(@Query("q") query: String): SearchResponseDto

    @GET("api/v1/health")
    suspend fun health(): HealthResponseDto
}

/**
 * Cria (e reutiliza) um Retrofit por URL base. A URL do servidor é editável
 * em Ajustes, então não podemos fixar o host no build.
 */
class NewsApiFactory(private val client: OkHttpClient) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    private val apis = ConcurrentHashMap<String, NewsApi>()

    fun create(baseUrl: String): NewsApi {
        val normalized = baseUrl.trim().trimEnd('/') + "/"
        return apis.getOrPut(normalized) {
            Retrofit.Builder()
                .baseUrl(normalized)
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(NewsApi::class.java)
        }
    }
}
