package com.trading.app.presentation.learn.flowbuilder

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trading.app.presentation.theme.DarkBorder

/**
 * Interactive Sankey Canvas Diagram for the Money Flow Builder.
 * Renders dynamic ribbon paths where stroke width directly equals allocated dollars,
 * smoothly animating between allocation changes without any text clipping or squeezing.
 */
@Composable
fun MoneyFlowCanvas(
    state: MoneyFlowState,
    blocks: List<MoneyBlock>,
    modifier: Modifier = Modifier
) {
    // Collect animated values for all blocks
    val animatedAmounts = blocks.associate { block ->
        val target = (state.allocations[block.id] ?: 0).toFloat()
        val anim by animateFloatAsState(
            targetValue = target,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
            label = "amt_${block.id}"
        )
        block.id to anim
    }

    val unassignedTarget = state.unassignedDollars.coerceAtLeast(0).toFloat()
    val animatedUnassigned by animateFloatAsState(
        targetValue = unassignedTarget,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "amt_unassigned"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF14202E),
                        Color(0xFF161E28)
                    )
                )
            )
            .border(1.2.dp, Color(0xFF2E4057), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(235.dp)
        ) {
            val totalIncome = state.totalIncome.toFloat()
            val availableHeight = size.height - 38.dp.toPx()
            val scale = availableHeight / totalIncome

            val lx = 4.dp.toPx()
            val lw = 14.dp.toPx()
            // Shift right node column slightly to the left (45% width) so long labels have 55% of the screen
            val rx = size.width * 0.44f
            val rw = 12.dp.toPx()
            val topY = 24.dp.toPx()
            val minNodeH = 13.dp.toPx()
            val gap = 6.dp.toPx()
            val mx = (lx + lw + rx) / 2f

            // 1. Draw Left Income Column & Label
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#8B949E")
                    textSize = 10.5.sp.toPx()
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.MONOSPACE
                }
                drawText("INCOME $${state.totalIncome}", lx, 14.dp.toPx(), paint)
            }

            // Income vertical bar
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF5FE2D6),
                        Color(0xFF6DE7C2),
                        Color(0xFF38B291)
                    )
                ),
                topLeft = Offset(lx, topY),
                size = Size(lw, totalIncome * scale),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            // Deficit indicator if over-allocated
            val totalAllocated = state.totalAllocated
            if (totalAllocated > state.totalIncome) {
                val over = (totalAllocated - state.totalIncome).toFloat()
                val overH = over * scale
                drawRoundRect(
                    color = Color(0xFFF85149),
                    topLeft = Offset(lx, topY + totalIncome * scale + 2.dp.toPx()),
                    size = Size(lw, overH),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )

                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#F85149")
                        textSize = 10.sp.toPx()
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                    drawText("Short $$over", lx, topY + totalIncome * scale + overH + 13.dp.toPx(), paint)
                }
            }

            // 2. Build list of placed items
            val placedItems = mutableListOf<FlowRenderItem>()
            blocks.forEach { b ->
                val v = animatedAmounts[b.id] ?: 0f
                if (state.isPlaced(b.id) || v > 0.5f) {
                    placedItems.add(
                        FlowRenderItem(
                            id = b.id,
                            name = b.name,
                            amount = v,
                            color = b.color,
                            isUnassigned = false
                        )
                    )
                }
            }

            if (animatedUnassigned > 0.5f) {
                placedItems.add(
                    FlowRenderItem(
                        id = "unassigned",
                        name = "Unassigned",
                        amount = animatedUnassigned,
                        color = Color(0xFF58657B),
                        isUnassigned = true
                    )
                )
            }

            // 3. Draw Ribbons & Destination Nodes
            var cumIncomeY = 0f
            var currRightY = topY

            placedItems.forEach { item ->
                val ribbonWidth = (item.amount * scale).coerceAtLeast(0.5f)
                val nodeH = (item.amount * scale).coerceAtLeast(minNodeH)
                val y0 = topY + cumIncomeY + ribbonWidth / 2f
                val cy = currRightY + nodeH / 2f

                if (ribbonWidth > 0.6f) {
                    val path = Path().apply {
                        moveTo(lx + lw, y0)
                        cubicTo(mx, y0, mx, cy, rx, cy)
                    }

                    // Ribbon Stroke with smooth alpha
                    drawPath(
                        path = path,
                        color = if (item.isUnassigned) item.color.copy(alpha = 0.40f) else item.color.copy(alpha = 0.58f),
                        style = Stroke(
                            width = ribbonWidth,
                            pathEffect = if (item.isUnassigned) PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f) else null
                        )
                    )
                }

                // Destination Node
                drawRoundRect(
                    color = item.color,
                    topLeft = Offset(rx, currRightY),
                    size = Size(rw, nodeH),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )

                // Label & Amount with clean horizontal space
                drawContext.canvas.nativeCanvas.apply {
                    val namePaint = android.graphics.Paint().apply {
                        color = if (item.isUnassigned) android.graphics.Color.parseColor("#8B949E") else android.graphics.Color.WHITE
                        textSize = 12.sp.toPx()
                        isAntiAlias = true
                        typeface = if (item.isUnassigned) android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC) else android.graphics.Typeface.DEFAULT_BOLD
                    }

                    // Safely truncate name if necessary (never wrap vertically)
                    val maxNameChars = 14
                    val displayName = if (item.name.length > maxNameChars) item.name.take(maxNameChars - 2) + ".." else item.name
                    drawText(displayName, rx + rw + 8.dp.toPx(), cy + 4.dp.toPx(), namePaint)

                    val amtPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#E6EDF3")
                        textSize = 11.5.sp.toPx()
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.MONOSPACE
                        textAlign = android.graphics.Paint.Align.RIGHT
                    }
                    drawText("$${item.amount.toInt()}", size.width - 2.dp.toPx(), cy + 4.dp.toPx(), amtPaint)
                }

                cumIncomeY += ribbonWidth
                currRightY += nodeH + gap
            }

            // 4. Empty State Hint if nothing placed yet in build step
            if (placedItems.isEmpty() && state.stage == FlowStage.BUILD) {
                val boxW = 190.dp.toPx()
                val boxH = 40.dp.toPx()
                val cx = size.width * 0.52f
                val cy = size.height * 0.50f

                drawRoundRect(
                    color = Color(0xFF38475C),
                    topLeft = Offset(cx - boxW / 2f, cy - boxH / 2f),
                    size = Size(boxW, boxH),
                    cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f))
                )

                drawContext.canvas.nativeCanvas.apply {
                    val hintPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#5FE2D6")
                        textSize = 12.sp.toPx()
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                    drawText("Tap blocks below to add", cx, cy + 4.5.dp.toPx(), hintPaint)
                }
            }
        }
    }
}

private data class FlowRenderItem(
    val id: String,
    val name: String,
    val amount: Float,
    val color: Color,
    val isUnassigned: Boolean
)
