package com.trading.app.presentation.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.core.Alignment as RiveAlignment
import app.rive.runtime.kotlin.core.Fit

/**
 * Full Games Tab Screen:
 * - Header with overlapping tilted 3D glowing game cards collage and large "Games" title
 * - Rich royal purple gradient background
 * - Vertical feed of all 3 games with split-card layout (dark glowing tile on left, white detail card with Play button on right)
 */
@Composable
fun GamesScreen(
    onGameClick: (String) -> Unit,
    onBackClick: () -> Unit = {}
) {
    val games = AvailableMiniGames

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF282570), // Deep violet header base
                        Color(0xFF1B184C), // Mid transition
                        Color(0xFF2B2896), // Royal purple list start
                        Color(0xFF1E1C6E)  // Deep indigo base
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // 1. Header Section with Back Arrow, Large Title & 3D Floating Cards Collage
            item {
                GamesHeaderSection(onBackClick = onBackClick)
            }

            // 2. Games List: All 3 games fed in sequence
            items(games, key = { it.id }) { game ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    FullGameCard(
                        game = game,
                        onPlayClick = { onGameClick(game.id) }
                    )
                }
            }
        }
    }
}

/**
 * Top Header matching the reference image:
 * - Circular back button top-left
 * - Bold "Games" display title
 * - 3D floating glowing tilted cards collage on the right
 */
@Composable
private fun GamesHeaderSection(
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF28246E),
                        Color(0xFF141232),
                        Color(0xFF090818)
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Back Button
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 16.dp)
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0x33FFFFFF))
                .clickable { onBackClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // Floating 3D Cards Collage in Top-Right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 10.dp, y = (-10).dp)
                .size(width = 240.dp, height = 240.dp)
        ) {
            // Card 1: Top Gold / Amber Card (rotated 14 deg) with triangle glyph
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(x = 16.dp, y = 14.dp)
                    .rotate(14f)
                    .size(80.dp)
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF1A1208), Color(0xFFFBBF24))
                        )
                    )
                    .border(
                        2.dp,
                        Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.6f), Color(0xFFF59E0B))
                        ),
                        RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                TriangleGlyph(modifier = Modifier.size(32.dp), color = Color.White)
            }

            // Card 2: Left Pink / Magenta Card (rotated -18 deg)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-8).dp, y = (-12).dp)
                    .rotate(-18f)
                    .size(76.dp)
                    .shadow(elevation = 14.dp, shape = RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF28112A), Color(0xFFEC4899))
                        )
                    )
                    .border(
                        2.dp,
                        Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.5f), Color(0xFFD946EF))
                        ),
                        RoundedCornerShape(18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                BadgeGlyph(modifier = Modifier.size(28.dp), color = Color.White)
            }

            // Card 3: Right Indigo / Blue Card (rotated 20 deg) with taxi/car glyph
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 6.dp, y = 0.dp)
                    .rotate(20f)
                    .size(76.dp)
                    .shadow(elevation = 14.dp, shape = RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF0F172A), Color(0xFF3B82F6))
                        )
                    )
                    .border(
                        2.dp,
                        Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.5f), Color(0xFF60A5FA))
                        ),
                        RoundedCornerShape(18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Card 4: Foreground Fiery Red / Orange Card (rotated -8 deg) with crossed swords glyph
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(x = 10.dp, y = 14.dp)
                    .rotate(-8f)
                    .size(96.dp)
                    .shadow(elevation = 20.dp, shape = RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF1E0A0E), Color(0xFFF97316), Color(0xFFEF4444))
                        )
                    )
                    .border(
                        2.dp,
                        Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.7f), Color(0xFFFB923C))
                        ),
                        RoundedCornerShape(22.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                CrossedSwordsGlyph(modifier = Modifier.size(42.dp), color = Color.White)
            }
        }

        // "Games" Large Title
        Text(
            text = "Games",
            color = Color.White,
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 12.dp)
        )
    }
}

/**
 * Split Game Card matching the reference design:
 * - Left 42%: Dark indigo gradient, top-left reward badge (§+30,000 / §+50,000), centered glossy 3D game tile
 * - Right 58%: Clean white card with game title, description, and royal purple "Play" pill button
 */
@Composable
private fun FullGameCard(
    game: MiniGame,
    onPlayClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(192.dp)
            .shadow(elevation = 14.dp, shape = RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .clickable(onClick = onPlayClick)
    ) {
        // Left Half: Dark gradient with top-left badge and 3D glowing game tile
        Box(
            modifier = Modifier
                .weight(0.42f)
                .fillMaxHeight()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            game.leftTopGradient,
                            Color(0xFF141638),
                            Color(0xFF070814)
                        )
                    )
                )
                .padding(12.dp)
        ) {
            // Reward Badge: §+30,000 / §+50,000
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF222452).copy(alpha = 0.90f))
                    .border(
                        width = 0.8.dp,
                        color = Color.White.copy(alpha = 0.20f),
                        shape = RoundedCornerShape(50)
                    )
                    .padding(horizontal = 9.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "§" + game.rewardBadge,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Center Glossy 3D Game Tile with Rive preview
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 12.dp)
                    .size(92.dp)
                    .shadow(elevation = 10.dp, shape = RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(game.tileGlowTop, game.tileGlowBottom)
                        )
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.50f),
                                game.tileGlowBottom.copy(alpha = 0.85f)
                            )
                        ),
                        shape = RoundedCornerShape(22.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        RiveAnimationView(ctx).apply {
                            setRiveResource(
                                resId = game.riveResId,
                                artboardName = game.artboardName,
                                stateMachineName = game.stateMachineName,
                                fit = Fit.COVER,
                                alignment = RiveAlignment.CENTER,
                                autoplay = true
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Right Half: Pure white details with Play button
        Column(
            modifier = Modifier
                .weight(0.58f)
                .fillMaxHeight()
                .background(Color.White)
                .padding(18.dp)
        ) {
            Text(
                text = game.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = game.description,
                fontSize = 13.sp,
                color = Color(0xFF475569),
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.weight(1f))

            // "Play" Pill Button (Royal Periwinkle / Purple)
            Box(
                modifier = Modifier
                    .width(105.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF5051D6))
                    .clickable(onClick = onPlayClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Play",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Triangle Glyph for the gold card
 */
@Composable
private fun TriangleGlyph(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(size.width / 2f, size.height * 0.15f)
            lineTo(size.width * 0.88f, size.height * 0.85f)
            lineTo(size.width * 0.12f, size.height * 0.85f)
            close()
        }
        drawPath(path, color = color, style = Stroke(width = size.width * 0.22f, cap = StrokeCap.Round))
    }
}

/**
 * Badge / chat glyph for the magenta card
 */
@Composable
private fun BadgeGlyph(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.2f, h * 0.2f)
            lineTo(w * 0.8f, h * 0.2f)
            lineTo(w * 0.8f, h * 0.65f)
            lineTo(w * 0.5f, h * 0.65f)
            lineTo(w * 0.35f, h * 0.85f)
            lineTo(w * 0.35f, h * 0.65f)
            lineTo(w * 0.2f, h * 0.65f)
            close()
        }
        drawPath(path, color = color)
    }
}

/**
 * Crossed Swords Glyph for the red/orange card
 */
@Composable
private fun CrossedSwordsGlyph(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = w * 0.14f

        // Sword 1 (\)
        drawLine(
            color = color,
            start = Offset(w * 0.2f, h * 0.2f),
            end = Offset(w * 0.8f, h * 0.8f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        // Guard 1
        drawLine(
            color = color,
            start = Offset(w * 0.38f, h * 0.22f),
            end = Offset(w * 0.22f, h * 0.38f),
            strokeWidth = strokeW * 0.9f,
            cap = StrokeCap.Round
        )

        // Sword 2 (/)
        drawLine(
            color = color,
            start = Offset(w * 0.8f, h * 0.2f),
            end = Offset(w * 0.2f, h * 0.8f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        // Guard 2
        drawLine(
            color = color,
            start = Offset(w * 0.62f, h * 0.22f),
            end = Offset(w * 0.78f, h * 0.38f),
            strokeWidth = strokeW * 0.9f,
            cap = StrokeCap.Round
        )
    }
}
