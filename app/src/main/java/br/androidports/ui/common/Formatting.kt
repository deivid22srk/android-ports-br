package br.androidports.ui.common

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ptBr = Locale.forLanguageTag("pt-BR")
private val shortDate = DateTimeFormatter.ofPattern("dd MMM", ptBr)

fun relativeTime(epochMillis: Long, now: Long = System.currentTimeMillis()): String {
    val diff = (now - epochMillis).coerceAtLeast(0L)
    val minutes = diff / 60_000L
    return when {
        minutes < 1 -> "agora"
        minutes < 60 -> "há $minutes min"
        minutes < 1_440 -> "há ${minutes / 60} h"
        minutes < 10_080 -> "há ${minutes / 1_440} d"
        else -> shortDate.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
    }
}

fun compactCount(value: Int): String = when {
    value >= 1_000_000 -> String.format(Locale.US, "%.1f", value / 1_000_000.0)
        .replace(".0", "") + "M"
    value >= 1_000 -> String.format(Locale.US, "%.1f", value / 1_000.0)
        .replace(".0", "") + " mil"
    else -> value.toString()
}
