package com.trading.app.presentation.dashboard

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trading.app.domain.model.AssetClass
import com.trading.app.domain.model.PositionSide
import com.trading.app.presentation.components.Formatters
import com.trading.app.presentation.components.InstrumentSpecsBottomSheet
import com.trading.app.presentation.components.LearnAndPracticeCardSection
import com.trading.app.presentation.components.MultiAssetQuoteRow
import com.trading.app.presentation.components.NexoraDetailedPositionCard
import com.trading.app.presentation.components.NexoraDonutWheelView
import com.trading.app.presentation.components.NexoraHeroPortfolioCard
import com.trading.app.presentation.components.NexoraStackedMonthlyChart
import com.trading.app.presentation.components.NexoraTopAppBar
import com.trading.app.presentation.components.coachmark.CoachMarkManager
import com.trading.app.presentation.components.coachmark.CoachMarkOverlay
import com.trading.app.presentation.components.coachmark.rememberCoachMarkSteps
import com.trading.app.presentation.portfolio.DepositFundsBottomSheet
import com.trading.app.presentation.portfolio.WithdrawFundsBottomSheet
import com.trading.app.presentation.theme.DarkBackground
import com.trading.app.presentation.theme.DarkSurface
import com.trading.app.presentation.theme.TextPrimary
import com.trading.app.presentation.theme.TradingTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    onStockClick: (String) -> Unit,
    onGameClick: (String) -> Unit = {},
    onProfileClick: () -> Unit = {},
    onLearnClick: () -> Unit = {},
    onInvestClick: () -> Unit = onProfileClick,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coachMarkManager = remember { CoachMarkManager(context) }

    var heroCardBounds by remember { mutableStateOf<Rect?>(null) }
    var filterRowBounds by remember { mutableStateOf<Rect?>(null) }
    var learnCardBounds by remember { mutableStateOf<Rect?>(null) }

    var currentCoachStepIndex by remember { mutableIntStateOf(0) }
    var showCoachMarks by remember { mutableStateOf(!coachMarkManager.hasCompletedOnboarding) }

    val coachSteps = rememberCoachMarkSteps(
        watchlistTabBounds = filterRowBounds,
        tradingBalanceBounds = heroCardBounds,
        learnSectionBounds = learnCardBounds
    )

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Top App Bar (Stepped Logo + Title "Cash Cage" + Bell + Menu)
            item {
                NexoraTopAppBar(
                    onNotificationClick = { onProfileClick() },
                    onMenuClick = { viewModel.toggleViewMode() }
                )
            }

            // 2. View Mode Toggle Switch (Overview Card Mode vs. Donut Analytics Wheel Mode)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0F1A2C))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (state.selectedViewMode == DashboardViewMode.OVERVIEW) Color(0xFF26508A) else Color.Transparent)
                            .clickable { viewModel.setViewMode(DashboardViewMode.OVERVIEW) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.ViewAgenda, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Text("Portfolio Overview", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (state.selectedViewMode == DashboardViewMode.ANALYTICS_WHEEL) Color(0xFF26508A) else Color.Transparent)
                            .clickable { viewModel.setViewMode(DashboardViewMode.ANALYTICS_WHEEL) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.DonutLarge, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Text("Radial Analytics", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 3. Dynamic Center Section: Overview (Left Phone) OR Radial Wheel (Right Phone)
            if (state.selectedViewMode == DashboardViewMode.OVERVIEW) {
                // (A) Left Phone: Hero Portfolio Card (Live Real Account Equity & Floating Actions)
                item {
                    val formattedTotalEquity = Formatters.formatCurrency(state.totalEquity)
                    val formattedTodayReturnPct = String.format(Locale.US, "%+.2f%%", state.todayReturnPct)
                    val formattedDayPnL = String.format(
                        Locale.US,
                        "%s%s - Today",
                        if (state.dayPnL >= 0) "+" else "",
                        Formatters.formatCurrency(state.dayPnL)
                    )

                    Box(modifier = Modifier.onGloballyPositioned { coords -> heroCardBounds = coords.boundsInWindow() }) {
                        NexoraHeroPortfolioCard(
                            totalValue = formattedTotalEquity,
                            todayReturnPct = formattedTodayReturnPct,
                            todayDollarGain = formattedDayPnL,
                            onInvestClick = onInvestClick,
                            onAddFundClick = { viewModel.setShowDepositSheet(true) },
                            onWithdrawClick = { viewModel.setShowWithdrawSheet(true) }
                        )
                    }
                }

                // (B) Left Phone: Monthly Performance Stacked Bar Chart
                item {
                    NexoraStackedMonthlyChart()
                }
            } else {
                // (A) Right Phone: Radial Donut Allocation Wheel (Live Real Portfolio Allocation)
                item {
                    val totalEquityIntK = (state.totalEquity / 1000).toInt()
                    val centerValueStr = if (totalEquityIntK > 0) "\$${totalEquityIntK}k" else Formatters.formatCurrency(state.totalEquity)

                    NexoraDonutWheelView(
                        centerValue = centerValueStr,
                        onSegmentClick = { }
                    )
                }

                // (B) Right Phone: Detailed Position Cards with exact clip path using live positions
                if (state.positions.isNotEmpty()) {
                    items(state.positions) { pos ->
                        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US)
                        val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
                        val dateStr = dateFormat.format(Date(pos.openedAt))
                        val timeStr = timeFormat.format(Date(pos.openedAt))
                        val tag = if (pos.side == PositionSide.LONG) "Invested" else "Short / Margin"

                        NexoraDetailedPositionCard(
                            tag = tag,
                            timeStr = timeStr,
                            dateStr = dateStr,
                            symbolTitle = "${pos.companyName} (${pos.symbol})",
                            pctReturn = String.format(Locale.US, "%+.2f%%", pos.unrealizedPnLPercent),
                            dollarReturn = String.format(Locale.US, "%s%s - P&L", if (pos.isProfit) "+" else "", Formatters.formatCurrency(pos.unrealizedPnL)),
                            totalInvested = Formatters.formatCurrency(pos.costBasis),
                            profit = Formatters.formatCurrency(pos.unrealizedPnL),
                            modifier = Modifier.clickable { onStockClick(pos.symbol) }
                        )
                    }
                } else {
                    items(state.topMovers.take(3)) { quote ->
                        NexoraDetailedPositionCard(
                            tag = "Tradeable",
                            timeStr = "Live",
                            dateStr = "Market",
                            symbolTitle = "${quote.companyName} (${quote.symbol})",
                            pctReturn = String.format(Locale.US, "%+.2f%%", quote.changePercent),
                            dollarReturn = String.format(Locale.US, "%s%s - Today", if (quote.isPositive) "+" else "", Formatters.formatCurrency(quote.change)),
                            totalInvested = Formatters.formatCurrency(quote.lastPrice),
                            profit = if (quote.isPositive) "Bullish" else "Bearish",
                            modifier = Modifier.clickable { onStockClick(quote.symbol) }
                        )
                    }
                }
            }

            // 4. Learn & Practice Educational Card Carousel
            item {
                Box(modifier = Modifier.onGloballyPositioned { coords -> learnCardBounds = coords.boundsInWindow() }) {
                    LearnAndPracticeCardSection(
                        balance = state.totalEquity,
                        onAddMoneyClick = { onProfileClick() },
                        onLearnClick = { onLearnClick() },
                        onCardClick = { onLearnClick() }
                    )
                }
            }

            // 5. Multi-Asset Terminal Section
            item {
                Box(modifier = Modifier.onGloballyPositioned { coords -> filterRowBounds = coords.boundsInWindow() }) {
                    Column {
                        Text("Multi-Asset Terminal", style = TradingTypography.sectionHeader, color = TextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                AssetFilterChip(
                                    label = "All Markets",
                                    isSelected = state.selectedAssetClass == null,
                                    onClick = { viewModel.selectAssetClass(null) }
                                )
                            }
                            items(AssetClass.entries) { assetClass ->
                                AssetFilterChip(
                                    label = assetClass.displayName,
                                    isSelected = state.selectedAssetClass == assetClass,
                                    onClick = { viewModel.selectAssetClass(assetClass) }
                                )
                            }
                        }
                    }
                }
            }

            // 6. Multi-Asset Live Quotes List
            items(state.multiAssetQuotes, key = { it.symbol }) { quote ->
                MultiAssetQuoteRow(
                    quote = quote,
                    onClick = { onStockClick(quote.symbol) },
                    onInfoClick = { viewModel.showSpecs(quote) }
                )
            }
        }

        // Instrument Specifications Bottom Sheet
        state.selectedSpecsQuote?.let { quote ->
            InstrumentSpecsBottomSheet(
                quote = quote,
                onDismiss = viewModel::dismissSpecs
            )
        }

        // Quick Deposit Bottom Sheet from Hero Action Button [Add Fund]
        if (state.showDepositSheet) {
            DepositFundsBottomSheet(
                onDismiss = { viewModel.setShowDepositSheet(false) },
                onDeposit = { amount ->
                    viewModel.depositFunds(amount)
                    viewModel.setShowDepositSheet(false)
                    Toast.makeText(context, "Deposited ${Formatters.formatCurrency(amount)} into Trading Balance", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Quick Withdraw Bottom Sheet from Hero Action Button [Withdraw]
        if (state.showWithdrawSheet) {
            WithdrawFundsBottomSheet(
                onDismiss = { viewModel.setShowWithdrawSheet(false) },
                onWithdraw = { amount ->
                    viewModel.withdrawFunds(amount)
                    viewModel.setShowWithdrawSheet(false)
                    Toast.makeText(context, "Withdrew ${Formatters.formatCurrency(amount)} from Trading Balance", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Onboarding Coach Marks Overlay with Curled Arrows
        if (showCoachMarks && coachSteps.isNotEmpty()) {
            val step = coachSteps[currentCoachStepIndex.coerceIn(0, coachSteps.size - 1)]
            CoachMarkOverlay(
                visible = showCoachMarks,
                currentStep = step,
                stepIndex = currentCoachStepIndex,
                totalSteps = coachSteps.size,
                onNext = {
                    if (currentCoachStepIndex < coachSteps.size - 1) {
                        currentCoachStepIndex++
                    } else {
                        showCoachMarks = false
                        coachMarkManager.hasCompletedOnboarding = true
                    }
                },
                onDismissAll = {
                    showCoachMarks = false
                    coachMarkManager.hasCompletedOnboarding = true
                }
            )
        }
    }
}

@Composable
private fun AssetFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF26508A) else Color(0xFF1E2D44))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color(0xFF94A3B8)
        )
    }
}
