package com.trading.app.presentation.game

import androidx.annotation.RawRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.core.Alignment as RiveAlignment
import app.rive.runtime.kotlin.core.Fit
import com.trading.app.R
import com.trading.app.presentation.theme.TextPrimary
import com.trading.app.presentation.theme.TradingTypography

data class MiniGame(
    val id: String,
    val title: String,
    val description: String,
    val rewardBadge: String,
    @RawRes val riveResId: Int,
    val artboardName: String?,
    val stateMachineName: String?,
    val isLandscape: Boolean,
    val leftTopGradient: Color,
    val tileGlowTop: Color,
    val tileGlowBottom: Color
)

val AvailableMiniGames: List<MiniGame> = listOf(
    MiniGame(
        id = "brave_knight",
        title = "The Brave Knight",
        description = "Defeat the boss knight in epic combat to earn trading rewards",
        rewardBadge = "+50,000",
        riveResId = R.raw.game_brave_knight,
        artboardName = "Main artboard",
        stateMachineName = "State Machine 1",
        isLandscape = true,
        leftTopGradient = Color(0xFF3E3CA8),
        tileGlowTop = Color(0xFF1A1C2E),
        tileGlowBottom = Color(0xFF4C5AE8)
    ),
    MiniGame(
        id = "brawl_archery",
        title = "Brawl Archery",
        description = "Hit bullseye targets under pressure to win bonus coins",
        rewardBadge = "+30,000",
        riveResId = R.raw.game_brawl_archery,
        artboardName = "Brawl Arrow",
        stateMachineName = "State Machine 1",
        isLandscape = false,
        leftTopGradient = Color(0xFF4A3FA8),
        tileGlowTop = Color(0xFF2B1810),
        tileGlowBottom = Color(0xFFF5B82E)
    ),
    MiniGame(
        id = "graveyard_shift",
        title = "Graveyard Shift",
        description = "Whack spooky graveyard moles before time runs out",
        rewardBadge = "+40,000",
        riveResId = R.raw.game_graveyard_shift,
        artboardName = "Game",
        stateMachineName = "Game SM",
        isLandscape = true,
        leftTopGradient = Color(0xFF36399A),
        tileGlowTop = Color(0xFF142428),
        tileGlowBottom = Color(0xFF2DD4BF)
    )
)

@Composable
fun MiniGameCarouselSection(
    onGameClick: (MiniGame) -> Unit,
    modifier: Modifier = Modifier
) {
    val games = AvailableMiniGames
    val pagerState = rememberPagerState(pageCount = { games.size })

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Play & Earn Rewards",
            style = TradingTypography.sectionHeader,
            color = TextPrimary,
            maxLines = 1,
            softWrap = false
        )

        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(end = 24.dp),
            pageSpacing = 12.dp,
            beyondViewportPageCount = 2,
            key = { page -> games[page].id },
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val game = games[page]
            MiniGamePreviewCard(
                game = game,
                onClick = { onGameClick(game) }
            )
        }

        // Subtle Page Indicator Dots
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            games.indices.forEach { idx ->
                val isSelected = pagerState.currentPage == idx
                val dotWidth by animateDpAsState(
                    targetValue = if (isSelected) 20.dp else 6.dp,
                    animationSpec = tween(250),
                    label = "dotWidth"
                )
                val dotColor by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFF5356D6) else Color(0xFF30363D),
                    animationSpec = tween(250),
                    label = "dotColor"
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(6.dp)
                        .width(dotWidth)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
        }
    }
}

@Composable
private fun MiniGamePreviewCard(
    game: MiniGame,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp)
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
    ) {
        // Left Panel: Deep violet-to-black gradient with top-left reward badge & tilted glowing game tile
        Box(
            modifier = Modifier
                .weight(0.43f)
                .fillMaxHeight()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            game.leftTopGradient,
                            Color(0xFF1A1B4B),
                            Color(0xFF07080C)
                        )
                    )
                )
                .padding(12.dp)
        ) {
            // Top-left reward pill ("UP TO +30,000") matching reference image
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF28295C).copy(alpha = 0.92f))
                    .border(
                        width = 0.8.dp,
                        color = Color.White.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy((-2).dp)
                ) {
                    Text(
                        text = "UP",
                        fontSize = 6.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White.copy(alpha = 0.72f),
                        lineHeight = 6.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "TO",
                        fontSize = 6.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White.copy(alpha = 0.72f),
                        lineHeight = 6.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Text(
                    text = game.rewardBadge,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false
                )
            }

            // Center Tilted Glowing App Icon Tile with Fit.COVER Rive preview fixed to this game
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
                    .size(92.dp)
                    .rotate(-10f)
                    .shadow(elevation = 12.dp, shape = RoundedCornerShape(22.dp))
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
                                Color.White.copy(alpha = 0.45f),
                                game.tileGlowBottom.copy(alpha = 0.9f)
                            )
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .padding(4.dp)
                    .clip(RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                key(game.id) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            RiveAnimationView(ctx).apply {
                                try {
                                    setRiveResource(
                                        resId = game.riveResId,
                                        artboardName = game.artboardName,
                                        stateMachineName = game.stateMachineName,
                                        autoplay = true,
                                        autoBind = true,
                                        fit = Fit.COVER,
                                        alignment = RiveAlignment.CENTER
                                    )
                                } catch (_: Exception) {
                                    setRiveResource(
                                        resId = game.riveResId,
                                        autoplay = true,
                                        autoBind = true,
                                        fit = Fit.COVER,
                                        alignment = RiveAlignment.CENTER
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }

        // Right Panel: Crisp White Card Content with Title, Description & Indigo Pill "Play" Button
        Column(
            modifier = Modifier
                .weight(0.57f)
                .fillMaxHeight()
                .background(Color.White)
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = game.title,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0D1117),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = game.description,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF374151),
                    lineHeight = 17.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Button(
                onClick = onClick,
                modifier = Modifier
                    .width(120.dp)
                    .height(40.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4F52C8),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
            ) {
                Text(
                    text = "Play",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
