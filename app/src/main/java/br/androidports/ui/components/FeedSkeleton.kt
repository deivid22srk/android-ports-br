package br.androidports.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import br.androidports.ui.theme.LocalReduceMotion
import br.androidports.ui.theme.PortsTheme

@Composable
fun rememberShimmerBrush(): Brush {
    val colors = PortsTheme.colors
    val reduceMotion = LocalReduceMotion.current
    if (reduceMotion) return remember(colors.surfaceElevated) {
        SolidColor(colors.surfaceElevated.copy(alpha = 0.7f))
    }

    val transition = rememberInfiniteTransition(label = "shimmer")
    val offset by transition.animateFloat(
        initialValue = -500f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerOffset",
    )

    return remember(colors.surfaceElevated, offset) {
        Brush.linearGradient(
            colors = listOf(
                colors.surfaceElevated.copy(alpha = 0.45f),
                colors.surfaceElevated,
                colors.surfaceElevated.copy(alpha = 0.45f),
            ),
            start = Offset(offset, 0f),
            end = Offset(offset + 420f, 420f),
        )
    }
}

@Composable
fun FeedSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    val colors = PortsTheme.colors

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(colors.surfaceElevated.copy(alpha = 0.5f), colors.background),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 20.dp, end = 84.dp, bottom = 150.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(width = 86.dp, height = 22.dp)
                    .clip(RoundedCornerShape(50))
                    .background(brush),
            )
            Box(
                Modifier
                    .fillMaxWidth(0.92f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(brush),
            )
            Box(
                Modifier
                    .fillMaxWidth(0.7f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(brush),
            )
            Spacer(Modifier.height(2.dp))
            Box(
                Modifier
                    .fillMaxWidth(0.95f)
                    .height(15.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(brush),
            )
            Box(
                Modifier
                    .fillMaxWidth(0.55f)
                    .height(15.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(brush),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 150.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            repeat(4) {
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(50))
                        .background(brush),
                )
            }
        }
    }
}
