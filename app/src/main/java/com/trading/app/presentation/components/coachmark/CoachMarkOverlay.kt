package com.trading.app.presentation.components.coachmark

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class CoachMarkStep(
    val id: String,
    val headline: String,
    val bodyBullets: List<String>,
    val targetBounds: Rect? = null,
    val arrowOrientation: ArrowOrientation = ArrowOrientation.LOOP_DOWN_RIGHT,
    val buttonText: String = "Okay, Got It!"
)

enum class ArrowOrientation {
    LOOP_DOWN_RIGHT,
    LOOP_DOWN_LEFT,
    LOOP_UP_RIGHT,
    SIMPLE_DOWN
}

@Composable
fun CoachMarkOverlay(
    visible: Boolean,
    currentStep: CoachMarkStep,
    stepIndex: Int,
    totalSteps: Int,
    onNext: () -> Unit,
    onDismissAll: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onNext
                )
        ) {
            // 1. Dark Scrim with Spotlight Hole Cutout
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(alpha = 0.99f) // Required for BlendMode.Clear hole
            ) {
                // Dim entire screen with 88% black scrim
                drawRect(Color(0xE005080E))

                // Cut out spotlight aperture over target if available
                currentStep.targetBounds?.let { bounds ->
                    val expanded = Rect(
                        left = bounds.left - 8.dp.toPx(),
                        top = bounds.top - 6.dp.toPx(),
                        right = bounds.right + 8.dp.toPx(),
                        bottom = bounds.bottom + 6.dp.toPx()
                    )
                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = Offset(expanded.left, expanded.top),
                        size = Size(expanded.width, expanded.height),
                        cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                        blendMode = BlendMode.Clear
                    )
                    // Spotlight glowing border
                    drawRoundRect(
                        color = Color(0xFFFF6D00),
                        topLeft = Offset(expanded.left, expanded.top),
                        size = Size(expanded.width, expanded.height),
                        cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            // 2. Curled Hand-Drawn Arrow Canvas
            CurledArrowCanvas(
                targetBounds = currentStep.targetBounds,
                orientation = currentStep.arrowOrientation,
                arrowColor = Color(0xFFFF6D00)
            )

            // 3. Educational Content Card & Dismiss Button
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Headline
                    Text(
                        text = currentStep.headline,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 28.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Concise Short Sentences
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        currentStep.bodyBullets.forEach { sentence ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 6.dp)
                                        .size(6.dp)
                                        .background(Color(0xFFFF6D00), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = sentence,
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 21.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Elevated Swiggy-style Pill Action Button
                    Button(
                        onClick = onNext,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF0F172A)
                        ),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = if (stepIndex < totalSteps - 1) currentStep.buttonText else "Get Started",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Step indicator dots
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until totalSteps) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(if (i == stepIndex) 8.dp else 6.dp)
                                    .background(
                                        if (i == stepIndex) Color(0xFFFF6D00) else Color(0x66FFFFFF),
                                        CircleShape
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Custom Canvas drawing the exact signature curled loop arrow with arrowhead.
 */
@Composable
private fun CurledArrowCanvas(
    targetBounds: Rect?,
    orientation: ArrowOrientation,
    arrowColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Default or target-based anchor endpoints
        val targetX = targetBounds?.center?.x ?: (w * 0.5f)
        val targetY = (targetBounds?.top?.minus(12.dp.toPx())) ?: (h * 0.36f)

        val path = Path()

        when (orientation) {
            ArrowOrientation.LOOP_DOWN_RIGHT -> {
                // Starts near the text (mid-screen), loops up and swoops down to target
                val startX = targetX - 70.dp.toPx()
                val startY = targetY - 60.dp.toPx()

                path.moveTo(startX, startY)
                // First curl: loops rightward and upward
                path.cubicTo(
                    startX + 35.dp.toPx(), startY - 25.dp.toPx(),
                    startX + 65.dp.toPx(), startY + 20.dp.toPx(),
                    startX + 40.dp.toPx(), startY + 45.dp.toPx()
                )
                // Second curl: loops inward and arcs down toward target
                path.cubicTo(
                    startX + 18.dp.toPx(), startY + 60.dp.toPx(),
                    startX + 15.dp.toPx(), targetY - 20.dp.toPx(),
                    targetX, targetY
                )
                drawCurledPathWithArrowhead(path, targetX, targetY, angleDeg = 90f, arrowColor = arrowColor)
            }
            ArrowOrientation.LOOP_DOWN_LEFT -> {
                val startX = targetX + 70.dp.toPx()
                val startY = targetY - 60.dp.toPx()

                path.moveTo(startX, startY)
                path.cubicTo(
                    startX - 35.dp.toPx(), startY - 25.dp.toPx(),
                    startX - 65.dp.toPx(), startY + 20.dp.toPx(),
                    startX - 40.dp.toPx(), startY + 45.dp.toPx()
                )
                path.cubicTo(
                    startX - 18.dp.toPx(), startY + 60.dp.toPx(),
                    startX - 15.dp.toPx(), targetY - 20.dp.toPx(),
                    targetX, targetY
                )
                drawCurledPathWithArrowhead(path, targetX, targetY, angleDeg = 90f, arrowColor = arrowColor)
            }
            else -> {
                val startX = targetX - 50.dp.toPx()
                val startY = targetY - 50.dp.toPx()
                path.moveTo(startX, startY)
                path.quadraticTo(startX + 20.dp.toPx(), targetY - 40.dp.toPx(), targetX, targetY)
                drawCurledPathWithArrowhead(path, targetX, targetY, angleDeg = 85f, arrowColor = arrowColor)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCurledPathWithArrowhead(
    path: Path,
    tipX: Float,
    tipY: Float,
    angleDeg: Float,
    arrowColor: Color
) {
    // Draw Curled Body
    drawPath(
        path = path,
        color = arrowColor,
        style = Stroke(
            width = 3.5.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // Draw Arrowhead Wings
    val rad = Math.toRadians(angleDeg.toDouble())
    val wingLen = 14.dp.toPx()
    val wingAngle = Math.toRadians(28.0)

    val x1 = (tipX - wingLen * cos(rad - wingAngle)).toFloat()
    val y1 = (tipY - wingLen * sin(rad - wingAngle)).toFloat()
    val x2 = (tipX - wingLen * cos(rad + wingAngle)).toFloat()
    val y2 = (tipY - wingLen * sin(rad + wingAngle)).toFloat()

    val wingPath = Path().apply {
        moveTo(x1, y1)
        lineTo(tipX, tipY)
        lineTo(x2, y2)
    }

    drawPath(
        path = wingPath,
        color = arrowColor,
        style = Stroke(
            width = 3.5.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}
