package br.androidports.ui.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import br.androidports.domain.NewsItem

fun shareNews(context: Context, item: NewsItem) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, item.title)
        putExtra(Intent.EXTRA_TEXT, "${item.title}\n\n${item.sourceUrl}")
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "Compartilhar notícia"))
    }
}

fun openSource(context: Context, item: NewsItem) {
    if (item.sourceUrl.isBlank()) return
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.sourceUrl)))
    }
}
