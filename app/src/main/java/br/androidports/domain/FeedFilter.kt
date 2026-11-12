package br.androidports.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Canal simples entre Explorar e Feed para abrir uma categoria específica
 * sem recriar a tela do feed.
 */
class FeedFilter {
    private val _category = MutableStateFlow<String?>(null)
    val category: StateFlow<String?> = _category.asStateFlow()

    fun select(id: String?) {
        _category.value = id
    }
}
