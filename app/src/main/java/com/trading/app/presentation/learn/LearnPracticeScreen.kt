package com.trading.app.presentation.learn

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.core.Alignment as RiveAlignment
import app.rive.runtime.kotlin.core.Fit
import com.trading.app.R
import com.trading.app.presentation.theme.*

/**
 * Learn & Practice Education Screen.
 * Replicates the hero curved-horizon layout, Rive dancing bear & streak animations,
 * gamified daily stat headline, in-progress session card with progress dots,
 * and daily interactive quizzes/challenges adapted to our dark trading terminal theme.
 */
@Composable
fun LearnPracticeScreen(
    onBack: () -> Unit,
    onStockClick: (String) -> Unit = {},
    onOpenQuizOfWeek: () -> Unit = {},
    onOpenDynamicQuiz: (String) -> Unit = {},
    viewModel: LearnPracticeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val filteredChallenges = remember(state.challenges, state.searchQuery) {
        if (state.searchQuery.isBlank()) {
            state.challenges
        } else {
            state.challenges.filter { it.title.contains(state.searchQuery, ignoreCase = true) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Top Header: Avatar + Back, Search Bar Pill, XP Badge Pill, Action Button
            item {
                LearnTopHeader(
                    totalXp = state.totalXp,
                    searchQuery = state.searchQuery,
                    onSearchQueryChanged = viewModel::onSearchQueryChanged,
                    onBack = onBack
                )
            }

            // 2. Hero Section: Curved Mound Backdrop + Dancing Bear & Streak Rive Animations + Tooltip
            item {
                LearnHeroSection(
                    streakDays = state.streakDays,
                    onStreakClick = viewModel::openStreakDialog
                )
            }

            // 3. Primary Gamified Headline: "0 XP today • 0 lessons • 1 challenge"
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${state.todayXp} XP today",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        letterSpacing = (-0.5).sp
                    )

                    Text(
                        text = "${state.todayLessons} lessons • ${state.todayChallenges} challenge${if (state.todayChallenges != 1) "s" else ""}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }
            }

            // 4. Featured Active Module Card: "Master Risk & Position Sizing • Session 4"
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    FeaturedSessionCard(
                        isCompleted = state.sessionQuizAnswered,
                        onClick = viewModel::openSessionDialog
                    )
                }
            }

            // 5. Section Header for Daily Challenges
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daily Challenges & Quizzes",
                        style = TradingTypography.sectionHeader,
                        color = TextPrimary
                    )
                    Text(
                        text = "${filteredChallenges.count { it.isCompleted }}/${filteredChallenges.size} Done",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF5FE2D6)
                    )
                }
            }

            // 6. Interactive Challenges & Quizzes List
            items(filteredChallenges, key = { it.id }) { challenge ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ChallengeRowCard(
                        challenge = challenge,
                        onClick = {
                            if (challenge.simulationType == "money_flow_builder" || challenge.id == "quiz_of_week" || challenge.id == "money_flow_builder_poc") {
                                onOpenQuizOfWeek()
                            } else {
                                onOpenDynamicQuiz(challenge.id)
                            }
                        }
                    )
                }
            }
        }

        // Streak Details Dialog
        if (state.showStreakDialog) {
            StreakDetailsDialog(
                streakDays = state.streakDays,
                onDismiss = viewModel::closeStreakDialog
            )
        }

        // Interactive Quiz Dialog
        state.activeChallenge?.let { challenge ->
            ChallengeQuizDialog(
                challenge = challenge,
                selectedOption = state.quizSelectedOption,
                isCorrect = state.quizIsCorrect,
                onSelectOption = viewModel::submitQuizAnswer,
                onDismiss = viewModel::closeChallenge
            )
        }

        // Featured Session Overview Dialog
        if (state.activeSessionDialog) {
            SessionOverviewDialog(
                isCompleted = state.sessionQuizAnswered,
                onComplete = viewModel::completeSession,
                onDismiss = viewModel::closeSessionDialog
            )
        }
    }
}

/**
 * Top Header: Profile avatar with back button, rounded search pill,
 * glowing XP gem pill, and AI / notification action button.
 */
@Composable
private fun LearnTopHeader(
    totalXp: Int,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Profile avatar circle that doubles as back action
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(DarkSurfaceVariant)
                .border(1.5.dp, Color(0xFF5FE2D6).copy(alpha = 0.5f), CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.profile_avatar),
                contentDescription = "Profile & Back",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            // Tiny back icon badge overlay on bottom right
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Color(0xFF040A06))
                    .border(1.dp, Color(0xFF5FE2D6), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = Color(0xFF5FE2D6),
                    modifier = Modifier.size(10.dp)
                )
            }
        }

        // Center: Search Bar Pill
        Box(
            modifier = Modifier
                .weight(1f)
                .height(42.dp)
                .clip(RoundedCornerShape(21.dp))
                .background(DarkSurfaceVariant)
                .border(1.dp, DarkBorder, RoundedCornerShape(21.dp))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (searchQuery.isEmpty()) "Search" else searchQuery,
                    color = if (searchQuery.isEmpty()) TextSecondary else TextPrimary,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Right 1: Glowing Berry / XP Pill
        Row(
            modifier = Modifier
                .height(42.dp)
                .clip(RoundedCornerShape(21.dp))
                .background(DarkSurfaceVariant)
                .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.4f), RoundedCornerShape(21.dp))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Berry / Gem Icon Vector
            BerryGemIcon(modifier = Modifier.size(20.dp))

            Text(
                text = "$totalXp",
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        // Right 2: AI / Assistant Action Button
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(DarkSurfaceVariant)
                .border(1.dp, DarkBorder, CircleShape)
                .clickable { /* action */ },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = "AI Assistant",
                tint = Color(0xFF5FE2D6),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Hero Section: Atmospheric Curved Mound backdrop with dancing bear Rive animation,
 * campfire streak badge Rive animation, and "See my streak" speech bubble.
 */
@Composable
private fun LearnHeroSection(
    streakDays: Int,
    onStreakClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(290.dp)
    ) {
        // Atmospheric radial glow behind the bear
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF58A6FF).copy(alpha = 0.16f),
                                Color(0xFF8B5CF6).copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.45f, size.height * 0.40f),
                            radius = size.width * 0.50f
                        )
                    )
                }
        )

        // Curved Mound Horizon (smooth arch where the bear and campfire rest)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .align(Alignment.BottomCenter)
        ) {
            val w = size.width
            val h = size.height

            val hillPath = Path().apply {
                moveTo(0f, h * 0.45f)
                cubicTo(
                    w * 0.25f, 0f,
                    w * 0.75f, 0f,
                    w, h * 0.45f
                )
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }

            // Fill curved hill with dark elevated surface
            drawPath(
                path = hillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF151D28),
                        Color(0xFF0F151F),
                        DarkBackground
                    )
                )
            )

            // Crest subtle luminous rim stroke
            val crestPath = Path().apply {
                moveTo(0f, h * 0.45f)
                cubicTo(
                    w * 0.25f, 0f,
                    w * 0.75f, 0f,
                    w, h * 0.45f
                )
            }
            drawPath(
                path = crestPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0x335FE2D6),
                        Color(0x995FE2D6),
                        Color(0x668B5CF6),
                        Color(0x228B5CF6)
                    )
                ),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 2.dp.toPx()
                )
            )
        }

        // Bear Animation (cool_bear_dancing.riv) - positioned slightly left of center
        Box(
            modifier = Modifier
                .width(220.dp)
                .height(230.dp)
                .align(Alignment.BottomCenter)
                .offset(x = (-30).dp, y = (-20).dp)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    RiveAnimationView(ctx).apply {
                        isOpaque = false
                        background = null
                        setRiveResource(
                            resId = R.raw.cool_bear_dancing,
                            fit = Fit.CONTAIN,
                            alignment = RiveAlignment.CENTER,
                            autoplay = true
                        )
                    }
                }
            )
        }

        // Streak Campfire Animation (streak_days.riv) - positioned to the right of the bear
        Box(
            modifier = Modifier
                .width(135.dp)
                .height(135.dp)
                .align(Alignment.BottomEnd)
                .offset(x = (-26).dp, y = (-10).dp)
                .clickable(onClick = onStreakClick)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    RiveAnimationView(ctx).apply {
                        isOpaque = false
                        background = null
                        setRiveResource(
                            resId = R.raw.streak_days,
                            fit = Fit.CONTAIN,
                            alignment = RiveAlignment.CENTER,
                            autoplay = true
                        )
                    }
                }
            )
        }

        // "See my streak" Tooltip speech bubble pointing down to the campfire badge
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-20).dp, y = (-125).dp)
                .clickable(onClick = onStreakClick)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Speech Bubble Body
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF262C36))
                        .border(1.dp, Color(0xFF3F4756), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "See my streak",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Triangle pointer downward caret
                Canvas(
                    modifier = Modifier
                        .size(10.dp, 6.dp)
                        .offset(y = (-1).dp)
                ) {
                    val pointerPath = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width / 2f, size.height)
                        lineTo(size.width, 0f)
                        close()
                    }
                    drawPath(path = pointerPath, color = Color(0xFF262C36))
                }
            }
        }
    }
}

/**
 * Featured Session Card: "Master Risk & Position Sizing • Session 4"
 * with progress indicator dots, XP reward, and image thumbnail.
 */
@Composable
private fun FeaturedSessionCard(
    isCompleted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
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
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: Details & Progress
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Master Risk & Position Sizing",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    lineHeight = 22.sp
                )

                // Session 4 and Progress Dots
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Session 4",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )

                    // Progress Dots (12 dots matching reference)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..12) {
                            val dotColor = when {
                                i <= 3 -> Color(0xFF3FB950) // completed green
                                i == 4 -> if (isCompleted) Color(0xFF3FB950) else Color(0xFF58A6FF) // active
                                else -> Color(0xFF2D3642) // upcoming
                            }
                            val dotSize = if (i == 4) 6.5.dp else 5.dp

                            Box(
                                modifier = Modifier
                                    .size(dotSize)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // XP Reward Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isCompleted) "Completed" else "100",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) Color(0xFF3FB950) else TextPrimary
                    )
                    BerryGemIcon(modifier = Modifier.size(16.dp))
                }
            }

            // Right Column: Session Thumbnail with rounded corners
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0F151F))
                    .border(1.dp, Color(0xFF303B4A), RoundedCornerShape(14.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.learn_session_thumb),
                    contentDescription = "Session Thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

/**
 * Interactive Challenge Row Card matching the reference list design.
 */
@Composable
private fun ChallengeRowCard(
    challenge: ChallengeItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DarkSurface)
            .border(
                width = 1.dp,
                color = if (challenge.isCompleted) Color(0xFF3FB950).copy(alpha = 0.4f) else DarkBorder,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Circular Badge Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        when (challenge.iconType) {
                            "quiz" -> Color(0xFF8B5CF6).copy(alpha = 0.15f)
                            "trade" -> Color(0xFF3FB950).copy(alpha = 0.15f)
                            "chart" -> Color(0xFF58A6FF).copy(alpha = 0.15f)
                            else -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                        }
                    )
                    .border(
                        1.dp,
                        when (challenge.iconType) {
                            "quiz" -> Color(0xFF8B5CF6).copy(alpha = 0.4f)
                            "trade" -> Color(0xFF3FB950).copy(alpha = 0.4f)
                            "chart" -> Color(0xFF58A6FF).copy(alpha = 0.4f)
                            else -> Color(0xFFF59E0B).copy(alpha = 0.4f)
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                val icon = when (challenge.iconType) {
                    "quiz" -> Icons.Outlined.Psychology
                    "trade" -> Icons.Outlined.TrendingUp
                    "chart" -> Icons.Outlined.ShowChart
                    else -> Icons.Outlined.LocalFireDepartment
                }
                val iconTint = when (challenge.iconType) {
                    "quiz" -> Color(0xFFBC8CFF)
                    "trade" -> Color(0xFF3FB950)
                    "chart" -> Color(0xFF58A6FF)
                    else -> Color(0xFFF59E0B)
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Middle: Title and XP Reward Range
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = challenge.title,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = if (challenge.minXp == challenge.maxXp) "${challenge.minXp}" else "${challenge.minXp} - ${challenge.maxXp}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    BerryGemIcon(modifier = Modifier.size(15.dp))

                    if (challenge.isCompleted) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "• Done",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3FB950)
                        )
                    }
                }
            }

            // Right: Navigation Arrow
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF21262D)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (challenge.isCompleted) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open",
                    tint = if (challenge.isCompleted) Color(0xFF3FB950) else Color(0xFF58A6FF),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Berry / Gem icon matching the reference's circular blue-purple reward berry.
 */
@Composable
fun BerryGemIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Gradient berry body
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF8B5CF6),
                    Color(0xFF4F46E5),
                    Color(0xFF2E2675)
                ),
                center = Offset(center.x - radius * 0.25f, center.y - radius * 0.25f),
                radius = radius
            ),
            center = center,
            radius = radius
        )

        // Shimmer specular highlight
        drawCircle(
            color = Color.White.copy(alpha = 0.55f),
            center = Offset(center.x - radius * 0.35f, center.y - radius * 0.35f),
            radius = radius * 0.28f
        )
    }
}

/**
 * Interactive Streak Details Dialog showing current streak and calendar track.
 */
@Composable
private fun StreakDetailsDialog(
    streakDays: Int,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(DarkSurface)
                .border(1.5.dp, Color(0xFFF59E0B).copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Streak Campfire Animation inside dialog
                Box(modifier = Modifier.size(90.dp)) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            RiveAnimationView(ctx).apply {
                                isOpaque = false
                                background = null
                                setRiveResource(
                                    resId = R.raw.streak_days,
                                    fit = Fit.CONTAIN,
                                    alignment = RiveAlignment.CENTER,
                                    autoplay = true
                                )
                            }
                        }
                    )
                }

                Text(
                    text = "$streakDays-Day Trading Streak!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )

                Text(
                    text = "Complete at least 1 educational lesson or simulated trade each day to keep your learning multiplier active.",
                    fontSize = 13.5.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                // Weekly streak tracker row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val days = listOf("M", "T", "W", "T", "F", "S", "S")
                    days.forEachIndexed { index, day ->
                        val isDayDone = index < streakDays
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isDayDone) Color(0xFFF59E0B).copy(alpha = 0.2f)
                                        else DarkSurfaceVariant
                                    )
                                    .border(
                                        1.dp,
                                        if (isDayDone) Color(0xFFF59E0B) else DarkBorder,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isDayDone) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Text(
                                        text = day,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextTertiary
                                    )
                                }
                            }
                            Text(
                                text = day,
                                fontSize = 11.sp,
                                color = if (isDayDone) TextPrimary else TextTertiary
                            )
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF5FE2D6)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Got It!",
                        color = Color(0xFF040A06),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Interactive Challenge Quiz Dialog allowing user to answer a trading question and earn XP!
 */
@Composable
private fun ChallengeQuizDialog(
    challenge: ChallengeItem,
    selectedOption: Int?,
    isCorrect: Boolean?,
    onSelectOption: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(DarkSurface)
                .border(1.2.dp, Color(0xFF58A6FF).copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BerryGemIcon(modifier = Modifier.size(18.dp))
                        Text(
                            text = "+${challenge.maxXp} XP",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5FE2D6)
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Text(
                    text = challenge.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )

                challenge.question?.let { q ->
                    Text(
                        text = q,
                        fontSize = 14.5.sp,
                        color = TextPrimary,
                        lineHeight = 20.sp
                    )
                }

                // Options list
                challenge.options.forEachIndexed { index, optionText ->
                    val isChosen = selectedOption == index
                    val optionBorder = when {
                        isChosen && isCorrect == true -> Color(0xFF3FB950)
                        isChosen && isCorrect == false -> Color(0xFFF85149)
                        else -> DarkBorder
                    }
                    val optionBg = when {
                        isChosen && isCorrect == true -> Color(0xFF3FB950).copy(alpha = 0.15f)
                        isChosen && isCorrect == false -> Color(0xFFF85149).copy(alpha = 0.15f)
                        else -> DarkSurfaceVariant
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(optionBg)
                            .border(1.2.dp, optionBorder, RoundedCornerShape(12.dp))
                            .clickable(enabled = selectedOption == null) {
                                onSelectOption(index)
                            }
                            .padding(14.dp)
                    ) {
                        Text(
                            text = optionText,
                            fontSize = 13.5.sp,
                            fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium,
                            color = if (isChosen) TextPrimary else TextSecondary
                        )
                    }
                }

                // Feedback / Explanation
                if (isCorrect != null) {
                    challenge.explanation?.let { exp ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isCorrect) Color(0xFF3FB950).copy(alpha = 0.12f)
                                    else Color(0xFFF85149).copy(alpha = 0.12f)
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = (if (isCorrect) "✓ Correct! " else "✕ Try again next time. ") + exp,
                                fontSize = 12.5.sp,
                                color = if (isCorrect) Color(0xFF3FB950) else Color(0xFFF85149),
                                lineHeight = 17.sp
                            )
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF5FE2D6)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Continue",
                            color = Color(0xFF040A06),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Overview dialog for the featured "Master Risk & Position Sizing" session.
 */
@Composable
private fun SessionOverviewDialog(
    isCompleted: Boolean,
    onComplete: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(DarkSurface)
                .border(1.2.dp, Color(0xFF5FE2D6).copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Session 4: Risk Management",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Text(
                    text = "Core Rules of Capital Preservation:\n\n" +
                            "1. Maximum 1% - 2% Risk: Never risk more than 1% to 2% of total account equity on any single trade.\n" +
                            "2. Asymmetric Risk/Reward: Target at least 2:1 profit potential relative to your defined stop-loss.\n" +
                            "3. Position Sizing Formula: Position Units = (Account Balance × Risk %) ÷ (Entry Price - Stop Price).",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )

                Button(
                    onClick = onComplete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCompleted) Color(0xFF3FB950) else Color(0xFF5FE2D6)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isCompleted) "Completed (+100 XP Earned)" else "Complete Session (+100 XP)",
                        color = Color(0xFF040A06),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
