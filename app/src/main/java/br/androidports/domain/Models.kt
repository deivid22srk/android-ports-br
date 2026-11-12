package br.androidports.domain

import androidx.compose.runtime.Immutable

@Immutable
data class NewsItem(
    val id: String,
    val title: String,
    val summary: String,
    val content: String,
    val imageUrl: String,
    val category: String,
    val sourceName: String,
    val sourceUrl: String,
    val publishedAt: Long,
    val likes: Int,
    val saved: Boolean = false,
    val liked: Boolean = false,
)

@Immutable
data class Category(
    val id: String,
    val label: String,
)

@Immutable
data class CategoryCount(
    val id: String,
    val label: String,
    val count: Int,
)

@Immutable
data class ExploreData(
    val trending: List<NewsItem> = emptyList(),
    val categories: List<CategoryCount> = emptyList(),
)

val DefaultCategories: List<Category> = listOf(
    Category("ports", "Ports"),
    Category("roms", "ROMs"),
    Category("apps", "Apps"),
    Category("dicas", "Dicas"),
    Category("lancamentos", "Lançamentos"),
)

fun categoryLabel(id: String): String =
    DefaultCategories.firstOrNull { it.id == id }?.label
        ?: id.replaceFirstChar { it.uppercase() }
