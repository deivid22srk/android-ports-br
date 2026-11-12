package br.androidports.data.api

import br.androidports.domain.NewsItem
import java.time.Instant

fun NewsDto.toDomain(now: Long = System.currentTimeMillis()): NewsItem = NewsItem(
    id = id,
    title = title,
    summary = summary,
    content = content.ifBlank { summary },
    imageUrl = imageUrl.orEmpty(),
    category = category.ifBlank { "lancamentos" },
    sourceName = sourceName,
    sourceUrl = sourceUrl,
    publishedAt = parsePublishedAt(publishedAt, now),
    likes = likes,
)

private fun parsePublishedAt(value: String, now: Long): Long {
    if (value.isBlank()) return now
    return runCatching { Instant.parse(value).toEpochMilli() }.getOrElse { now }
}
