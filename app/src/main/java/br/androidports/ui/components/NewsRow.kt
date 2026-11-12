package br.androidports.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.androidports.domain.NewsItem
import br.androidports.ui.common.relativeTime
import br.androidports.ui.theme.PortsTheme
import coil3.compose.AsyncImage

@Composable
fun NewsRow(
    item: NewsItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    rank: Int? = null,
    showSummary: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = PortsTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (rank != null) {
            Text(
                text = rank.toString().padStart(2, '0'),
                style = MaterialTheme.typography.headlineMedium,
                color = if (rank <= 3) colors.accent else colors.textTertiary,
                modifier = Modifier.width(38.dp),
            )
        }
        AsyncImage(
            model = item.imageUrl,
            contentDescription = null,
            modifier = Modifier
                .size(78.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.surfaceElevated),
            contentScale = ContentScale.Crop,
            placeholder = ColorPainter(colors.surfaceElevated),
            error = ColorPainter(colors.surfaceElevated),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (showSummary && item.summary.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = item.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(5.dp))
            Text(
                text = "${item.sourceName.ifBlank { "Fonte" }} · ${relativeTime(item.publishedAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                trailing()
            }
        }
    }
}
