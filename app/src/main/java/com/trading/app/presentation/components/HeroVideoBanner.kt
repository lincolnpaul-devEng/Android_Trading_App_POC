package com.trading.app.presentation.components

import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.trading.app.R
import com.trading.app.domain.model.SectorPerformance
import com.trading.app.presentation.theme.*
import kotlinx.coroutines.delay

@Composable
fun HeroVideoBanner(
    modifier: Modifier = Modifier,
    sectors: List<SectorPerformance> = emptyList(),
    onSectorClick: ((SectorPerformance) -> Unit)? = null
) {
    val context = LocalContext.current
    val videoUri = remember(context) {
        Uri.parse("android.resource://${context.packageName}/${R.raw.hero_clip}")
    }

    var currentIndex by remember { mutableIntStateOf(0) }

    // Cycle through each sector card every 1.5 seconds so each fills the video card
    LaunchedEffect(sectors.size) {
        if (sectors.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(1500)
            currentIndex = (currentIndex + 1) % sectors.size
        }
    }

    val currentSector = if (sectors.isNotEmpty()) sectors[currentIndex % sectors.size] else null

    // Dynamic translucent background tint based on the active sector's performance
    val (baseColor, targetAlpha) = when {
        currentSector == null -> Pair(DarkSurfaceVariant, 0.32f)
        currentSector.changePercent > 2.0 -> Pair(HeatmapLightGreen, 0.44f)
        currentSector.changePercent > 0.5 -> Pair(HeatmapGreen, 0.38f)
        currentSector.changePercent > 0.0 -> Pair(HeatmapDarkGreen, 0.32f)
        currentSector.changePercent > -0.5 -> Pair(HeatmapNeutral, 0.28f)
        currentSector.changePercent > -2.0 -> Pair(HeatmapLightRed, 0.38f)
        else -> Pair(HeatmapDarkRed, 0.48f)
    }

    val animatedBgColor by animateColorAsState(
        targetValue = baseColor.copy(alpha = targetAlpha),
        animationSpec = tween(durationMillis = 400),
        label = "heroCardBg"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(205.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(
                BorderStroke(1.2.dp, baseColor.copy(alpha = 0.55f)),
                RoundedCornerShape(18.dp)
            )
            .background(Color(0xFF0D1117))
            .then(
                if (onSectorClick != null && currentSector != null) {
                    Modifier.clickable { onSectorClick(currentSector) }
                } else Modifier
            )
    ) {
        // 1. Background Video Layer (Lightweight looping video)
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                VideoView(ctx).apply {
                    setVideoURI(videoUri)
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        mp.setVolume(0f, 0f) // Silent ambient background
                        start()
                    }
                }
            },
            update = { videoView ->
                if (!videoView.isPlaying) {
                    videoView.start()
                }
            }
        )

        // 2. Full-Card Translucent Tint Layer (Video is clearly visible behind this layer)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(animatedBgColor)
        )

        // 3. Subtle Vignette Gradient for Depth and Legibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x33000000),
                            Color(0x11000000),
                            Color(0x55000000)
                        )
                    )
                )
        )

        // 4. Full-Card Sector Display (Fills the entire card, transitions every 1.5s)
        if (currentSector != null) {
            Crossfade(
                targetState = currentSector,
                animationSpec = tween(durationMillis = 350),
                label = "sectorCardCrossfade"
            ) { sector ->
                val isPositive = sector.changePercent >= 0
                val changePrefix = if (isPositive) "+" else ""

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Row: Sector index indicator & Bull/Bear momentum chip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Sector Index Capsule
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x55000000))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "SECTOR ${(currentIndex % sectors.size) + 1} OF ${sectors.size}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.85f),
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Bullish / Bearish Momentum Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isPositive) ProfitGreen.copy(alpha = 0.25f)
                                    else LossRed.copy(alpha = 0.25f)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isPositive) "BULLISH MOMENTUM" else "BEARISH PRESSURE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isPositive) ProfitGreen else LossRed,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Center Hero: Big Bold Sector Name & Trade Percentage
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = sector.name,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = (-0.5).sp
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isPositive) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = if (isPositive) ProfitGreen else LossRed,
                                modifier = Modifier.size(34.dp)
                            )
                            Text(
                                text = "$changePrefix${Formatters.formatPercent(sector.changePercent)}",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = (-0.5).sp
                            )
                        }
                    }

                    // Bottom Row: 11-Sector Segmented Indicator Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        sectors.forEachIndexed { idx, _ ->
                            val isSelected = idx == (currentIndex % sectors.size)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(
                                        if (isSelected) Color.White
                                        else Color.White.copy(alpha = 0.25f)
                                    )
                            )
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Loading Live Sectors...",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }
        }
    }
}
