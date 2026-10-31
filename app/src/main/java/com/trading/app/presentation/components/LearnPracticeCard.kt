package com.trading.app.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.core.Alignment as RiveAlignment
import app.rive.runtime.kotlin.core.Fit
import com.trading.app.R
import com.trading.app.presentation.theme.TextPrimary
import com.trading.app.presentation.theme.TradingTypography
import kotlinx.coroutines.delay

/**
 * Custom Shape that replicates the exact Bensch Wallet card silhouette:
 * - Rounded top-left corner
 * - Top edge steps down by ~10% via a smooth cubic S-curve at ~49%-59% width
 * - Rounded top-right corner
 * - Right vertical edge
 * - Bottom edge steps down by ~10% via a cubic S-curve to form an extended cradle for the CTA button
 * - Returns back to base bottom level via an inverse cubic curve
 * - Rounded bottom-left corner
 */
/**
 * Constructs the exact Bensch Wallet card silhouette path:
 * - Rounded top-left corner
 * - Top edge steps down by ~10% via a smooth cubic S-curve at ~49%-59% width
 * - Rounded top-right corner
 * - Right vertical edge
 * - Bottom edge steps down by ~10% via a cubic S-curve to form an extended cradle for the CTA button
 * - Returns back to base bottom level via an inverse cubic curve
 * - Rounded bottom-left corner
 */
fun buildBenschWalletCardPath(
    size: Size,
    density: Density,
    cornerRadiusDp: Dp = 22.dp
): Path {
    val w = size.width
    val h = size.height
    val r = with(density) { cornerRadiusDp.toPx() }.coerceAtMost(w * 0.08f).coerceAtMost(h * 0.12f)

    val topShelf = 0.10f * h
    val botShelf = 0.898f * h
    val botCradle = 1.00f * h

    val xS = 0.49f * w
    val xE = 0.59f * w
    val xRs = 0.86f * w
    val xRe = 0.94f * w

    val dx = xE - xS
    val dxRet = xRe - xRs

    return Path().apply {
        // 1. Top-left corner
        moveTo(0f, r)
        cubicTo(0f, r * 0.45f, r * 0.45f, 0f, r, 0f)

        // 2. Top-left shelf
        lineTo(xS, 0f)

        // 3. Top step-down S-curve
        cubicTo(
            xS + dx * 0.5f, 0f,
            xE - dx * 0.5f, topShelf,
            xE, topShelf
        )

        // 4. Top-right shelf
        lineTo(w - r, topShelf)

        // 5. Top-right corner
        cubicTo(
            w - r * 0.45f, topShelf,
            w, topShelf + r * 0.45f,
            w, topShelf + r
        )

        // 6. Right edge
        lineTo(w, botShelf - r)

        // 7. Bottom-right corner
        cubicTo(
            w, botShelf - r * 0.45f,
            w - r * 0.45f, botShelf,
            w - r, botShelf
        )

        // 8. Bottom-right shelf
        lineTo(xRe, botShelf)

        // 9. Bottom cradle return S-curve (right to left)
        cubicTo(
            xRe - dxRet * 0.5f, botShelf,
            xRs + dxRet * 0.5f, botCradle,
            xRs, botCradle
        )

        // 10. Bottom cradle horizontal shelf
        lineTo(xE, botCradle)

        // 11. Bottom cradle step-up S-curve (right to left)
        cubicTo(
            xE - dx * 0.5f, botCradle,
            xS + dx * 0.5f, botShelf,
            xS, botShelf
        )

        // 12. Bottom-left shelf
        lineTo(r, botShelf)

        // 13. Bottom-left corner
        cubicTo(
            r * 0.45f, botShelf,
            0f, botShelf - r * 0.45f,
            0f, botShelf - r
        )

        // 14. Left edge back to top
        close()
    }
}

class BenschWalletCardShape(
    private val cornerRadiusDp: Dp = 22.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Generic(buildBenschWalletCardPath(size, density, cornerRadiusDp))
    }
}

/**
 * Data model for cards in the Learn & Practice horizontal auto-scrolling row.
 */
enum class LearnCardCategory {
    VIRTUAL_WALLET,
    FINANCIAL_MANAGEMENT,
    TRADING_MECHANICS
}

data class LearnPracticeCardItem(
    val id: String,
    val category: LearnCardCategory,
    val headline: String,
    val title: String,
    val subtitle: String? = null,
    val metaLabel1: String,
    val metaValue1: String,
    val metaLabel2: String,
    val metaValue2: String,
    val buttonText: String,
    val usesRiveBackground: Boolean = false
)

/**
 * Learn & Practice Section featuring an auto-scrolling, user-swipeable carousel row
 * of the exact custom-clipped Bensch Wallet cards:
 * 1. Virtual Wallet card with neon gradient & topographic wave contours
 * 2. "LEARN TO MANAGE YOUR FINANCES" with meet_cute.riv Rive background & "Learn Now" button
 * 3. "HOW TRADING WORKS" with meet_cute.riv Rive background & "Learn Now" button
 */
@Composable
fun LearnAndPracticeCardSection(
    balance: Double? = null,
    balanceText: String = balance?.let { Formatters.formatCurrency(it) } ?: "$76,149.12",
    onAddMoneyClick: () -> Unit = {},
    onLearnClick: (LearnPracticeCardItem) -> Unit = {},
    onCardClick: (LearnPracticeCardItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val cards = remember(balanceText) {
        listOf(
            LearnPracticeCardItem(
                id = "wallet_card",
                category = LearnCardCategory.VIRTUAL_WALLET,
                headline = "Current Balance",
                title = balanceText,
                subtitle = "Virtual Demo Balance",
                metaLabel1 = "Card Number",
                metaValue1 = "5679 ★★★★",
                metaLabel2 = "Exp.",
                metaValue2 = "08/26",
                buttonText = "Add Money",
                usesRiveBackground = false
            ),
            LearnPracticeCardItem(
                id = "education_card",
                category = LearnCardCategory.FINANCIAL_MANAGEMENT,
                headline = "",
                title = "LEARN TO MANAGE YOUR FINANCES",
                subtitle = "HOW TRADING WORKS",
                metaLabel1 = "",
                metaValue1 = "",
                metaLabel2 = "",
                metaValue2 = "",
                buttonText = "Learn Now",
                usesRiveBackground = true
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { cards.size })

    // Auto-scroll every 4.5 seconds to advance smoothly across cards when not being dragged
    LaunchedEffect(pagerState) {
        while (true) {
            delay(4500)
            if (!pagerState.isScrollInProgress) {
                val nextPage = (pagerState.currentPage + 1) % cards.size
                pagerState.animateScrollToPage(
                    page = nextPage,
                    animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Section Header: "Learn & Practice"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Learn & Practice",
                style = TradingTypography.sectionHeader,
                color = TextPrimary,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = "${pagerState.currentPage + 1} / ${cards.size}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF5FE2D6)
            )
        }

        // Horizontal Pager: Auto-scroll & User swipeable row
        HorizontalPager(
            state = pagerState,
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val card = cards[page]
            if (card.usesRiveBackground) {
                BenschWalletRiveCard(
                    card = card,
                    onLearnClick = { onLearnClick(card) },
                    onCardClick = { onCardClick(card) }
                )
            } else {
                BenschWalletGradientCard(
                    card = card,
                    onAddMoneyClick = onAddMoneyClick,
                    onCardClick = { onCardClick(card) }
                )
            }
        }

        // Animated Page Indicator Dots
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            cards.indices.forEach { idx ->
                val isSelected = pagerState.currentPage == idx
                val dotWidth by animateDpAsState(
                    targetValue = if (isSelected) 22.dp else 6.dp,
                    animationSpec = tween(250),
                    label = "dotWidth"
                )
                val dotColor by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFF5FE2D6) else Color(0xFF30363D),
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

/**
 * Card 1: Original Bensch Wallet Style with Mint-to-Lime gradient & topographic waves.
 */
@Composable
private fun BenschWalletGradientCard(
    card: LearnPracticeCardItem,
    onAddMoneyClick: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .drawBehind {
                val cardWidth = size.width - 24.dp.toPx()
                val cardHeight = cardWidth / 1.72f
                val topOffset = (size.height - cardHeight) / 2f
                val leftOffset = 12.dp.toPx()

                // Left Mint-Cyan ambient glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF4EE3B8).copy(alpha = 0.32f),
                            Color(0xFF38B291).copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = Offset(leftOffset + cardWidth * 0.28f, topOffset + cardHeight * 0.5f),
                        radius = cardWidth * 0.45f
                    )
                )

                // Right Neon Lime ambient glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFC6F438).copy(alpha = 0.26f),
                            Color(0xFF8DC024).copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(leftOffset + cardWidth * 0.74f, topOffset + cardHeight * 0.5f),
                        radius = cardWidth * 0.48f
                    )
                )

                // Wire trace rising from notch into dark canvas
                val notchX = leftOffset + cardWidth * 0.59f
                val notchY = topOffset + cardHeight * 0.10f
                val wirePath = Path().apply {
                    moveTo(notchX, notchY)
                    cubicTo(
                        notchX + 15.dp.toPx(), notchY - 20.dp.toPx(),
                        notchX + 30.dp.toPx(), notchY - 40.dp.toPx(),
                        notchX + 32.dp.toPx(), topOffset - 16.dp.toPx()
                    )
                }
                drawPath(
                    path = wirePath,
                    color = Color(0xFF488D79).copy(alpha = 0.45f),
                    style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        val cardShape = remember { BenschWalletCardShape(cornerRadiusDp = 22.dp) }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.72f)
                .clip(cardShape)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF5FE2D6),
                            Color(0xFF6DE7C2),
                            Color(0xFF8AE8A0),
                            Color(0xFFA5EE84),
                            Color(0xFFC4EF75)
                        )
                    )
                )
                .clickable(onClick = onCardClick)
        ) {
            val cardWidth = maxWidth
            val cardHeight = maxHeight

            // Topographic Wave Lines
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val startX = w * 0.46f

                val waveData = listOf(
                    Triple(0.40f, 0.35f, 0.55f),
                    Triple(0.47f, 0.43f, 0.45f),
                    Triple(0.55f, 0.51f, 0.35f),
                    Triple(0.63f, 0.59f, 0.25f)
                )

                for ((baseYFrac, dipYFrac, alpha) in waveData) {
                    val path = Path().apply {
                        val startY = h * baseYFrac
                        val dipY = h * dipYFrac
                        moveTo(startX, startY)
                        cubicTo(
                            startX + w * 0.12f, startY,
                            startX + w * 0.18f, dipY,
                            startX + w * 0.28f, dipY
                        )
                        cubicTo(
                            startX + w * 0.38f, dipY,
                            startX + w * 0.44f, startY + (h * 0.08f),
                            w, startY + (h * 0.08f)
                        )
                    }
                    drawPath(
                        path = path,
                        color = Color.White.copy(alpha = alpha),
                        style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // Top-Left: Label & Big Bold Balance
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 22.dp, top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = card.headline,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF042721).copy(alpha = 0.85f),
                    letterSpacing = (-0.2).sp
                )
                Text(
                    text = card.title,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF021714),
                    letterSpacing = (-0.8).sp
                )
            }

            // Bottom-Left: Card Number & Exp. Date (Resting comfortably on the 0.898 base shelf)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 22.dp, bottom = (cardHeight * 0.13f).coerceAtLeast(16.dp)),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = card.metaLabel1,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF042721).copy(alpha = 0.80f)
                    )
                    Text(
                        text = card.metaValue1,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF021714),
                        letterSpacing = 0.5.sp
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = card.metaLabel2,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF042721).copy(alpha = 0.80f)
                    )
                    Text(
                        text = card.metaValue2,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF021714)
                    )
                }
            }

            // Bottom-Right: Obsidian "Add Money" Button placed directly in the lower hollow!
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = cardWidth * 0.103f,
                        bottom = (cardHeight * 0.022f).coerceAtLeast(5.dp)
                    )
                    .width(cardWidth * 0.340f)
                    .height(cardHeight * 0.215f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF040A06))
                    .clickable(onClick = onAddMoneyClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = card.buttonText,
                    color = Color(0xFFC9FCEB),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/**
 * Cards 2 & 3: Duplicate cards featuring the meet_cute.riv Rive animated background,
 * high-contrast educational titles, and "Learn Now" CTA buttons in the bottom cradle.
 */
@Composable
private fun BenschWalletRiveCard(
    card: LearnPracticeCardItem,
    onLearnClick: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val cardShape = remember { BenschWalletCardShape(cornerRadiusDp = 22.dp) }

    // Vibrant gradient matching the first card: Mint #5FE2D6 to Lime #C4EF75
    val firstCardGradient = remember {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF5FE2D6), // Vibrant Mint
                Color(0xFF6DE7C2),
                Color(0xFF8AE8A0),
                Color(0xFFA5EE84),
                Color(0xFFC4EF75)  // Vibrant Lime
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .drawBehind {
                val cardWidth = size.width - 24.dp.toPx()
                val cardHeight = cardWidth / 1.72f
                val topOffset = (size.height - cardHeight) / 2f
                val leftOffset = 12.dp.toPx()

                // Ambient atmospheric glows matching the first card: Mint & Neon Lime
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF4EE3B8).copy(alpha = 0.38f),
                            Color(0xFF38B291).copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = Offset(leftOffset + cardWidth * 0.28f, topOffset + cardHeight * 0.5f),
                        radius = cardWidth * 0.45f
                    )
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFC6F438).copy(alpha = 0.32f),
                            Color(0xFF8DC024).copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = Offset(leftOffset + cardWidth * 0.74f, topOffset + cardHeight * 0.5f),
                        radius = cardWidth * 0.48f
                    )
                )

                // Wire trace rising from notch into dark canvas
                val notchX = leftOffset + cardWidth * 0.59f
                val notchY = topOffset + cardHeight * 0.10f
                val wirePath = Path().apply {
                    moveTo(notchX, notchY)
                    cubicTo(
                        notchX + 15.dp.toPx(), notchY - 20.dp.toPx(),
                        notchX + 30.dp.toPx(), notchY - 40.dp.toPx(),
                        notchX + 32.dp.toPx(), topOffset - 16.dp.toPx()
                    )
                }
                drawPath(
                    path = wirePath,
                    color = Color(0xFF488D79).copy(alpha = 0.50f),
                    style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.72f)
                .clip(cardShape)
                .background(Color(0xFF090E14))
                .clickable(onClick = onCardClick)
                .drawWithContent {
                    drawContent()
                    val cardPath = buildBenschWalletCardPath(size, density, 22.dp)
                    // Outer diffuse glow along the card silhouette using first card's colors
                    drawPath(
                        path = cardPath,
                        brush = firstCardGradient,
                        style = Stroke(
                            width = 5.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        ),
                        alpha = 0.50f
                    )
                    // Inner sharp glowing border outline along the exact card silhouette
                    drawPath(
                        path = cardPath,
                        brush = firstCardGradient,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        ),
                        alpha = 0.98f
                    )
                }
        ) {
            val cardWidth = maxWidth
            val cardHeight = maxHeight

            // Rive Animation Background from meet_cute.riv
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    RiveAnimationView(ctx).apply {
                        setRiveResource(
                            resId = R.raw.meet_cute,
                            fit = Fit.COVER,
                            alignment = RiveAlignment.CENTER,
                            autoplay = true
                        )
                    }
                }
            )

            // High-legibility scrim gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xD9060B10), // 85% opacity dark
                                Color(0x6609131C), // 40% translucent mid so animation shines through
                                Color(0xF505090E)  // 96% opacity dark bottom
                            )
                        )
                    )
            )

            // Top-Left: Clean, uncompressed titles
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 24.dp, top = 22.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "LEARN TO MANAGE YOUR FINANCES",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    lineHeight = 26.sp,
                    letterSpacing = (-0.4).sp,
                    maxLines = 2
                )

                Text(
                    text = "HOW TRADING WORKS",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5FE2D6),
                    letterSpacing = 0.5.sp
                )
            }

            // Bottom-Right: Solid Cyan-Mint "Learn Now" Button Accurately Placed directly in the Lower Hollow!
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = cardWidth * 0.103f,
                        bottom = (cardHeight * 0.022f).coerceAtLeast(5.dp)
                    )
                    .width(cardWidth * 0.340f)
                    .height(cardHeight * 0.215f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF5FE2D6))
                    .clickable(onClick = onLearnClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = card.buttonText,
                    color = Color(0xFF040A06),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.2.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
