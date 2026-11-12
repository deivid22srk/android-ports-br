package br.androidports.ui.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.androidports.data.repository.NewsRepository
import br.androidports.domain.NewsItem
import br.androidports.ui.components.EmptyState
import br.androidports.ui.components.NewsRow
import br.androidports.ui.components.ScreenHeader
import br.androidports.ui.components.SectionLabel
import br.androidports.ui.components.rememberShimmerBrush
import br.androidports.ui.reader.ReaderSheet
import br.androidports.ui.theme.PortsTheme

@Composable
fun SavedScreen(
    repository: NewsRepository,
    onExplore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: SavedViewModel = viewModel(factory = SavedViewModel.factory(repository))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = PortsTheme.colors
    val reader = state.readerId?.let { id -> state.items.firstOrNull { it.id == id } }

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
        ) {
            item {
                ScreenHeader(
                    title = "Salvos",
                    subtitle = if (state.items.isEmpty()) {
                        "Sua biblioteca fica disponível mesmo sem internet."
                    } else {
                        "${state.items.size} notícias salvas para ler offline."
                    },
                )
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(colors.accent),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = "DISPONÍVEL OFFLINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textTertiary,
                    )
                }
                Spacer(Modifier.height(14.dp))
            }

            when {
                state.isLoading -> {
                    items(4) { SavedSkeletonRow() }
                }

                state.items.isEmpty() -> {
                    item {
                        EmptyState(
                            icon = Icons.Outlined.Bookmark,
                            title = "Nada salvo ainda",
                            message = "Toque no marcador de uma notícia no feed para guardar aqui e ler quando quiser.",
                            modifier = Modifier.padding(vertical = 32.dp),
                            actionLabel = "Explorar notícias",
                            onAction = onExplore,
                        )
                    }
                }

                else -> {
                    item {
                        SectionLabel("Sua biblioteca") {
                            Text(
                                text = state.items.size.toString().padStart(2, '0'),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textTertiary,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    items(state.items, key = { it.id }) { item ->
                        SavedSwipeItem(
                            item = item,
                            onClick = { viewModel.openReader(item) },
                            onRemove = { viewModel.remove(item) },
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
private fun SavedSwipeItem(
    item: NewsItem,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = PortsTheme.colors
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                onRemove()
                true
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = Modifier.padding(vertical = 2.dp),
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp))
                    .background(colors.accentSoft)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Remover",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.accent,
                    )
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        },
    ) {
        NewsRow(
            item = item,
            onClick = onClick,
            modifier = Modifier.background(colors.background),
        )
    }
}

@Composable
private fun SavedSkeletonRow() {
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
        androidx.compose.foundation.layout.Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(0.9f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(brush),
            )
            Box(
                Modifier
                    .fillMaxWidth(0.55f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(brush),
            )
        }
    }
}
