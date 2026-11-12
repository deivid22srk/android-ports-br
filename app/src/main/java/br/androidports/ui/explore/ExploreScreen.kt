package br.androidports.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.androidports.data.repository.NewsRepository
import br.androidports.domain.CategoryCount
import br.androidports.domain.NewsItem
import br.androidports.ui.components.EmptyState
import br.androidports.ui.components.GlassSurface
import br.androidports.ui.components.NewsRow
import br.androidports.ui.components.ScreenHeader
import br.androidports.ui.components.SectionLabel
import br.androidports.ui.components.rememberShimmerBrush
import br.androidports.ui.reader.ReaderSheet
import br.androidports.ui.theme.PortsTheme
import br.androidports.ui.theme.Spacing

@Composable
fun ExploreScreen(
    repository: NewsRepository,
    onOpenCategory: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ExploreViewModel = viewModel(factory = ExploreViewModel.factory(repository))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = PortsTheme.colors
    val context = LocalContext.current

    val reader = state.readerId?.let { id ->
        state.trending.firstOrNull { it.id == id } ?: state.results.firstOrNull { it.id == id }
    }

    Box(modifier.fillMaxSize().background(colors.background)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 18.dp,
                bottom = 128.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                ScreenHeader(
                    title = "Explorar",
                    subtitle = "Ports, ROMs, apps e dicas em um só lugar.",
                )
            }

            item {
                SearchField(
                    query = state.query,
                    onQueryChange = viewModel::onQueryChange,
                    onClear = { viewModel.onQueryChange("") },
                )
            }

            if (state.offline) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp),
                    ) {
                        Box(
                            Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(colors.accent),
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = "Servidor indisponível — usando o cache local.",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textTertiary,
                        )
                    }
                }
            }

            if (state.query.isNotBlank()) {
                item {
                    SectionLabel("Resultados") {
                        Text(
                            text = state.results.size.toString().padStart(2, '0'),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textTertiary,
                        )
                    }
                }

                if (state.isSearching) {
                    items(3) {
                        SearchSkeletonRow()
                    }
                } else if (state.results.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Outlined.Search,
                            title = "Nenhum resultado",
                            message = "Tente outro termo. A busca considera título, resumo e conteúdo.",
                            modifier = Modifier.padding(vertical = 24.dp),
                        )
                    }
                } else {
                    items(state.results, key = { it.id }) { item ->
                        NewsRow(item = item, onClick = { viewModel.openReader(item) })
                    }
                }
            } else {
                item {
                    SectionLabel("Categorias")
                }
                items(
                    items = state.categories.chunked(2),
                    key = { row -> row.first().id },
                ) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        row.forEach { category ->
                            CategoryCard(
                                category = category,
                                onClick = { onOpenCategory(category.id) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }

                item {
                    Spacer(Modifier.height(10.dp))
                    SectionLabel("Em alta") {
                        Icon(
                            imageVector = Icons.Outlined.TrendingUp,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                if (state.isLoading && state.trending.isEmpty()) {
                    items(4) { SearchSkeletonRow() }
                } else {
                    itemsIndexed(state.trending, key = { _, item -> item.id }) { index, item ->
                        NewsRow(
                            item = item,
                            rank = index + 1,
                            showSummary = false,
                            onClick = { viewModel.openReader(item) },
                        )
                    }
                }
            }
        }

        if (reader != null) {
            ReaderSheet(
                item = reader,
                onDismiss = viewModel::closeReader,
                onLike = { viewModel.toggleLike(reader.id) },
                onSave = { viewModel.toggleSaved(reader.id) },
            )
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    val colors = PortsTheme.colors
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Buscar notícias, ROMs, apps…",
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.textTertiary,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            if (query.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Limpar busca",
                    tint = colors.textSecondary,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = onClear),
                )
            }
        }
    }
}

@Composable
private fun CategoryCard(
    category: CategoryCount,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PortsTheme.colors
    GlassSurface(
        modifier = modifier.height(106.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        contentPadding = PaddingValues(16.dp),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(colors.accent),
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = category.label,
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary,
                maxLines = 1,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${category.count} notícias",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textTertiary,
            )
        }
    }
}

@Composable
private fun SearchSkeletonRow() {
    val colors = PortsTheme.colors
    val brush = rememberShimmerBrush()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(78.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(brush),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .fillMaxWidth(0.9f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(brush),
            )
            Box(
                Modifier
                    .fillMaxWidth(0.6f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(brush),
            )
        }
    }
}
