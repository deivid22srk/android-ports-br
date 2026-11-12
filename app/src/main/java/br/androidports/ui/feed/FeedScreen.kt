package br.androidports.ui.feed

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.androidports.data.repository.NewsRepository
import br.androidports.domain.DefaultCategories
import br.androidports.domain.FeedFilter
import br.androidports.domain.NewsItem
import br.androidports.domain.categoryLabel
import br.androidports.ui.common.compactCount
import br.androidports.ui.common.openSource
import br.androidports.ui.common.relativeTime
import br.androidports.ui.common.shareNews
import br.androidports.ui.components.CategoryChip
import br.androidports.ui.components.EmptyState
import br.androidports.ui.components.FeedAction
import br.androidports.ui.components.FeedSkeleton
import br.androidports.ui.components.OfflinePill
import br.androidports.ui.reader.ReaderSheet
import br.androidports.ui.theme.LocalHapticsEnabled
import br.androidports.ui.theme.PortsTheme
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    repository: NewsRepository,
    feedFilter: FeedFilter,
    modifier: Modifier = Modifier,
) {
    val viewModel: FeedViewModel = viewModel(factory = FeedViewModel.factory(repository, feedFilter))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = PortsTheme.colors
    val context = LocalContext.current
    val items = state.items
    val pagerState = rememberPagerState(pageCount = { items.size })

    LaunchedEffect(state.selectedCategory) {
        runCatching { pagerState.scrollToPage(0) }
    }

    LaunchedEffect(pagerState.currentPage, items.size) {
        if (items.isNotEmpty() && pagerState.currentPage >= items.lastIndex - 1) {
            viewModel.loadMore()
        }
        items.getOrNull(pagerState.currentPage + 1)?.let { next ->
            preloadImage(context, next.imageUrl)
        }
    }

    Box(modifier.fillMaxSize().background(colors.background)) {
        when {
            state.isLoading && items.isEmpty() -> FeedSkeleton()

            items.isEmpty() -> EmptyState(
                icon = Icons.Outlined.Newspaper,
                title = "Nada por aqui",
                message = state.error ?: "Não encontramos notícias nesta categoria. Tente outra ou atualize o feed.",
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                actionLabel = "Tentar novamente",
                onAction = viewModel::refresh,
            )

            else -> {
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    VerticalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 1,
                        key = { index -> items.getOrNull(index)?.id ?: index },
                    ) { page ->
                        val item = items.getOrNull(page) ?: return@VerticalPager
                        FeedPage(
                            item = item,
                            onOpen = { viewModel.openReader(item.id) },
                            onLike = { viewModel.toggleLike(item.id) },
                            onSave = { viewModel.toggleSaved(item.id) },
                            onShare = { shareNews(context, item) },
                            onOpenSource = { openSource(context, item) },
                        )
                    }
                }

                FeedTopOverlay(
                    pagerState = pagerState,
                    count = items.size,
                    selectedCategory = state.selectedCategory,
                    offline = state.offline,
                    onSelectCategory = viewModel::selectCategory,
                    onRefresh = viewModel::refresh,
                )
            }
        }

        val reader = state.readerId?.let { id -> items.firstOrNull { it.id == id } }
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
private fun FeedTopOverlay(
    pagerState: PagerState,
    count: Int,
    selectedCategory: String?,
    offline: Boolean,
    onSelectCategory: (String?) -> Unit,
    onRefresh: () -> Unit,
) {
    val colors = PortsTheme.colors

    Box(
        Modifier
            .fillMaxWidth()
            .height(230.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.82f),
                        Color.Black.copy(alpha = 0.28f),
                        Color.Transparent,
                    ),
                ),
            ),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .drawBehind {
                    drawRect(colors.hairline)
                    if (count > 0) {
                        val fraction = (
                            (pagerState.currentPage + pagerState.currentPageOffsetFraction + 1f) / count
                            ).coerceIn(0f, 1f)
                        drawRect(
                            color = colors.accent,
                            size = Size(size.width * fraction, size.height),
                        )
                    }
                },
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "ANDROID PORTS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 15.sp,
                        letterSpacing = 2.1.sp,
                    ),
                    color = Color.White,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "BR",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 15.sp,
                        letterSpacing = 2.1.sp,
                    ),
                    color = colors.accent,
                )
            }
            Spacer(Modifier.weight(1f))
            if (offline) {
                OfflinePill(onClick = onRefresh)
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 14.dp),
        ) {
            item {
                CategoryChip(
                    label = "Para você",
                    selected = selectedCategory == null,
                    onClick = { onSelectCategory(null) },
                )
            }
            items(DefaultCategories, key = { it.id }) { category ->
                CategoryChip(
                    label = category.label,
                    selected = selectedCategory == category.id,
                    onClick = { onSelectCategory(category.id) },
                )
            }
        }
    }
}

@Composable
private fun FeedPage(
    item: NewsItem,
    onOpen: () -> Unit,
    onLike: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onOpenSource: () -> Unit,
) {
    val colors = PortsTheme.colors
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val hapticsEnabled = LocalHapticsEnabled.current
    val currentOnOpen by rememberUpdatedState(onOpen)
    val currentOnLike by rememberUpdatedState(onLike)
    val currentLiked by rememberUpdatedState(item.liked)

    var burstKey by remember { mutableIntStateOf(0) }
    var burstOffset by remember { mutableStateOf(Offset.Zero) }

    val imageRequest = remember(item.id, item.imageUrl) {
        ImageRequest.Builder(context)
            .data(item.imageUrl)
            .crossfade(true)
            .build()
    }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(item.id) {
                detectTapGestures(
                    onTap = { currentOnOpen() },
                    onDoubleTap = { offset ->
                        burstOffset = offset
                        burstKey += 1
                        if (hapticsEnabled) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        if (!currentLiked) currentOnLike()
                    },
                )
            },
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .background(colors.surfaceElevated),
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
                        0.36f to Color.Transparent,
                        0.66f to Color.Black.copy(alpha = 0.55f),
                        1f to Color.Black.copy(alpha = 0.97f),
                    ),
                ),
        )

        // Brilho decorativo com blur real (API 31+); abaixo disso o Modifier.blur é inerte.
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 70.dp, y = (-30).dp)
                .size(230.dp)
                .background(colors.accent.copy(alpha = 0.22f), CircleShape)
                .blur(72.dp),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 92.dp, bottom = 104.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(colors.accent),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = categoryLabel(item.category).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.92f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = item.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.78f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "${item.sourceName.ifBlank { "Fonte" }} · ${relativeTime(item.publishedAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.62f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 10.dp, bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            FeedAction(
                icon = if (item.liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                label = compactCount(item.likes + if (item.liked) 1 else 0),
                onClick = onLike,
                active = item.liked,
            )
            FeedAction(
                icon = if (item.saved) Icons.Filled.Bookmark else Icons.Outlined.Bookmark,
                label = if (item.saved) "Salvo" else "Salvar",
                onClick = onSave,
                active = item.saved,
            )
            FeedAction(
                icon = Icons.Outlined.Share,
                label = "Enviar",
                onClick = onShare,
            )
            FeedAction(
                icon = Icons.Outlined.OpenInNew,
                label = "Fonte",
                onClick = onOpenSource,
            )
        }

        if (burstKey > 0) {
            LikeBurst(trigger = burstKey, position = burstOffset)
        }
    }
}

@Composable
private fun LikeBurst(trigger: Int, position: Offset) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    val colors = PortsTheme.colors
    val iconSizePx = with(LocalDensity.current) { 92.dp.toPx() }

    LaunchedEffect(trigger) {
        scale.snapTo(0.45f)
        alpha.snapTo(1f)
        scale.animateTo(
            targetValue = 1.2f,
            animationSpec = spring(dampingRatio = 0.44f, stiffness = 380f),
        )
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        )
    }

    if (alpha.value > 0f) {
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (position.x - iconSizePx / 2f).roundToInt(),
                        y = (position.y - iconSizePx / 2f).roundToInt(),
                    )
                }
                .size(92.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    this.alpha = alpha.value
                },
        )
    }
}

private fun preloadImage(context: Context, url: String) {
    if (url.isBlank()) return
    SingletonImageLoader.get(context).enqueue(
        ImageRequest.Builder(context)
            .data(url)
            .size(1080, 1920)
            .crossfade(true)
            .build(),
    )
}
