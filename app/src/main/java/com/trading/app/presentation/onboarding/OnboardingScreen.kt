package com.trading.app.presentation.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Exact colors directly extracted from the reference image:
val OnboardingBgColor = Color(0xFF0F1511)          // Matte deep black-olive
val ActiveCardLimeColor = Color(0xFFE7FE54)        // Vibrant electric lime
val DarkContentColor = Color(0xFF0F1511)           // Black text on lime
val StepperButtonColor = Color(0xFFE2FC45)         // Stepper square button lime
val BottomBarContainerColor = Color(0xFF1E261E)    // Elevated bottom dark pill

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val screens = OnboardingScreensList
    var activePageIndex by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    var cardValues by remember {
        mutableStateOf(screens.map { it.defaultVal })
    }
    var cardSwitches by remember {
        mutableStateOf(screens.map { it.defaultSwitchState })
    }

    // Drag gesture tracking for smooth manual card flip
    var dragProgress by remember { mutableStateOf(0f) }

    fun goToNext() {
        if (activePageIndex < screens.size - 1) {
            activePageIndex++
        } else {
            onComplete()
        }
    }

    fun goToPrev() {
        if (activePageIndex > 0) {
            activePageIndex--
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OnboardingBgColor)
            .padding(top = 48.dp, bottom = 36.dp)
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    dragProgress += delta
                },
                onDragStopped = { velocity ->
                    if (dragProgress < -80f || velocity < -500f) {
                        goToNext()
                    } else if (dragProgress > 80f || velocity > 500f) {
                        goToPrev()
                    }
                    dragProgress = 0f
                }
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Headline (Smooth animated swap matching the image)
            AnimatedContent(
                targetState = activePageIndex,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "HeadlineAnimation"
            ) { targetIndex ->
                val currentScreen = screens[targetIndex]
                Text(
                    text = currentScreen.title,
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 38.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 12.dp)
                )
            }

            // 2. Middle Layer: Tilted Corner Stacks with Center Active Card & Glassy Neighbor Cards
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val cardWidth = 265.dp
                val cardHeight = 350.dp

                // (A) Left Neighbor Card (Tilted to the left in the corner)
                if (activePageIndex > 0) {
                    val prevIndex = activePageIndex - 1
                    GlassySideCard(
                        screenData = screens[prevIndex],
                        isLeft = true,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = (-105).dp, y = 20.dp)
                            .size(cardWidth, cardHeight)
                            .zIndex(1f)
                            .clickable { goToPrev() }
                    )
                }

                // (B) Right Neighbor Card (Glassy, tilted to the right in the corner with top glass sheen)
                if (activePageIndex < screens.size - 1) {
                    val nextIndex = activePageIndex + 1
                    GlassySideCard(
                        screenData = screens[nextIndex],
                        isLeft = false,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .offset(x = 105.dp, y = (-20).dp)
                            .size(cardWidth, cardHeight)
                            .zIndex(1f)
                            .clickable { goToNext() }
                    )
                }

                // (C) Ambient Yellow Neon Glow Behind Active Center Card
                Box(
                    modifier = Modifier
                        .size(width = cardWidth * 1.05f, height = cardHeight * 1.05f)
                        .graphicsLayer {
                            rotationZ = -3.5f
                        }
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0x55E7FE54),
                                        Color(0x1AE7FE54),
                                        Color.Transparent
                                    ),
                                    center = center,
                                    radius = size.width * 0.7f
                                )
                            )
                        }
                        .zIndex(2f)
                )

                // (D) Active Center Lime Card (Tilted -3.5° just like reference)
                val animatedRotation by animateFloatAsState(
                    targetValue = -3.5f,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "centerRotation"
                )

                ActiveCenterCard(
                    screenData = screens[activePageIndex],
                    currentVal = cardValues[activePageIndex],
                    isSwitchOn = cardSwitches[activePageIndex],
                    onIncrement = {
                        val updated = cardValues.toMutableList()
                        if (updated[activePageIndex] < screens[activePageIndex].maxVal) {
                            updated[activePageIndex] += screens[activePageIndex].step
                            cardValues = updated
                        }
                    },
                    onDecrement = {
                        val updated = cardValues.toMutableList()
                        if (updated[activePageIndex] > screens[activePageIndex].minVal) {
                            updated[activePageIndex] -= screens[activePageIndex].step
                            cardValues = updated
                        }
                    },
                    onToggleSwitch = {
                        val updated = cardSwitches.toMutableList()
                        updated[activePageIndex] = !updated[activePageIndex]
                        cardSwitches = updated
                    },
                    modifier = Modifier
                        .size(width = cardWidth, height = cardHeight)
                        .graphicsLayer {
                            rotationZ = animatedRotation
                        }
                        .shadow(elevation = 24.dp, shape = RoundedCornerShape(32.dp), spotColor = Color(0x88E7FE54))
                        .zIndex(3f)
                )
            }

            // 3. Bottom Section: 4 Indicator Dash Lines + Floating Pill Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 4-Segment Dash Indicator Lines
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(screens.size) { index ->
                        val isSelected = index == activePageIndex
                        val widthAnim by animateFloatAsState(
                            targetValue = if (isSelected) 32f else 12f,
                            animationSpec = spring(stiffness = Spring.StiffnessMedium),
                            label = "dashWidth"
                        )
                        Box(
                            modifier = Modifier
                                .height(3.dp)
                                .width(widthAnim.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isSelected) Color.White else Color(0x33FFFFFF))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(26.dp))

                // Bottom Pill Button with White Play Circle & Chevron Triplet "Start tracking >>>"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(BottomBarContainerColor)
                        .clickable { goToNext() }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: White Rounded Play Circle
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Next",
                                tint = DarkContentColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Center: "Start tracking" on last page, "Start tracking" / "Continue" on earlier
                        Text(
                            text = if (activePageIndex == screens.size - 1) "Start trading" else "Start tracking",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Right: Triplet Chevron Arrow ">>>"
                        Row(
                            modifier = Modifier.padding(end = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "›", color = Color(0x66FFFFFF), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text(text = "›", color = Color(0xAAFFFFFF), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text(text = "›", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Center Active Card: High-contrast electric lime card with -3.5° tilt.
 */
@Composable
fun ActiveCenterCard(
    screenData: OnboardingScreenData,
    currentVal: Int,
    isSwitchOn: Boolean,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onToggleSwitch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(ActiveCardLimeColor)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Card Heading Text
            Text(
                text = screenData.cardHeading,
                color = DarkContentColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 23.sp
            )

            // 2. Stepper Component [-]  8 hours  [+]
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(StepperButtonColor)
                        .clickable { onDecrement() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrement", tint = DarkContentColor, modifier = Modifier.size(20.dp))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$currentVal",
                        color = DarkContentColor,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = screenData.unitLabel,
                        color = DarkContentColor.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }

                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(StepperButtonColor)
                        .clickable { onIncrement() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increment", tint = DarkContentColor, modifier = Modifier.size(20.dp))
                }
            }

            // 3. Bottom Pill Toggle Switch (Dark pill with lime thumb)
            Box(
                modifier = Modifier
                    .width(135.dp)
                    .height(46.dp)
                    .clip(RoundedCornerShape(23.dp))
                    .background(DarkContentColor)
                    .clickable { onToggleSwitch() }
                    .padding(4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                val thumbOffset by animateFloatAsState(
                    targetValue = if (isSwitchOn) 72f else 0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "switchThumb"
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = screenData.switchLabel,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .offset { IntOffset(thumbOffset.dp.roundToPx(), 0) }
                        .size(width = 54.dp, height = 38.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(ActiveCardLimeColor)
                )
            }
        }
    }
}

/**
 * Corner Neighbor Card: Exactly mimics the glassy, tilted translucent card in the reference image.
 * Features:
 * - Tilted angle (12° to 14°)
 * - Dark smoky translucent body with subtle border
 * - Top-corner curved frosted glass highlight gradient
 * - White outline pill switch at the bottom
 */
@Composable
fun GlassySideCard(
    screenData: OnboardingScreenData,
    isLeft: Boolean,
    modifier: Modifier = Modifier
) {
    val rotation = if (isLeft) -14f else 14f

    Box(
        modifier = modifier
            .graphicsLayer {
                rotationZ = rotation
                scaleX = 0.95f
                scaleY = 0.95f
            }
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF333833),
                        Color(0xFF222622),
                        Color(0xFF181D18)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x66FFFFFF),
                        Color(0x1AFFFFFF),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(32.dp)
            )
            .padding(24.dp)
    ) {
        // Frosted Glass Light Sheen at the top corner
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0x44FFFFFF),
                                Color(0x11FFFFFF),
                                Color.Transparent
                            ),
                            center = if (isLeft) Offset(size.width * 0.8f, 0f) else Offset(size.width * 0.2f, 0f),
                            radius = size.width * 0.6f
                        )
                    )
                }
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Text Header
            Text(
                text = screenData.cardHeading,
                color = Color(0xFFF1F5F9),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 23.sp
            )

            // Minimalist ghost stepper outline
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x22FFFFFF))
                )
                Text(
                    text = "${screenData.defaultVal}",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x22FFFFFF))
                )
            }

            // Outline pill switch matching reference image
            Box(
                modifier = Modifier
                    .width(135.dp)
                    .height(46.dp)
                    .clip(RoundedCornerShape(23.dp))
                    .border(1.5.dp, Color(0x88FFFFFF), RoundedCornerShape(23.dp))
                    .background(Color(0x1A000000))
                    .padding(4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = 72.dp)
                        .size(width = 54.dp, height = 38.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(Color(0x44FFFFFF))
                )
            }
        }
    }
}
