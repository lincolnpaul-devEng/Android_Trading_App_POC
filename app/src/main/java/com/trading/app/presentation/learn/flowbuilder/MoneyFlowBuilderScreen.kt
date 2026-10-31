package com.trading.app.presentation.learn.flowbuilder

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DirectionsTransit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SentimentSatisfiedAlt
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trading.app.presentation.learn.BerryGemIcon
import com.trading.app.presentation.theme.*

/**
 * Full Brilliant.org-style interactive Money Flow Builder lesson.
 * Built with the exact dark theme background, elevated card shapes, and typography
 * of LearnPracticeScreen to eliminate any vibe-coded feel, with resilient layouts
 * that never squeeze or vertically stack text.
 */
@Composable
fun MoneyFlowBuilderScreen(
    onExit: () -> Unit,
    onComplete: ((Int) -> Unit)? = null,
    viewModel: MoneyFlowBuilderViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lockStatus = remember(state) { viewModel.getLockStatus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            // Atmospheric ambient radial glow matching LearnPracticeScreen
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF58A6FF).copy(alpha = 0.12f),
                            Color(0xFF8B5CF6).copy(alpha = 0.07f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.50f, size.height * 0.22f),
                        radius = size.width * 0.70f
                    )
                )
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Top App Bar (Exit button, 4-segment Progress Bar, Live XP Counter)
            TopAppBarSection(
                stage = state.stage,
                xp = state.xpEarned,
                onExit = onExit
            )

            // 2. Main Screen Area with Stage Switcher
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (state.stage) {
                    FlowStage.INTRO -> IntroSection(onStart = viewModel::startLesson)
                    FlowStage.BUILD, FlowStage.SURPRISE -> BuildStageSection(
                        state = state,
                        blocks = viewModel.blocks,
                        lockStatus = lockStatus,
                        onPlaceBlock = viewModel::placeBlock,
                        onAmountChanged = viewModel::setAmount,
                        onAmountAdjusted = viewModel::adjustAmount,
                        onLock = {
                            if (state.stage == FlowStage.BUILD) viewModel.lockPlan1()
                            else viewModel.lockPlan2()
                        },
                        getRowImpact = viewModel::getRowImpact
                    )
                    FlowStage.REFLECT -> ReflectSection(
                        state = state,
                        blocks = viewModel.blocks,
                        onContinue = viewModel::proceedToQuiz
                    )
                    FlowStage.QUIZ -> QuizSection(
                        state = state,
                        questions = viewModel.quizQuestions,
                        onSelectOption = viewModel::selectQuizOption,
                        onCheck = viewModel::checkQuizAnswer,
                        onNext = viewModel::nextQuizQuestion
                    )
                    FlowStage.DONE -> DoneSection(
                        state = state,
                        onFinish = {
                            onComplete?.invoke(state.xpEarned)
                            onExit()
                        },
                        onReplay = viewModel::resetLesson
                    )
                }
            }
        }

        // Surprise Shock Alert Dialog ("Day 19: The car won't start")
        if (state.shockAlertOpen) {
            SurpriseShockDialog(
                shockAmount = state.shockAmount,
                onAcknowledge = viewModel::acknowledgeShock
            )
        }
    }
}

/**
 * Top App Bar with segmented progress bar and XP badge matching LearnTopHeader.
 */
@Composable
private fun TopAppBarSection(
    stage: FlowStage,
    xp: Int,
    onExit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Exit button
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(DarkSurfaceVariant)
                .border(1.dp, DarkBorder, CircleShape)
                .clickable(onClick = onExit),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Exit",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        // 4-Segment Progress Bar (Intro/Build, Surprise, Reflect, Quiz/Done)
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (segment in 0..3) {
                val fillRatio = when {
                    stage.stepIndex > segment + 1 -> 1f
                    stage.stepIndex == segment + 1 -> 0.65f
                    stage.stepIndex == segment -> 0.15f
                    else -> 0f
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF263244))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fillRatio)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF5FE2D6), Color(0xFFC4EF75))
                                )
                            )
                    )
                }
            }
        }

        // Glowing Berry XP Pill
        Row(
            modifier = Modifier
                .height(38.dp)
                .clip(RoundedCornerShape(19.dp))
                .background(DarkSurfaceVariant)
                .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f), RoundedCornerShape(19.dp))
                .padding(horizontal = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BerryGemIcon(modifier = Modifier.size(17.dp))
            Text(
                text = "$xp XP",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}

/**
 * Step 0: Intro mental model screen.
 */
@Composable
private fun IntroSection(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Eyebrows
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IntroBadge("Trading Foundations")
                IntroBadge("Lesson 1")
                IntroBadge("About 8 min")
            }

            Text(
                text = "The Money Flow Builder",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                lineHeight = 36.sp,
                letterSpacing = (-0.5).sp
            )

            Text(
                text = "Before you risk a single dollar in real or simulated markets, learn where every dollar goes. Build a month of cash flow first, then see what a surprise does to your capital.",
                fontSize = 14.5.sp,
                color = TextSecondary,
                lineHeight = 21.sp
            )

            // Mini visual preview card with bespoke gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
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
                    .padding(14.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Left Income Bar
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF5FE2D6), Color(0xFF38B291))
                        ),
                        topLeft = Offset(8f, 10f),
                        size = Size(10f, h - 20f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )

                    // 4 sample ribbons
                    val ribbonData = listOf(
                        Triple(0.2f, 0.2f, Color(0xFF6C93F5)),
                        Triple(0.45f, 0.45f, Color(0xFFF59A5A)),
                        Triple(0.7f, 0.7f, Color(0xFF3CC7CC)),
                        Triple(0.9f, 0.9f, Color(0xFF52C286))
                    )

                    ribbonData.forEach { (srcYRatio, dstYRatio, color) ->
                        val p = Path().apply {
                            moveTo(18f, h * srcYRatio)
                            cubicTo(w * 0.45f, h * srcYRatio, w * 0.55f, h * dstYRatio, w - 80f, h * dstYRatio)
                        }
                        drawPath(path = p, color = color.copy(alpha = 0.55f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 10f))
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(w - 76f, h * dstYRatio - 8f),
                            size = Size(8f, 16f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                    }
                }
            }

            // 3-beat outline list
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                IntroBeatRow("BUILD", "Drag blocks into a flow and give every single dollar a job.")
                IntroBeatRow("EXPERIMENT", "A surprise bill arrives mid-month. Rearrange and watch what it costs.")
                IntroBeatRow("LEARN", "Three quick conceptual checks on why traders start here.")
            }
        }

        Button(
            onClick = onStart,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5FE2D6)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text(
                text = "Start building",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF040A06)
            )
        }
    }
}

@Composable
private fun IntroBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, DarkBorder, RoundedCornerShape(99.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )
    }
}

@Composable
private fun IntroBeatRow(label: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurface)
            .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF5FE2D6).copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = label,
                fontSize = 10.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5FE2D6),
                maxLines = 1,
                softWrap = false
            )
        }

        Text(
            text = desc,
            fontSize = 13.sp,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Step 1 & 2: Build & Surprise phase with live flow canvas, horizontal scroll block tray,
 * uncompressed allocation sliders, AI coach, and dock CTA.
 */
@Composable
private fun BuildStageSection(
    state: MoneyFlowState,
    blocks: List<MoneyBlock>,
    lockStatus: LockStatus,
    onPlaceBlock: (String) -> Unit,
    onAmountChanged: (String, Int) -> Unit,
    onAmountAdjusted: (String, Int) -> Unit,
    onLock: () -> Unit,
    getRowImpact: (MoneyBlock) -> RowImpact?
) {
    val isSurprisePhase = state.stage == FlowStage.SURPRISE
    val unplacedBlocks = remember(state.allocations, isSurprisePhase) {
        blocks.filter { b ->
            !state.isPlaced(b.id) && (isSurprisePhase || b.kind != BlockKind.SHOCK)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Pill: Status of current money flow
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSurprisePhase) "This month, with the repair" else "Monthly cash flow",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )

                    // Pill
                    val pillText = when {
                        !isSurprisePhase && state.allocations.isEmpty() -> "$${state.totalIncome} to assign"
                        !isSurprisePhase && state.unassignedDollars > 0 -> "$${state.unassignedDollars} left to assign"
                        !isSurprisePhase -> "Every dollar has a job"
                        state.unassignedDollars < 0 -> "Short $${-state.unassignedDollars}"
                        state.unassignedDollars > 0 -> "$${state.unassignedDollars} unassigned"
                        else -> "Fully funded"
                    }
                    val pillColor = when {
                        state.unassignedDollars < 0 -> Color(0xFFF85149)
                        state.unassignedDollars > 0 -> Color(0xFFFFC21A)
                        else -> Color(0xFF3FB950)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(pillColor.copy(alpha = 0.15f))
                            .border(1.dp, pillColor.copy(alpha = 0.4f), RoundedCornerShape(99.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = pillText,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = pillColor
                        )
                    }
                }
            }

            // Interactive Dynamic Sankey Canvas
            item {
                MoneyFlowCanvas(state = state, blocks = blocks)
            }

            // Block Tray (LazyRow prevents ANY vertical text squeezing or weird letter stacking)
            if (unplacedBlocks.isNotEmpty() && !isSurprisePhase) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "BLOCKS TO ASSIGN",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = TextTertiary
                            )
                            Text(
                                text = "Swipe & tap to add",
                                fontSize = 11.sp,
                                color = Color(0xFF5FE2D6)
                            )
                        }

                        // Horizontal scrolling tray with natural unconstrained widths
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(unplacedBlocks, key = { it.id }) { block ->
                                BlockTile(
                                    block = block,
                                    onClick = { onPlaceBlock(block.id) },
                                    modifier = Modifier.width(132.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Placed Allocation Rows
            item {
                Text(
                    text = if (isSurprisePhase) "REARRANGE TO COVER THE BILL" else "ACTIVE BUDGET ALLOCATIONS",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextTertiary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(blocks.filter { state.isPlaced(it.id) }, key = { it.id }) { block ->
                val amount = state.allocations[block.id] ?: 0
                val impact = getRowImpact(block)

                AllocationRowCard(
                    block = block,
                    amount = amount,
                    impact = impact,
                    isLocked = block.kind == BlockKind.SHOCK,
                    onAmountChanged = { onAmountChanged(block.id, it) },
                    onAdjust = { onAmountAdjusted(block.id, it) }
                )
            }
        }

        // Bottom Dock (Coach Speech Bubble + CTA Button)
        BottomDock(
            coachMessage = state.overrideMessage ?: state.coachMessage,
            buttonText = lockStatus.buttonText,
            isReady = lockStatus.isReady,
            onAction = onLock
        )
    }
}

@Composable
private fun BlockTile(
    block: MoneyBlock,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(1.2.dp, DarkBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Icon
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(block.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getBlockIcon(block.id),
                    contentDescription = null,
                    tint = block.color,
                    modifier = Modifier.size(17.dp)
                )
            }

            Text(
                text = block.name,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = when (block.kind) {
                    BlockKind.NEED -> "min $${block.min}"
                    BlockKind.GOAL -> "for shocks"
                    BlockKind.WANT -> "flexible"
                    BlockKind.SHOCK -> "$${block.min}"
                },
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * Resilient Allocation Row Card:
 * The left column takes flex weight(1f) and never gets compressed or vertically squeezed.
 */
@Composable
private fun AllocationRowCard(
    block: MoneyBlock,
    amount: Int,
    impact: RowImpact?,
    isLocked: Boolean,
    onAmountChanged: (Int) -> Unit,
    onAdjust: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DarkSurface)
            .border(
                1.dp,
                if (impact?.tone == ImpactTone.BAD) Color(0xFFF85149).copy(alpha = 0.5f) else DarkBorder,
                RoundedCornerShape(18.dp)
            )
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Top Row: Icon + Title/Tag (with weight 1f) + Fixed Steppers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category icon badge
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(block.color.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getBlockIcon(block.id),
                        contentDescription = null,
                        tint = block.color,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Left Title & Tag: Takes remaining width, NEVER squeezes into vertical letters
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = block.name,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when (block.kind) {
                            BlockKind.NEED -> "Need • min $${block.min}"
                            BlockKind.GOAL -> "Cushion for surprises"
                            BlockKind.WANT -> "Want • flexible"
                            BlockKind.SHOCK -> "Due today"
                        },
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextTertiary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Right Steppers & Readout (Fixed width, never encroaches on title)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!isLocked) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(DarkSurfaceVariant)
                                .border(1.dp, DarkBorder, RoundedCornerShape(9.dp))
                                .clickable { onAdjust(-50) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("-", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }

                    Text(
                        text = "$$amount",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        modifier = Modifier.width(58.dp),
                        textAlign = TextAlign.Center
                    )

                    if (!isLocked) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(DarkSurfaceVariant)
                                .border(1.dp, DarkBorder, RoundedCornerShape(9.dp))
                                .clickable { onAdjust(50) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }
                }
            }

            // Slider
            if (!isLocked) {
                Slider(
                    value = amount.toFloat(),
                    onValueChange = { onAmountChanged(it.toInt()) },
                    valueRange = 0f..block.max.toFloat(),
                    steps = (block.max / 10) - 1,
                    colors = SliderDefaults.colors(
                        thumbColor = block.color,
                        activeTrackColor = block.color,
                        inactiveTrackColor = Color(0xFF263244)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Impact statement
            if (impact != null) {
                val impactColor = when (impact.tone) {
                    ImpactTone.GOOD -> Color(0xFF3FB950)
                    ImpactTone.WARN -> Color(0xFFFFC21A)
                    ImpactTone.BAD -> Color(0xFFF85149)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(impactColor)
                    )
                    Text(
                        text = impact.text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = impactColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomDock(
    coachMessage: String,
    buttonText: String,
    isReady: Boolean,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .border(1.dp, DarkBorder)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Coach Bubble matching LearnPractice AI assistant style
        if (coachMessage.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                // AI Coach avatar circle
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF5FE2D6).copy(alpha = 0.2f))
                        .border(1.2.dp, Color(0xFF5FE2D6), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF5FE2D6),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSurfaceVariant)
                        .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "AI TRADING COACH",
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5FE2D6)
                        )
                        Text(
                            text = coachMessage,
                            fontSize = 12.5.sp,
                            color = TextPrimary,
                            lineHeight = 17.5.sp
                        )
                    }
                }
            }
        }

        // CTA Button
        Button(
            onClick = onAction,
            enabled = isReady,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF5FE2D6),
                disabledContainerColor = DarkSurfaceVariant
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = buttonText,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isReady) Color(0xFF040A06) else TextTertiary
            )
        }
    }
}

/**
 * Surprise Shock Dialog: Day 19 unexpected car repair bill.
 */
@Composable
private fun SurpriseShockDialog(
    shockAmount: Int,
    onAcknowledge: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(DarkSurface)
                .border(1.5.dp, Color(0xFFF26A6E), RoundedCornerShape(22.dp))
                .padding(22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Alert Icon
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF26A6E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Build,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "DAY 19 • UNEXPECTED EXPENSE",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF26A6E)
                )

                Text(
                    text = "The car won't start.",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )

                Text(
                    text = "The repair shop's quote is $$shockAmount, due today. Your monthly income hasn't changed, so something in your existing budget has to give.",
                    fontSize = 14.5.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )

                Button(
                    onClick = onAcknowledge,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5FE2D6)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "Rearrange my budget",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF040A06)
                    )
                }
            }
        }
    }
}

/**
 * Step 3: Consequence Reflection & The Bridge to Trading.
 */
@Composable
private fun ReflectSection(
    state: MoneyFlowState,
    blocks: List<MoneyBlock>,
    onContinue: () -> Unit
) {
    val p1 = state.plan1 ?: emptyMap()
    val p2 = state.plan2 ?: emptyMap()

    val rentShort = (p2["rent"] ?: 0) < 900
    val debtShort = (p2["debt"] ?: 0) < 200
    val totalFees = (if (rentShort) 45 else 0) + (if (debtShort) 35 else 0)
    val fromSavings = (p1["savings"] ?: 0) - (p2["savings"] ?: 0)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "What the surprise cost you",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
        }

        // 3 Badges with safe maxLines to prevent any vertical wrapping
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScoreBadge(
                    title = "BILLS",
                    value = if (rentShort || debtShort) "Short" else "All covered",
                    isGood = !rentShort && !debtShort,
                    modifier = Modifier.weight(1f)
                )
                ScoreBadge(
                    title = "CUSHION",
                    value = if (fromSavings > 0) "Used $$fromSavings" else "None saved",
                    isGood = fromSavings > 0,
                    modifier = Modifier.weight(1f)
                )
                ScoreBadge(
                    title = "FEES",
                    value = if (totalFees > 0) "$$totalFees due" else "None",
                    isGood = totalFees == 0,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Comparison Panel: Before vs After stacked bar
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkSurface)
                    .border(1.2.dp, DarkBorder, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("BEFORE: $${state.totalIncome}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
                    StackedBar(plan = p1, blocks = blocks, totalIncome = state.totalIncome)

                    Text("AFTER THE REPAIR: $${state.totalIncome}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
                    StackedBar(plan = p2, blocks = blocks, totalIncome = state.totalIncome)

                    // Deltas with proper flex weights
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        blocks.forEach { b ->
                            val a = p1[b.id] ?: 0
                            val c = p2[b.id] ?: 0
                            if (a != c) {
                                val delta = c - a
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(b.color))
                                        Text(
                                            text = b.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text("$$a → $$c", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
                                    Text(
                                        text = "${if (delta > 0) "+" else ""}$$delta",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = if (delta < 0) Color(0xFFFFC21A) else Color(0xFFF26A6E)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Order to Protect Money Ladder
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkSurface)
                    .border(1.dp, DarkBorder, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ORDER TO PROTECT MONEY", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextTertiary)
                    LadderRow("1 · Needs", "Rent, food, transport")
                    LadderRow("2 · Obligations", "Debt minimums")
                    LadderRow("3 · Cushion", "Savings for surprises")
                    LadderRow("4 · Wants", "Fun, upgrades, extras")
                }
            }
        }

        // The Bridge to Trading
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1B2433))
                    .border(1.2.dp, Color(0xFF5FE2D6).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "WHY THIS COMES FIRST IN TRADING",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5FE2D6)
                    )
                    Text(
                        text = "What is left after steps 1 to 3 is Risk Capital — the ONLY money a trade is ever allowed to touch. In real trading, a position size is just another block in your cash flow.",
                        fontSize = 13.5.sp,
                        color = Color.White,
                        lineHeight = 19.sp
                    )
                }
            }
        }

        item {
            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5FE2D6)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Check your thinking",
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF040A06)
                )
            }
        }
    }
}

@Composable
private fun ScoreBadge(
    title: String,
    value: String,
    isGood: Boolean,
    modifier: Modifier = Modifier
) {
    val bg = if (isGood) Color(0xFF3FB950).copy(alpha = 0.15f) else Color(0xFFF85149).copy(alpha = 0.15f)
    val color = if (isGood) Color(0xFF3FB950) else Color(0xFFF85149)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace, color = color)
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StackedBar(
    plan: Map<String, Int>,
    blocks: List<MoneyBlock>,
    totalIncome: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .clip(RoundedCornerShape(7.dp))
    ) {
        blocks.forEach { b ->
            val amt = plan[b.id] ?: 0
            if (amt > 0) {
                val weight = amt.toFloat() / totalIncome
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(weight)
                        .background(b.color)
                )
            }
        }
    }
}

@Composable
private fun LadderRow(step: String, detail: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(step, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(detail, fontSize = 12.sp, color = TextSecondary)
    }
}

/**
 * Step 4: Interactive Quiz Questions matching LearnPracticeScreen dialog aesthetic.
 */
@Composable
private fun QuizSection(
    state: MoneyFlowState,
    questions: List<QuizQuestion>,
    onSelectOption: (Int) -> Unit,
    onCheck: () -> Unit,
    onNext: () -> Unit
) {
    val q = questions.getOrNull(state.currentQuizIndex) ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "CHECK YOUR THINKING • ${state.currentQuizIndex + 1} OF ${questions.size}",
                fontSize = 11.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5FE2D6)
            )

            Text(
                text = q.question,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                lineHeight = 26.sp
            )

            // Options A, B, C
            q.options.forEachIndexed { index, option ->
                val isSelected = state.quizSelectedOption == index
                val isCorrectAnswer = index == q.correctIndex

                val borderColor = when {
                    state.quizIsChecked && isCorrectAnswer -> Color(0xFF3FB950)
                    state.quizIsChecked && isSelected && !isCorrectAnswer -> Color(0xFFF85149)
                    isSelected -> Color(0xFF5FE2D6)
                    else -> DarkBorder
                }

                val bgColor = when {
                    state.quizIsChecked && isCorrectAnswer -> Color(0xFF3FB950).copy(alpha = 0.15f)
                    state.quizIsChecked && isSelected && !isCorrectAnswer -> Color(0xFFF85149).copy(alpha = 0.15f)
                    isSelected -> Color(0xFF5FE2D6).copy(alpha = 0.10f)
                    else -> DarkSurface
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(bgColor)
                        .border(1.2.dp, borderColor, RoundedCornerShape(16.dp))
                        .clickable(enabled = !state.quizIsChecked) { onSelectOption(index) }
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceVariant)
                                .border(1.dp, borderColor, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${'A' + index}",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Text(
                            text = option,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = TextPrimary,
                            lineHeight = 19.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Explanation Card after checking
            if (state.quizIsChecked) {
                val isRight = state.quizSelectedOption == q.correctIndex
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isRight) Color(0xFF3FB950).copy(alpha = 0.12f) else Color(0xFFF85149).copy(alpha = 0.12f))
                        .border(1.2.dp, if (isRight) Color(0xFF3FB950) else Color(0xFFF85149), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (isRight) "✓ Right." else "✕ Not quite.",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isRight) Color(0xFF3FB950) else Color(0xFFF85149)
                        )
                        Text(
                            text = q.explanation,
                            fontSize = 12.5.sp,
                            color = TextPrimary,
                            lineHeight = 17.5.sp
                        )
                    }
                }
            }
        }

        // CTA Button
        Button(
            onClick = {
                if (!state.quizIsChecked) onCheck()
                else onNext()
            },
            enabled = state.quizSelectedOption != null,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5FE2D6)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = if (!state.quizIsChecked) "Check" else (if (state.currentQuizIndex == questions.size - 1) "Finish lesson" else "Continue"),
                fontSize = 15.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF040A06)
            )
        }
    }
}

/**
 * Step 5: Lesson Complete Celebration & XP Summary.
 */
@Composable
private fun DoneSection(
    state: MoneyFlowState,
    onFinish: () -> Unit,
    onReplay: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Checkmark Ring
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3FB950).copy(alpha = 0.2f))
                    .border(3.dp, Color(0xFF3FB950), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF3FB950),
                    modifier = Modifier.size(48.dp)
                )
            }

            Text(
                text = "Lesson complete",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )

            Text(
                text = "You built a budget, handled a surprise, and saw why traders start with both before risking capital.",
                fontSize = 14.5.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            // 3 Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DoneStatCard("+${state.xpEarned}", "XP EARNED", Modifier.weight(1f))
                DoneStatCard("${state.quizCorrectCount}/3", "QUIZ SCORE", Modifier.weight(1f))
                DoneStatCard("$${state.allocations["savings"] ?: 0}", "CUSHION LEFT", Modifier.weight(1f))
            }

            Text(
                text = "SKILLS PRACTICED",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = TextTertiary,
                modifier = Modifier.padding(top = 10.dp)
            )

            // Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                listOf("Budgeting", "Cash Flow", "Trade-offs", "Emergency Planning").forEach { skill ->
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, DarkBorder, RoundedCornerShape(99.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(skill, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onFinish,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5FE2D6)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Back to Learn & Practice",
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF040A06)
                )
            }

            OutlinedButton(
                onClick = onReplay,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Replay lesson",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun DoneStatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            Text(label, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextTertiary)
        }
    }
}

private fun getBlockIcon(id: String): ImageVector = when (id) {
    "rent" -> Icons.Outlined.Home
    "food" -> Icons.Outlined.Restaurant
    "transport" -> Icons.Outlined.DirectionsTransit
    "debt" -> Icons.Outlined.CreditCard
    "savings" -> Icons.Outlined.Security
    "fun" -> Icons.Outlined.SentimentSatisfiedAlt
    else -> Icons.Outlined.Build
}
