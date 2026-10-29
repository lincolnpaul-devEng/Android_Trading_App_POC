package com.trading.app.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.trading.app.presentation.theme.DarkBorder

/**
 * YouTube-style linear animated shimmer gradient modifier
 */
fun Modifier.shimmerEffect(
    baseColor: Color = Color(0xFF161B22),
    highlightColor: Color = Color(0xFF282F3B),
    durationMillis: Int = 1200
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim = transition.animateFloat(
        initialValue = -300f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val brush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(x = translateAnim.value - 300f, y = translateAnim.value - 300f),
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    this.background(brush)
}

/**
 * YouTube-style news item skeleton loader
 */
@Composable
fun NewsRowSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Line 1: Source & timestamp placeholder
        Box(
            modifier = Modifier
                .width(130.dp)
                .height(11.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect()
        )

        // Middle Row: Text column on left, preview thumbnail on right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Title line 1
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )

                // Title line 2
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Snippet line
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(11.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )

                // Ticker chips placeholder
                Box(
                    modifier = Modifier
                        .width(160.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
            }

            // Right Thumbnail placeholder
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .shimmerEffect()
            )
        }

        HorizontalDivider(
            color = DarkBorder.copy(alpha = 0.5f),
            thickness = 0.5.dp,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}

@Composable
fun NewsShimmerList(
    count: Int = 4,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(count) {
            NewsRowSkeleton()
        }
    }
}
