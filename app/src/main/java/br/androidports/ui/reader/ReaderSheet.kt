package br.androidports.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.androidports.domain.NewsItem
import br.androidports.domain.categoryLabel
import br.androidports.ui.common.compactCount
import br.androidports.ui.common.openSource
import br.androidports.ui.common.relativeTime
import br.androidports.ui.common.shareNews
import br.androidports.ui.components.ActionPill
import br.androidports.ui.components.GlassSurface
import br.androidports.ui.theme.PortsTheme
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSheet(
    item: NewsItem,
    onDismiss: () -> Unit,
    onLike: () -> Unit,
    onSave: () -> Unit,
) {
    val colors = PortsTheme.colors
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val paragraphs = remember(item.id, item.content) {
        item.content.split("\n\n").map { it.trim() }.filter { it.isNotBlank() }
    }
    val imageRequest = remember(item.imageUrl) {
        ImageRequest.Builder(context)
            .data(item.imageUrl)
            .crossfade(true)
            .build()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        contentColor = colors.textPrimary,
        scrimColor = colors.scrim,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 38.dp, height = 4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colors.textTertiary.copy(alpha = 0.55f)),
            )
        },
    ) {
        Column(Modifier.fillMaxHeight(0.93f)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(220.dp),
            ) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = ColorPainter(colors.surfaceElevated),
                    error = ColorPainter(colors.surfaceElevated),
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.55f to Color.Transparent,
                                1f to colors.surface,
                            ),
                        ),
                )
                GlassSurface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(44.dp),
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.42f),
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Fechar leitor",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = 22.dp,
                    end = 22.dp,
                    top = 2.dp,
                    bottom = 20.dp,
                ),
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(colors.accent),
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = categoryLabel(item.category).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textSecondary,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "· ${relativeTime(item.publishedAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textTertiary,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.textPrimary,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = item.summary,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textSecondary,
                    )
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = colors.hairline)
                    Spacer(Modifier.height(16.dp))
                }

                itemsIndexed(paragraphs) { index, paragraph ->
                    Text(
                        text = paragraph,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textPrimary.copy(alpha = 0.92f),
                    )
                    Spacer(Modifier.height(if (index == paragraphs.lastIndex) 8.dp else 16.dp))
                }

                item {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Fonte: ${item.sourceName.ifBlank { "site original" }}",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textTertiary,
                    )
                    Spacer(Modifier.height(10.dp))
                    GlassSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = item.sourceUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                imageVector = Icons.Outlined.OpenInNew,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                }
            }

            HorizontalDivider(color = colors.hairline)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ActionPill(
                    icon = if (item.liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    label = compactCount(item.likes + if (item.liked) 1 else 0),
                    onClick = onLike,
                    active = item.liked,
                )
                ActionPill(
                    icon = if (item.saved) Icons.Filled.Bookmark else Icons.Outlined.Bookmark,
                    label = if (item.saved) "Salvo" else "Salvar",
                    onClick = onSave,
                    active = item.saved,
                )
                ActionPill(
                    icon = Icons.Outlined.Share,
                    label = "Enviar",
                    onClick = { shareNews(context, item) },
                )
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(colors.accent)
                        .clickable { openSource(context, item) }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Abrir fonte",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Outlined.OpenInNew,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}
