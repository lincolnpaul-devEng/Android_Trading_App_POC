package com.trading.app.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trading.app.presentation.theme.DarkBorder
import com.trading.app.presentation.theme.ProfitGreen
import com.trading.app.presentation.theme.TextPrimary
import com.trading.app.presentation.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class DonutWheelSegment(
    val id: String,
    val title: String,
    val sweepAngle: Float,
    val color: Color,
    val iconName: String? = null
)

/**
 * Replicates the signature Radial Donut Allocation Wheel from the reference image.
 * Features:
 * - Thick arc with segmented pills
 * - Top mini-pills with financial icons (Chart, Trend, Token)
 * - Arc labels ($66k, Profit, $120k)
 * - Large central bold figure ($186k)
 * - Radial atmospheric background glow
 */
@Composable
fun NexoraDonutWheelView(
    centerValue: String = "$186k",
    onSegmentClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animProgress.animateTo(1f, animationSpec = tween(900))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(310.dp),
        contentAlignment = Alignment.Center
    ) {
        // Soft atmospheric radial background aura matching reference
        Canvas(modifier = Modifier.size(260.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x2238BDF8),
                        Color(0x0C38BDF8),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.width * 0.5f
                )
            )
        }

        // Custom Canvas Donut Arc
        Canvas(modifier = Modifier.size(230.dp)) {
            val strokeW = 42.dp.toPx()
            val arcSize = Size(size.width - strokeW, size.height - strokeW)
            val topLeft = Offset(strokeW / 2f, strokeW / 2f)

            // Segment 1: Main Dark Slate/Navy Arc (Bottom-Right "Invested")
            // Sweeps from 0 deg (3 o'clock) clockwise by 170 deg
            drawArc(
                color = Color(0xFF132035),
                startAngle = 350f,
                sweepAngle = 175f * animProgress.value,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )

            // Segment 2: Light Sky Accent Arc (Left-Hand "Profit" & "$66k")
            // Sweeps from 175 deg to 300 deg
            drawArc(
                color = Color(0xFFE0EDFB),
                startAngle = 175f,
                sweepAngle = 125f * animProgress.value,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )

            // Segment 3: Deep Navy Gap Divider
            drawArc(
                color = Color(0xFF0F172A),
                startAngle = 305f,
                sweepAngle = 40f * animProgress.value,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
        }

        // Top mini-pill icon tabs positioned along the upper arc
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pill 1: Bar chart
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFE0EDFB))
                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.BarChart, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(16.dp))
            }
            // Pill 2: Trend arrow
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFE0EDFB))
                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(16.dp))
            }
            // Pill 3: Token B
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFE0EDFB))
                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("B", color = Color(0xFF0F172A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // "Invested" curved text pill on the right arc
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-32).dp, y = (-58).dp)
                .rotate(42f)
        ) {
            Text(
                text = "Invested",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        // "$66k" label on the left arc
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 36.dp, y = (-40).dp)
                .rotate(-70f)
        ) {
            Text(
                text = "\$66k",
                color = Color(0xFF0F172A),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // "Profit" label on the bottom-left arc
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 38.dp, y = 48.dp)
                .rotate(-35f)
        ) {
            Text(
                text = "Profit",
                color = Color(0xFF0F172A),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // "$120k" label at the bottom-left edge
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 75.dp, y = (-42).dp)
                .rotate(25f)
        ) {
            Text(
                text = "\$120k",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Large Center Value Figure
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerValue,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = (-0.5).sp
            )
        }
    }
}

/**
 * Asymmetric tabbed card shape replicating the Nexora design clip path:
 * - High left tab of width [tabWidthFraction] * width (holding "Invested" or "Withdraw")
 * - Fillet / S-curve transition of width [transitionWidth] dropping down to [dropHeight]
 * - Horizontal lower ledge running across to the right edge
 * - Smooth rounded corners:
 *   - Top-left (tab corner)
 *   - Top-right (lower body corner)
 *   - Bottom-right corner
 *   - Bottom-left corner
 */
class NexoraTabbedCardShape(
    private val tabWidthFraction: Float = 0.38f,
    private val dropHeight: Dp = 38.dp,
    private val transitionWidth: Dp = 46.dp,
    private val cornerRadius: Dp = 24.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val r = with(density) { cornerRadius.toPx() }
            val drop = with(density) { dropHeight.toPx() }
            val transW = with(density) { transitionWidth.toPx() }
            val tabW = size.width * tabWidthFraction

            // Start after top-left corner
            moveTo(r, 0f)

            // Flat top edge of the tab
            lineTo(tabW, 0f)

            // Smooth cubic Bezier S-curve transition down to dropHeight
            cubicTo(
                x1 = tabW + transW * 0.45f, y1 = 0f,
                x2 = tabW + transW * 0.55f, y2 = drop,
                x3 = tabW + transW, y3 = drop
            )

            // Lower horizontal top edge leading to the top-right corner
            lineTo(size.width - r, drop)

            // Top-right corner of the lower section
            arcTo(
                rect = Rect(size.width - 2 * r, drop, size.width, drop + 2 * r),
                startAngleDegrees = -90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // Right edge down to bottom-right corner
            lineTo(size.width, size.height - r)

            // Bottom-right corner
            arcTo(
                rect = Rect(size.width - 2 * r, size.height - 2 * r, size.width, size.height),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // Bottom edge to bottom-left corner
            lineTo(r, size.height)

            // Bottom-left corner
            arcTo(
                rect = Rect(0f, size.height - 2 * r, 2 * r, size.height),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // Left edge up to top-left corner
            lineTo(0f, r)

            // Top-left corner
            arcTo(
                rect = Rect(0f, 0f, 2 * r, 2 * r),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * Replicates the detailed position card from the Nexora design:
 * Features:
 * - Asymmetric tabbed clip path with high left tab and smooth S-curve drop
 * - Exposed dark navy header notch displaying the live time (16:00) and date (01-06-2026)
 * - Authentic Nexora sky-blue body with high-contrast slate text
 * - Green / performance badges (+4.73%, +$1,286 - Today)
 * - Bottom split: Total Invested : $96k | Profit : $16k
 */
@Composable
fun NexoraDetailedPositionCard(
    tag: String = "Invested",
    timeStr: String = "16:00",
    dateStr: String = "01-06-2026",
    symbolTitle: String = "Coinbase Global (COIN)",
    pctReturn: String = "+4.73%",
    dollarReturn: String = "+$1,286 - Today",
    totalInvested: String = "$96k",
    profit: String = "$16k",
    cardColor: Color = Color(0xFF6DA5E3),
    containerBg: Color = Color(0xFF0C2449),
    modifier: Modifier = Modifier
) {
    val cornerRadius = 26.dp
    val dropHeight = 38.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .background(containerBg)
    ) {
        // Exposed header notch in top-right displaying time and date
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = timeStr,
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = dateStr,
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Foreground card with the exact asymmetric tabbed clip path
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(
                    NexoraTabbedCardShape(
                        tabWidthFraction = 0.38f,
                        dropHeight = dropHeight,
                        transitionWidth = 46.dp,
                        cornerRadius = 24.dp
                    )
                )
                .background(cardColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 18.dp)
            ) {
                // Tab title ("Invested" / "Withdraw")
                Text(
                    text = tag,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 18.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Inner content
                Column(modifier = Modifier.padding(horizontal = 18.dp)) {
                    Text(
                        text = symbolTitle,
                        color = Color(0xFF0F172A),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Performance badges
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x30FFFFFF))
                                .border(1.dp, Color(0x35FFFFFF), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = pctReturn,
                                    color = Color(0xFF0F172A),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x30FFFFFF))
                                .border(1.dp, Color(0x35FFFFFF), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = dollarReturn,
                                color = Color(0xFF0F172A),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Thin subtle divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0x250F172A))
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Metrics footer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total Invested : $totalInvested",
                            color = Color(0xFF0F172A),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = "Profit : $profit",
                            color = Color(0xFF0F172A),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
