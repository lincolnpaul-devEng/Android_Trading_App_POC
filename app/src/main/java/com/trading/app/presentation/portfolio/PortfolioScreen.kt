package com.trading.app.presentation.portfolio

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.core.Fit
import app.rive.runtime.kotlin.core.Alignment as RiveAlignment
import com.trading.app.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.trading.app.domain.model.*
import com.trading.app.presentation.components.FlashingPriceText
import com.trading.app.presentation.components.Formatters
import com.trading.app.presentation.theme.*
import java.util.Locale
import kotlin.math.abs

// Styling tokens matching the uploaded design
private val DesignBlack = Color(0xFF000000)
private val DesignDarkSurface = Color(0xFF101216)
private val DesignButtonGrey = Color(0xFF1E2127)
private val DesignVerifiedGreen = Color(0xFF22C55E)
private val DesignLossRed = Color(0xFFF85149)
private val DesignPillRedBg = Color(0xFF2E1518)
private val DesignSolanaPurple = Color(0xFF9945FF)
private val DesignSolanaTeal = Color(0xFF14F195)
private val DesignUsdcBlue = Color(0xFF2775CA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    onStockClick: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: PortfolioViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Live reactive metrics driven by simulated market ticks and repository state
    val activeTfStats = state.activeTimeframeStats
    val isProfit = activeTfStats.isProfit
    val cashBalanceDisplay = Formatters.formatCurrency(state.rawCashBalance)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignBlack)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(DesignBlack)
        ) {
            // 1. Cosmic Hero Banner extending directly to the mobile top end
            item {
                PortfolioCosmicBanner()
            }

            // 2. Profile Header Section (Clean Trading Account identity without social follower clutter)
            item {
                PortfolioProfileHeader(
                    accountName = if (state.accountType == com.trading.app.domain.model.AccountType.DEMO) "Paper Trading ($100K Demo)" else "Live Quotes Paper Sandbox",
                    handle = if (state.accountType == com.trading.app.domain.model.AccountType.DEMO) "@demo_trader • Practice" else "@sandbox_alpha • Live Quotes",
                    analytics = state.analytics,
                    onViewStatsClick = { viewModel.setShowStatsSheet(true) },
                    onDepositClick = { viewModel.setShowDepositSheet(true) },
                    onWithdrawClick = { viewModel.setShowWithdrawSheet(true) },
                    onCopyHandle = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Account", "Account #TRD-884201 (@trader_alpha)"))
                        Toast.makeText(context, "Copied Account #TRD-884201 to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // 2b. Demo Account, Live Feed, Statement & Security Control Strip
            item {
                Spacer(modifier = Modifier.height(10.dp))
                AccountControlStrip(
                    accountType = state.accountType,
                    dataSourceMode = state.dataSourceMode,
                    onToggleAccountType = {
                        viewModel.toggleAccountType()
                        Toast.makeText(
                            context,
                            "Switched to ${if (state.accountType == com.trading.app.domain.model.AccountType.DEMO) "Live Quotes Paper Sandbox" else "Demo Practice"} (Virtual capital)",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onResetDemo = {
                        viewModel.resetDemoAccount()
                        Toast.makeText(context, "Demo Account reset to $100,000.00", Toast.LENGTH_SHORT).show()
                    },
                    onToggleFeed = { viewModel.toggleDataSourceMode() },
                    onOpenStatements = { viewModel.setShowStatementSheet(true) },
                    onLockAccount = { viewModel.setAccountLocked(true) }
                )
            }

            // 3. Portfolio Total Net Worth & Dynamic Timeframe Selection (Flickering live trade values)
            item {
                Spacer(modifier = Modifier.height(18.dp))
                PortfolioValueSection(
                    totalEquity = activeTfStats.totalEquity,
                    formattedTotalValue = activeTfStats.formattedTotalValue,
                    pnlAmount = activeTfStats.pnlAmount,
                    formattedPnL = activeTfStats.formattedPnL,
                    pnlPercent = activeTfStats.pnlPercent,
                    formattedPnLPercent = activeTfStats.formattedPnLPercent,
                    isProfit = isProfit,
                    selectedTimeframe = state.selectedTimeframe,
                    onSelectTimeframe = { viewModel.selectTimeframe(it) }
                )
            }

            // 4. Dynamic Interactive Performance Line Chart (Varying with simulated ticks)
            item {
                Spacer(modifier = Modifier.height(10.dp))
                PortfolioInteractiveChart(
                    points = activeTfStats.chartPoints,
                    isProfit = isProfit,
                    timeframe = state.selectedTimeframe
                )
            }

            // 5. Horizontal Tab Bar: Positions (N), Orders (N), Analytics, Margin, Bundles
            item {
                Spacer(modifier = Modifier.height(16.dp))
                PortfolioTabBar(
                    selectedTab = state.selectedTab,
                    positionsCount = state.holdings.size,
                    ordersCount = state.openOrders.size,
                    onTabSelected = { viewModel.selectTab(it) }
                )
            }

            // 6. Clean Balance & Sort Filter Row (when on Positions tab)
            if (state.selectedTab == 0) {
                item {
                    PortfolioBalanceAndSortRow(
                        rawCashBalance = state.rawCashBalance,
                        cashBalance = cashBalanceDisplay,
                        sortOption = state.sortOption,
                        onToggleSort = { viewModel.toggleSort() }
                    )
                }
            }

            // 7. Tab Content
            when (state.selectedTab) {
                0 -> {
                    // Positions Tab (Binance / FxPro style institutional cards)
                    if (state.holdings.isEmpty()) {
                        item {
                            EmptyStateView(
                                title = "No Open Positions",
                                subtitle = "Explore markets or indices on the terminal to place a trade"
                            )
                        }
                    } else {
                        items(state.holdings, key = { it.symbol }) { item ->
                            PortfolioHoldingRow(
                                item = item,
                                onClick = { onStockClick(item.symbol) },
                                onClosePosition = {
                                    viewModel.closePosition(item.symbol)
                                    Toast.makeText(context, "Closed ${item.symbol} position", Toast.LENGTH_SHORT).show()
                                },
                                onPartialClose = { viewModel.openPartialClose(item) },
                                onReverse = {
                                    viewModel.reversePosition(item.symbol)
                                    Toast.makeText(context, "Reversed ${item.symbol} position", Toast.LENGTH_SHORT).show()
                                },
                                onEditSlTp = { viewModel.openEditSlTp(item) }
                            )
                        }
                    }
                }
                1 -> {
                    // Orders Tab (Working Pending Orders with Cancel + Historical Orders)
                    item {
                        Text(
                            text = "Working Orders (${state.openOrders.size})",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    if (state.openOrders.isEmpty()) {
                        item {
                            EmptyStateView(
                                title = "No Active Working Orders",
                                subtitle = "Unfilled Limit and Stop orders will wait here with instant cancellation"
                            )
                        }
                    } else {
                        items(state.openOrders, key = { it.id }) { ord ->
                            PortfolioWorkingOrderRow(
                                order = ord,
                                onCancel = {
                                    viewModel.cancelWorkingOrder(ord.id)
                                    Toast.makeText(context, "Cancelled order #${ord.id}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Order Fill History",
                            color = Color(0xFF8B949E),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                    items(state.orders, key = { it.id }) { order ->
                        PortfolioOrderRow(order)
                    }
                }
                2 -> {
                    // Analytics & Trade Journal Tab (Win Rate, Profit Factor, Expectancy, Drawdown, Asset Breakdown)
                    item {
                        TraderAnalyticsDashboardView(
                            analytics = state.analytics,
                            closedTrades = state.closedTrades
                        )
                    }
                }
                3 -> {
                    // Margin, Statements & Security
                    item {
                        MarginSpecsSummaryView(
                            portfolio = state.portfolio,
                            onOpenStatements = { viewModel.setShowStatementSheet(true) },
                            onResetDemo = {
                                viewModel.resetDemoAccount()
                                Toast.makeText(context, "Demo Account reset to $100,000.00", Toast.LENGTH_SHORT).show()
                            },
                            onLockAccount = { viewModel.setAccountLocked(true) }
                        )
                    }
                }
                4 -> {
                    // Packs / Asset Bundles
                    item {
                        PacksView(onSelectSymbol = onStockClick)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // View Stats BottomSheet
    if (state.showStatsSheet) {
        AccountStatsBottomSheet(
            portfolio = state.portfolio,
            onDismiss = { viewModel.setShowStatsSheet(false) }
        )
    }

    // Quick Deposit BottomSheet
    if (state.showDepositSheet) {
        DepositFundsBottomSheet(
            onDismiss = { viewModel.setShowDepositSheet(false) },
            onDeposit = { amount ->
                viewModel.depositFunds(amount)
                Toast.makeText(context, "Deposited ${Formatters.formatCurrency(amount)} into Trading Balance", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Quick Withdraw BottomSheet
    if (state.showWithdrawSheet) {
        WithdrawFundsBottomSheet(
            onDismiss = { viewModel.setShowWithdrawSheet(false) },
            onWithdraw = { amount ->
                viewModel.withdrawFunds(amount)
                Toast.makeText(context, "Withdrew ${Formatters.formatCurrency(amount)} from Trading Balance", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Account Statement & Transaction Ledger BottomSheet
    if (state.showStatementSheet) {
        AccountStatementBottomSheet(
            statement = state.statement,
            portfolio = state.portfolio,
            transactions = state.transactions,
            closedTrades = state.closedTrades,
            onDismiss = { viewModel.setShowStatementSheet(false) }
        )
    }

    // Secure Account Lock Gate (PIN + Biometric Unlock)
    if (state.isAccountLocked) {
        AccountSecurityLockOverlay(
            pinError = state.pinError,
            onUnlockWithPin = { pin -> viewModel.unlockWithPin(pin) },
            onBiometricUnlock = { viewModel.unlockWithPin("1234") }
        )
    }

    // Partial Close BottomSheet
    state.partialClosingHolding?.let { holding ->
        PartialCloseBottomSheet(
            holding = holding,
            onDismiss = { viewModel.dismissPartialClose() },
            onConfirmClose = { fraction ->
                viewModel.partialClosePosition(holding.symbol, fraction)
                viewModel.dismissPartialClose()
                Toast.makeText(context, "Closed ${(fraction * 100).toInt()}% of ${holding.symbol}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Edit TP/SL BottomSheet
    state.editingSlTpHolding?.let { holding ->
        EditSlTpBottomSheet(
            holding = holding,
            onDismiss = { viewModel.dismissEditSlTp() },
            onConfirm = { tp, sl ->
                viewModel.updatePositionSlTp(holding.symbol, tp, sl)
                viewModel.dismissEditSlTp()
                Toast.makeText(context, "Updated TP/SL for ${holding.symbol}", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

// -------------------------------------------------------------
// Cosmic Space Banner (PNG Asset)
// -------------------------------------------------------------

@Composable
private fun PortfolioCosmicBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(Color(0xFF061A14))
    ) {
        Image(
            painter = painterResource(id = R.drawable.cosmic_banner),
            contentDescription = "Cosmic Banner",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

// -------------------------------------------------------------
// Profile Header Section (Clean Trading Account Identity)
// -------------------------------------------------------------

@Composable
private fun PortfolioProfileHeader(
    accountName: String,
    handle: String,
    analytics: com.trading.app.domain.model.TraderPerformanceAnalytics,
    onViewStatsClick: () -> Unit,
    onDepositClick: () -> Unit,
    onWithdrawClick: () -> Unit,
    onCopyHandle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Avatar row overlapping banner seam
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-32).dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Profile Avatar with Neon Green Border (PNG Asset)
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F1412))
                    .border(3.dp, DesignVerifiedGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.profile_avatar),
                    contentDescription = "Profile Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(66.dp)
                        .clip(CircleShape)
                )
            }

            // "View stats" Pill Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF14171E))
                    .border(1.dp, Color(0xFF262C38), RoundedCornerShape(20.dp))
                    .clickable(onClick = onViewStatsClick)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = "View stats",
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "View stats",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Account Handle & Title Row
        Row(
            modifier = Modifier
                .offset(y = (-18).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = handle,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy Account Number",
                tint = Color(0xFF8B949E),
                modifier = Modifier
                    .size(17.dp)
                    .clickable(onClick = onCopyHandle)
            )
        }

        // Professional Account Badges (Replaces dummy social counters)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .offset(y = (-12).dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Verified Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(DesignVerifiedGreen.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "VIP Tier 1",
                    color = DesignVerifiedGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }

            // Margin Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(PrimaryBlue.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "20x Margin Active",
                    color = PrimaryBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }

            // Account Type
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF21262D))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Verified Trader",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // Performance Summary Row (Dynamic real-time trader metrics)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .offset(y = (-6).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val winRateStr = String.format(java.util.Locale.US, "Win Rate: %.1f%%", analytics.winRatePercent)
            val pfStr = String.format(java.util.Locale.US, "PF: %.2f", analytics.profitFactor)
            Text(text = winRateStr, color = Color(0xFF8B949E), fontSize = 12.sp, maxLines = 1, softWrap = false)
            Text(text = "•", color = Color(0xFF484F58), fontSize = 12.sp, maxLines = 1, softWrap = false)
            Text(text = "${analytics.totalTrades} Trades", color = Color(0xFF8B949E), fontSize = 12.sp, maxLines = 1, softWrap = false)
            Text(text = "•", color = Color(0xFF484F58), fontSize = 12.sp, maxLines = 1, softWrap = false)
            Text(text = pfStr, color = if (analytics.profitFactor >= 1.0) DesignVerifiedGreen else DesignLossRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
            Text(text = "•", color = Color(0xFF484F58), fontSize = 12.sp, maxLines = 1, softWrap = false)
            Text(text = analytics.traderGrade, color = PrimaryBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Profile Action Buttons: Deposit Funds | Withdraw
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF1F6FEB))
                    .clickable(onClick = onDepositClick),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Deposit",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF1E2127))
                    .clickable(onClick = onWithdrawClick),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Withdraw",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Portfolio Net Worth & Dynamic Timeframe Selection
// -------------------------------------------------------------

@Composable
private fun PortfolioValueSection(
    totalEquity: Double,
    formattedTotalValue: String,
    pnlAmount: Double,
    formattedPnL: String,
    pnlPercent: Double,
    formattedPnLPercent: String,
    isProfit: Boolean,
    selectedTimeframe: String,
    onSelectTimeframe: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Big Bold Headline Balance with live green/red price tick flash
        FlashingPriceText(
            price = totalEquity,
            formattedPrice = formattedTotalValue,
            textStyle = TextStyle(
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            ),
            textColor = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Change Indicators & Timeframe Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // P&L Metrics with real-time flashing text and direction indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val textColor = if (isProfit) DesignVerifiedGreen else DesignLossRed
                val pillBg = if (isProfit) Color(0xFF132B1E) else DesignPillRedBg

                FlashingPriceText(
                    price = pnlAmount,
                    formattedPrice = formattedPnL,
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    textColor = textColor
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(pillBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = if (isProfit) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = formattedPnLPercent.replace("+", "").replace("-", "").trim(),
                            color = textColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Timeframe Pills (1D | 1W | 1M | All)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF14171E))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("1D", "1W", "1M", "All").forEach { tf ->
                    val isSelected = selectedTimeframe == tf
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) Color.White else Color.Transparent)
                            .clickable { onSelectTimeframe(tf) }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tf,
                            color = if (isSelected) Color.Black else Color(0xFF8B949E),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Dynamic Interactive Portfolio Performance Line Chart
// -------------------------------------------------------------

@Composable
private fun PortfolioInteractiveChart(
    points: List<Float>,
    isProfit: Boolean,
    timeframe: String
) {
    var touchX by remember { mutableStateOf<Float?>(null) }

    // Live continuous pulse radar animation around the current price end dot
    val infiniteTransition = rememberInfiniteTransition(label = "chartPulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 4.5f,
        targetValue = 13f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp)
            .padding(horizontal = 16.dp)
            .pointerInput(timeframe) {
                detectDragGestures(
                    onDragStart = { offset -> touchX = offset.x },
                    onDrag = { change, _ -> touchX = change.position.x },
                    onDragEnd = { touchX = null },
                    onDragCancel = { touchX = null }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            if (points.isEmpty()) return@Canvas

            // 1. Dotted Matrix Background Grid
            val numCols = 24
            val numRows = 7
            val colStep = width / (numCols - 1)
            val rowStep = height / (numRows - 1)

            for (c in 0 until numCols) {
                for (r in 0 until numRows) {
                    drawCircle(
                        color = Color(0x18FFFFFF),
                        radius = 1.3f,
                        center = Offset(c * colStep, r * rowStep)
                    )
                }
            }

            // 2. Dynamic Performance Line Path
            val chartColor = if (isProfit) DesignVerifiedGreen else DesignLossRed
            val path = Path()
            val fillPath = Path()

            val stepX = width / (points.size - 1)

            points.forEachIndexed { index, ny ->
                val x = index * stepX
                val y = height * ny

                if (index == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }

            fillPath.lineTo(width, height)
            fillPath.close()

            // Subtle gradient area fill underneath
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        chartColor.copy(alpha = 0.16f),
                        Color.Transparent
                    )
                )
            )

            // Primary jagged stroke line
            drawPath(
                path = path,
                color = chartColor,
                style = Stroke(
                    width = 2.4f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Current ending point dot with live pulsing radar ring
            val lastX = width
            val lastY = height * points.last()

            drawCircle(
                color = chartColor.copy(alpha = pulseAlpha),
                radius = pulseRadius,
                center = Offset(lastX, lastY)
            )
            drawCircle(
                color = chartColor,
                radius = 4.5f,
                center = Offset(lastX, lastY)
            )
            drawCircle(
                color = Color.White,
                radius = 2.2f,
                center = Offset(lastX, lastY)
            )

            // Interactive scrubber line when user drags
            touchX?.let { tx ->
                val clampedX = tx.coerceIn(0f, width)
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(clampedX, 0f),
                    end = Offset(clampedX, height),
                    strokeWidth = 1.2f
                )
                val normalizedIndex = ((clampedX / width) * (points.size - 1)).toInt().coerceIn(0, points.size - 1)
                val dotY = height * points[normalizedIndex]
                drawCircle(
                    color = Color.White,
                    radius = 4.5f,
                    center = Offset(clampedX, dotY)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Horizontal Tab Bar: Positions (N) | Orders (N) | Analytics | Margin | Bundles
// -------------------------------------------------------------

@Composable
private fun PortfolioTabBar(
    selectedTab: Int,
    positionsCount: Int,
    ordersCount: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf(
        "Positions ($positionsCount)",
        "Orders ($ordersCount)",
        "Analytics",
        "Margin",
        "Bundles"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, label ->
                val isSelected = selectedTab == index
                Column(
                    modifier = Modifier
                        .clickable { onTabSelected(index) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else Color(0xFF7D8590),
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Solid white active underline
                    Box(
                        modifier = Modifier
                            .height(2.5.dp)
                            .width(if (isSelected) 36.dp else 0.dp)
                            .background(Color.White, RoundedCornerShape(2.dp))
                    )
                }
            }
        }

        // Subtle divider rule
        HorizontalDivider(color = Color(0xFF161B22), thickness = 1.dp)
    }
}

// -------------------------------------------------------------
// Clean Balance & Sort Filter Row (No weird characters or glyphs)
// -------------------------------------------------------------

@Composable
private fun PortfolioBalanceAndSortRow(
    rawCashBalance: Double,
    cashBalance: String,
    sortOption: String,
    onToggleSort: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Dual Overlapping Token Badge + Balance
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Overlapping tokens
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // Token 1: USDC Blue
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(DesignUsdcBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "$", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                // Token 2: Solana Purple/Teal
                Box(
                    modifier = Modifier
                        .offset(x = 11.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(DesignSolanaPurple, DesignSolanaTeal)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "S", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = "Balance",
                color = Color(0xFF8B949E),
                fontSize = 14.sp
            )

            FlashingPriceText(
                price = rawCashBalance,
                formattedPrice = cashBalance,
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                ),
                textColor = Color.White
            )
        }

        // Clean Sort Button with standard Material Sort icon
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF14171E))
                .clickable(onClick = onToggleSort)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Sort,
                contentDescription = "Sort Filter",
                tint = Color(0xFF8B949E),
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = sortOption,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = Color(0xFF8B949E),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// -------------------------------------------------------------
// Position / Holding Row Card (Binance / FxPro Style)
// -------------------------------------------------------------

@Composable
private fun PortfolioHoldingRow(
    item: PortfolioHoldingItem,
    onClick: () -> Unit,
    onClosePosition: () -> Unit = {},
    onPartialClose: () -> Unit = {},
    onReverse: () -> Unit = {},
    onEditSlTp: () -> Unit = {}
) {
    val isLong = item.side == PositionSide.LONG
    val sideColor = if (isLong) DesignVerifiedGreen else DesignLossRed
    val pnlColor = if (item.isProfit) DesignVerifiedGreen else DesignLossRed

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
        border = BorderStroke(1.dp, Color(0xFF21262D))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Symbol, Side badge, Leverage, and PnL / ROE%
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Mascot or Avatar
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    item.isPenguin -> Color(0xFFEFF6FF)
                                    item.isLilPengu -> Color(0xFFF0FDF4)
                                    else -> Color(0xFF0D1117)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (item.isPenguin) {
                            AndroidView(
                                modifier = Modifier.size(28.dp),
                                factory = { ctx ->
                                    RiveAnimationView(ctx).apply {
                                        setRiveResource(resId = R.raw.pudgy_penguin, autoplay = true)
                                    }
                                }
                            )
                        } else if (item.isLilPengu) {
                            AndroidView(
                                modifier = Modifier.size(28.dp),
                                factory = { ctx ->
                                    RiveAnimationView(ctx).apply {
                                        setRiveResource(resId = R.raw.lil_pengu, autoplay = true)
                                    }
                                }
                            )
                        } else {
                            Text(
                                text = item.symbol.take(3),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Symbol & Contract
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = item.symbol,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            // Long/Short Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(sideColor.copy(alpha = 0.18f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isLong) "LONG" else "SHORT",
                                    color = sideColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            // Leverage Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF21262D))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${item.leverage}x",
                                    color = Color(0xFF8B949E),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Unrealized PnL & ROE
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = item.formattedPnL ?: "$0.00",
                        color = pnlColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ROE ${item.formattedPnLPercent ?: "0.00%"}",
                        color = pnlColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Metrics Grid: Entry, Mark, Margin, Size, Liq Price, TP/SL
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D1117), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PositionMetricItem(label = "Entry Price", value = Formatters.formatCurrency(item.entryPrice))
                    PositionMetricItem(label = "Mark Price", value = Formatters.formatCurrency(item.currentPrice))
                    PositionMetricItem(
                        label = "Est. Liq Price",
                        value = if (item.liquidationPrice > 0.0) Formatters.formatCurrency(item.liquidationPrice) else "--",
                        valueColor = if (item.liquidationPrice > 0.0) Color(0xFFE3B341) else Color(0xFF8B949E)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PositionMetricItem(
                        label = "Size (${item.symbol.substringBefore('/')})",
                        value = String.format(Locale.US, "%.4f", item.units).trimEnd('0').trimEnd('.')
                    )
                    PositionMetricItem(label = "Margin", value = Formatters.formatCurrency(item.usedMargin))
                    val tpDisplay = item.takeProfitPrice?.let { Formatters.formatCurrency(it) } ?: "--"
                    val slDisplay = item.stopLossPrice?.let { Formatters.formatCurrency(it) } ?: "--"
                    PositionMetricItem(label = "TP / SL", value = "$tpDisplay / $slDisplay")
                }
            }

            // Quick Institutional Action Bar: [TP/SL], [Partial], [Reverse], [Close]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEditSlTp,
                    modifier = Modifier.weight(1f).height(32.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0xFF30363D))
                ) {
                    Text("TP/SL", color = Color(0xFFC9D1D9), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onPartialClose,
                    modifier = Modifier.weight(1f).height(32.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0xFF30363D))
                ) {
                    Text("Partial", color = Color(0xFFC9D1D9), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onReverse,
                    modifier = Modifier.weight(1f).height(32.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0xFF30363D))
                ) {
                    Text("Reverse", color = Color(0xFF58A6FF), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onClosePosition,
                    modifier = Modifier.weight(1f).height(32.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DesignLossRed.copy(alpha = 0.2f))
                ) {
                    Text("Close", color = DesignLossRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PositionMetricItem(
    label: String,
    value: String,
    valueColor: Color = Color(0xFFC9D1D9)
) {
    Column {
        Text(
            text = label,
            color = Color(0xFF8B949E),
            fontSize = 10.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// -------------------------------------------------------------
// Working Pending Order Row Card
// -------------------------------------------------------------

@Composable
private fun PortfolioWorkingOrderRow(
    order: Order,
    onCancel: () -> Unit
) {
    val isBuy = order.side == com.trading.app.domain.model.OrderSide.BUY
    val sideColor = if (isBuy) DesignVerifiedGreen else DesignLossRed

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
        border = BorderStroke(1.dp, Color(0xFF21262D))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(sideColor.copy(alpha = 0.18f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isBuy) "BUY" else "SELL",
                            color = sideColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Text(
                        text = order.symbol,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = order.type.name.replace("_", " "),
                        color = Color(0xFF8B949E),
                        fontSize = 11.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Price: ${order.price?.let { Formatters.formatCurrency(it) } ?: "Market"}",
                        color = Color(0xFFC9D1D9),
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Amount: ${String.format(Locale.US, "%.4f", order.quantity).trimEnd('0').trimEnd('.')}",
                        color = Color(0xFF8B949E),
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.height(30.dp),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, DesignLossRed.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DesignLossRed),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Text(
                    text = "Cancel",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Trader Analytics Dashboard & Journal View
// -------------------------------------------------------------

@Composable
private fun TraderAnalyticsDashboardView(
    analytics: TraderPerformanceAnalytics,
    closedTrades: List<ClosedTrade>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Overall Grade & Summary Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            border = BorderStroke(1.dp, Color(0xFF21262D))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Trader Performance Score",
                        color = Color(0xFF8B949E),
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Grade ${analytics.traderGrade}",
                            color = Color(0xFF58A6FF),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                        val streakText = when {
                            analytics.currentStreak > 0 -> "${analytics.currentStreak}W Streak 🔥"
                            analytics.currentStreak < 0 -> "${abs(analytics.currentStreak)}L Streak ❄️"
                            else -> "No Streak"
                        }
                        val isWinStreak = analytics.currentStreak > 0
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isWinStreak) DesignVerifiedGreen.copy(alpha = 0.2f)
                                    else Color(0xFF21262D)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = streakText,
                                color = if (isWinStreak) DesignVerifiedGreen else Color(0xFF8B949E),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Win rate pill
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format(Locale.US, "%.1f%%", analytics.winRatePercent),
                        color = if (analytics.winRatePercent >= 50.0) DesignVerifiedGreen else DesignLossRed,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "Win Rate (${analytics.winningTrades}W / ${analytics.losingTrades}L)",
                        color = Color(0xFF8B949E),
                        fontSize = 11.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 4 Key Performance Indicators Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TraderKpiCard(
                title = "Profit Factor",
                value = String.format(Locale.US, "%.2f", analytics.profitFactor),
                subtitle = if (analytics.profitFactor >= 1.5) "Strong Edge" else if (analytics.profitFactor >= 1.0) "Breakeven" else "Negative",
                valueColor = if (analytics.profitFactor >= 1.0) DesignVerifiedGreen else DesignLossRed,
                modifier = Modifier.weight(1f)
            )
            TraderKpiCard(
                title = "Expectancy",
                value = Formatters.formatCurrency(analytics.expectancyPerTrade),
                subtitle = "Per Trade Edge",
                valueColor = if (analytics.expectancyPerTrade >= 0) DesignVerifiedGreen else DesignLossRed,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TraderKpiCard(
                title = "Max Drawdown",
                value = String.format(Locale.US, "%.1f%%", analytics.maxDrawdownPercent),
                subtitle = "Peak-to-Trough",
                valueColor = if (analytics.maxDrawdownPercent < 15.0) DesignVerifiedGreen else DesignLossRed,
                modifier = Modifier.weight(1f)
            )
            TraderKpiCard(
                title = "Payoff Ratio",
                value = String.format(Locale.US, "%.2f", analytics.payoffRatio),
                subtitle = "Avg Win / Avg Loss",
                valueColor = if (analytics.payoffRatio >= 1.5) DesignVerifiedGreen else Color.White,
                modifier = Modifier.weight(1f)
            )
        }

        // Directional Edge
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            border = BorderStroke(1.dp, Color(0xFF21262D))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Directional Edge",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Long Positions (${analytics.longTradesCount})",
                        color = Color(0xFF8B949E),
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${String.format(Locale.US, "%.1f%%", analytics.longWinRatePercent)} Win Rate",
                        color = if (analytics.longWinRatePercent >= 50.0) DesignVerifiedGreen else Color(0xFFC9D1D9),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Short Positions (${analytics.shortTradesCount})",
                        color = Color(0xFF8B949E),
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${String.format(Locale.US, "%.1f%%", analytics.shortWinRatePercent)} Win Rate",
                        color = if (analytics.shortWinRatePercent >= 50.0) DesignVerifiedGreen else Color(0xFFC9D1D9),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // Trade Journal (Closed Trades List)
        Text(
            text = "Trade Journal (${closedTrades.size})",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )

        if (closedTrades.isEmpty()) {
            EmptyStateView(
                title = "No Closed Trades Yet",
                subtitle = "Take trades and manage positions to generate performance analytics and journal history."
            )
        } else {
            closedTrades.reversed().forEach { trade ->
                ClosedTradeJournalCard(trade = trade)
            }
        }
    }
}

@Composable
private fun TraderKpiCard(
    title: String,
    value: String,
    subtitle: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
        border = BorderStroke(1.dp, Color(0xFF21262D))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                color = Color(0xFF8B949E),
                fontSize = 11.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                color = valueColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = Color(0xFF7D8590),
                fontSize = 10.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ClosedTradeJournalCard(trade: ClosedTrade) {
    val isProfit = trade.realizedPnL >= 0
    val pnlColor = if (isProfit) DesignVerifiedGreen else DesignLossRed
    val isLong = trade.side == PositionSide.LONG

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
        border = BorderStroke(1.dp, Color(0xFF21262D))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isLong) DesignVerifiedGreen.copy(alpha = 0.18f) else DesignLossRed.copy(alpha = 0.18f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isLong) "LONG" else "SHORT",
                            color = if (isLong) DesignVerifiedGreen else DesignLossRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Text(
                        text = trade.symbol,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF21262D))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = trade.reason.label,
                            color = Color(0xFF8B949E),
                            fontSize = 9.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = (if (isProfit) "+" else "") + Formatters.formatCurrency(trade.realizedPnL),
                        color = pnlColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = String.format(Locale.US, "%s%.2f%%", if (isProfit) "+" else "", trade.roePercent),
                        color = pnlColor,
                        fontSize = 10.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Entry: ${Formatters.formatCurrency(trade.entryPrice)} → Exit: ${Formatters.formatCurrency(trade.exitPrice)}",
                    color = Color(0xFF8B949E),
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Size: ${trade.quantity}",
                    color = Color(0xFF8B949E),
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Partial Close BottomSheet
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PartialCloseBottomSheet(
    holding: PortfolioHoldingItem,
    onDismiss: () -> Unit,
    onConfirmClose: (Double) -> Unit
) {
    var fraction by remember { mutableStateOf(0.5) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161B22),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Partial Close • ${holding.symbol}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Close a percentage of your position to lock in profit or de-risk margin exposure.",
                color = Color(0xFF8B949E),
                fontSize = 13.sp
            )

            // Percentage Chips: 25%, 50%, 75%, 100%
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val chips = listOf(Pair(0.25, "25%"), Pair(0.50, "50%"), Pair(0.75, "75%"), Pair(1.0, "100%"))
                for (chip in chips) {
                    val f = chip.first
                    val label = chip.second
                    val isSelected = fraction == f
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFF238636) else Color(0xFF21262D))
                            .clickable { fraction = f }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            val closingUnits = holding.units * fraction
            val estPnL = (holding.rawPnL ?: 0.0) * fraction
            val pnlColor = if (estPnL >= 0) DesignVerifiedGreen else DesignLossRed

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D1117), RoundedCornerShape(8.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Closing Amount", color = Color(0xFF8B949E), fontSize = 12.sp)
                    Text("${String.format(Locale.US, "%.4f", closingUnits).trimEnd('0').trimEnd('.')} / ${String.format(Locale.US, "%.4f", holding.units).trimEnd('0').trimEnd('.')}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Estimated Realized PnL", color = Color(0xFF8B949E), fontSize = 12.sp)
                    Text((if (estPnL >= 0) "+" else "") + Formatters.formatCurrency(estPnL), color = pnlColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = { onConfirmClose(fraction) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238636))
            ) {
                Text(
                    text = "Confirm Close ${(fraction * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// -------------------------------------------------------------
// Edit TP / SL BottomSheet
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSlTpBottomSheet(
    holding: PortfolioHoldingItem,
    onDismiss: () -> Unit,
    onConfirm: (tp: Double?, sl: Double?) -> Unit
) {
    var tpText by remember { mutableStateOf(holding.takeProfitPrice?.let { String.format(Locale.US, "%.2f", it) } ?: "") }
    var slText by remember { mutableStateOf(holding.stopLossPrice?.let { String.format(Locale.US, "%.2f", it) } ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161B22),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Modify Position TP / SL • ${holding.symbol}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Current Mark Price: ${Formatters.formatCurrency(holding.currentPrice)} | Entry: ${Formatters.formatCurrency(holding.entryPrice)}",
                color = Color(0xFF8B949E),
                fontSize = 13.sp
            )

            // TP Input
            OutlinedTextField(
                value = tpText,
                onValueChange = { tpText = it },
                label = { Text("Take Profit Price ($)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DesignVerifiedGreen,
                    unfocusedBorderColor = Color(0xFF30363D),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            // SL Input
            OutlinedTextField(
                value = slText,
                onValueChange = { slText = it },
                label = { Text("Stop Loss Price ($)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DesignLossRed,
                    unfocusedBorderColor = Color(0xFF30363D),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onConfirm(null, null) },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF30363D))
                ) {
                    Text("Clear TP/SL", color = Color(0xFF8B949E), fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        val tpVal = tpText.toDoubleOrNull()
                        val slVal = slText.toDoubleOrNull()
                        onConfirm(tpVal, slVal)
                    },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Save TP/SL", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// -------------------------------------------------------------
// Closed / Order History Row
// -------------------------------------------------------------

@Composable
private fun PortfolioOrderRow(order: Order) {
    val statusColor = when (order.status) {
        OrderStatus.FILLED -> DesignVerifiedGreen
        OrderStatus.CANCELLED -> Color(0xFF8B949E)
        OrderStatus.REJECTED -> DesignLossRed
        OrderStatus.NEW, OrderStatus.ACKNOWLEDGED, OrderStatus.PARTIALLY_FILLED -> Color(0xFFD29922)
        OrderStatus.PENDING_CANCEL -> Color(0xFF8B949E)
    }
    val sideColor = if (order.side == com.trading.app.domain.model.OrderSide.BUY) DesignVerifiedGreen else DesignLossRed

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(sideColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = order.side.name.take(1),
                    color = sideColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = order.symbol,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = order.status.name,
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                Text(
                    text = "${order.filledQuantity}/${order.quantity} units @ ${order.avgFillPrice?.let { Formatters.formatPrice(it) } ?: "—"}",
                    color = Color(0xFF8B949E),
                    fontSize = 12.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = Formatters.formatCurrency(order.quantity * (order.avgFillPrice ?: 0.0)),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = Formatters.formatTimestamp(order.updatedAt, "HH:mm:ss"),
                color = Color(0xFF6E7681),
                fontSize = 11.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

// -------------------------------------------------------------
// Replies / Activity Feed & Transaction Ledger View
// -------------------------------------------------------------

@Composable
private fun RepliesActivityView(
    transactions: List<com.trading.app.domain.model.Transaction> = emptyList(),
    onOpenStatement: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Transaction & Trade Ledger",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(PrimaryBlue.copy(alpha = 0.2f))
                    .clickable(onClick = onOpenStatement)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text("Full Statement", color = PrimaryBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        transactions.take(8).forEach { tx ->
            val amtColor = if (tx.amount >= 0) DesignVerifiedGreen else DesignLossRed
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF12151B))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(tx.type.label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${if (tx.amount >= 0) "+" else ""}${Formatters.formatCurrency(tx.amount)}",
                            color = amtColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(tx.description, color = Color(0xFF8B949E), fontSize = 13.sp)
                    Text(
                        text = "Ref: ${tx.reference ?: tx.id} • Bal: ${Formatters.formatCurrency(tx.balanceAfter)}",
                        color = Color(0xFF6E7681),
                        fontSize = 11.sp
                    )
                }
            }
        }

        listOf(
            Triple("Order Executed", "Filled 5.01B PENGU position at $0.008019.", "10m ago"),
            Triple("Price Alert Triggered", "Lil Pengu reached 24h high of $165.98 (+1019.7%).", "1h ago"),
            Triple("Margin Check Passed", "Account margin level safe at 1,420%. Free margin: $70.18.", "3h ago")
        ).forEach { (title, body, time) ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF12151B))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(time, color = Color(0xFF6E7681), fontSize = 11.sp)
                    }
                    Text(body, color = Color(0xFF8B949E), fontSize = 13.sp)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// More Tab: Margin Specs, Statements & Security Summary
// -------------------------------------------------------------

@Composable
private fun MarginSpecsSummaryView(
    portfolio: com.trading.app.domain.model.Portfolio?,
    onOpenStatements: () -> Unit = {},
    onResetDemo: () -> Unit = {},
    onLockAccount: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onOpenStatements,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Statements",
                    color = Color.Black,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Button(
                onClick = onResetDemo,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21262D)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Reset Demo",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Button(
                onClick = onLockAccount,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21262D)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "PIN Lock",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        listOf(
            "Total Net Worth (Equity)" to (portfolio?.let { Formatters.formatCurrency(it.totalValue) } ?: "$40,179,374.97"),
            "Account Balance" to (portfolio?.let { Formatters.formatCurrency(it.accountBalance) } ?: "$100,000.00"),
            "Available Cash Balance" to (portfolio?.let { Formatters.formatCurrency(it.cashBalance) } ?: "$70.18"),
            "Used Margin" to (portfolio?.let { Formatters.formatCurrency(it.totalUsedMargin) } ?: "$2,820.00"),
            "Free Margin" to (portfolio?.let { Formatters.formatCurrency(it.freeMargin) } ?: "$70.18"),
            "Margin Level" to (portfolio?.let { if (it.totalUsedMargin > 0) "${String.format("%.1f", it.marginLevel)}%" else "∞ (No Margin Used)" } ?: "1,424.8%"),
            "Realized P&L" to (portfolio?.let { Formatters.formatCurrency(it.realizedPnL) } ?: "$0.00"),
            "Margin Stop-Out Threshold" to "50.0% Margin Level",
            "Max Leverage Available" to "Up to 50x (Crypto) / 30x (FX)"
        ).forEach { (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF101318))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    color = Color(0xFF8B949E),
                    fontSize = 14.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = value,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Packs Tab: Curated Asset Bundles
// -------------------------------------------------------------

@Composable
private fun PacksView(onSelectSymbol: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        listOf(
            Triple("Crypto Majors Pack", "BTC, ETH, SOL", "3 Assets • High Momentum"),
            Triple("Tech Giants Pack", "AAPL, MSFT, NVDA, GOOGL", "4 Assets • Large Cap"),
            Triple("Forex Yield Pack", "EUR/USD, GBP/USD, USD/JPY", "3 Currency Pairs • Deep Liquidity"),
            Triple("Commodities Basket", "Gold, Silver, Brent Oil", "3 Assets • Inflation Hedge")
        ).forEach { (title, symbols, desc) ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF12161E))
                    .clickable { onSelectSymbol(symbols.split(",").first().trim()) }
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(symbols, color = PrimaryBlue, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text(desc, color = Color(0xFF6E7681), fontSize = 12.sp)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Quick Deposit BottomSheet
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepositFundsBottomSheet(
    onDismiss: () -> Unit,
    onDeposit: (Double) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF101318),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF30363D))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Deposit Virtual Trading Funds",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Text(
                text = "Select an amount to fund your buying power and margin capacity instantly:",
                color = Color(0xFF8B949E),
                fontSize = 14.sp
            )

            listOf(1_000.0, 10_000.0, 50_000.0, 100_000.0).forEach { amount ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E2127))
                        .clickable { onDeposit(amount) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Deposit ${Formatters.formatCurrency(amount)}",
                        color = PrimaryBlue,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// -------------------------------------------------------------
// Quick Withdraw BottomSheet
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawFundsBottomSheet(
    onDismiss: () -> Unit,
    onWithdraw: (Double) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF101318),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF30363D))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Withdraw Funds",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Text(
                text = "Select amount to withdraw from your available free cash:",
                color = Color(0xFF8B949E),
                fontSize = 14.sp
            )

            listOf(500.0, 5_000.0, 25_000.0).forEach { amount ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E2127))
                        .clickable { onWithdraw(amount) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Withdraw ${Formatters.formatCurrency(amount)}",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// -------------------------------------------------------------
// Account Stats BottomSheet (`View stats`)
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountStatsBottomSheet(
    portfolio: com.trading.app.domain.model.Portfolio?,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF101318),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF30363D))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Account Statistics",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            listOf(
                "Total Net Worth" to (portfolio?.let { Formatters.formatCurrency(it.totalValue) } ?: "$40,179,374.97"),
                "Cash Balance" to (portfolio?.let { Formatters.formatCurrency(it.cashBalance) } ?: "$70.18"),
                "Buying Power" to (portfolio?.let { Formatters.formatCurrency(it.buyingPower) } ?: "$70.18"),
                "Used Margin" to (portfolio?.let { Formatters.formatCurrency(it.totalUsedMargin) } ?: "$2,820.00"),
                "Free Margin" to (portfolio?.let { Formatters.formatCurrency(it.freeMargin) } ?: "$70.18"),
                "Margin Level" to (portfolio?.let { "${String.format("%.1f", it.marginLevel)}%" } ?: "1,424.8%"),
                "Total Unrealized P&L" to (portfolio?.let { Formatters.formatCurrency(it.totalUnrealizedPnL) } ?: "+$151.16")
            ).forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, color = Color(0xFF8B949E), fontSize = 14.sp)
                    Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// -------------------------------------------------------------
// Empty State
// -------------------------------------------------------------

@Composable
private fun EmptyStateView(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = Color(0xFF8B949E), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(subtitle, color = Color(0xFF6E7681), fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun AccountControlStrip(
    accountType: com.trading.app.domain.model.AccountType,
    dataSourceMode: com.trading.app.data.DataSourceMode,
    onToggleAccountType: () -> Unit,
    onResetDemo: () -> Unit,
    onToggleFeed: () -> Unit,
    onOpenStatements: () -> Unit,
    onLockAccount: () -> Unit
) {
    val isDemo = accountType == com.trading.app.domain.model.AccountType.DEMO
    val isLiveFeed = dataSourceMode != com.trading.app.data.DataSourceMode.SIMULATED
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Demo / Live Account Toggle Chip
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isDemo) Color(0xFF1F6FEB).copy(alpha = 0.22f) else DesignVerifiedGreen.copy(alpha = 0.22f))
                .clickable(onClick = onToggleAccountType)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (isDemo) "DEMO $100K" else "PAPER (LIVE QUOTES)",
                color = if (isDemo) PrimaryBlue else DesignVerifiedGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }

        // Reset Demo Button
        if (isDemo) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF161B22))
                    .clickable(onClick = onResetDemo)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Reset",
                    color = Color(0xFFC9D1D9),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // Live Stream vs Sim Toggle
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF161B22))
                .clickable(onClick = onToggleFeed)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (isLiveFeed) "Live Feed" else "Sim Feed",
                color = if (isLiveFeed) DesignVerifiedGreen else Color(0xFFD29922),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false
            )
        }

        // Statements Button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF161B22))
                .clickable(onClick = onOpenStatements)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Statement",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false
            )
        }

        // Security Lock Button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF161B22))
                .clickable(onClick = onLockAccount)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Lock",
                color = Color(0xFF8B949E),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun PortfolioClosedTradeRow(trade: com.trading.app.domain.model.ClosedTrade) {
    val pnlColor = if (trade.isProfit) DesignVerifiedGreen else DesignLossRed
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = trade.symbol,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(pnlColor.copy(alpha = 0.15f))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = trade.reason.label.uppercase(),
                        color = pnlColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = "${trade.side.name} ${trade.quantity} @ ${Formatters.formatPrice(trade.entryPrice)} → ${Formatters.formatPrice(trade.exitPrice)}",
                color = Color(0xFF8B949E),
                fontSize = 12.sp
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${if (trade.isProfit) "+" else ""}${Formatters.formatCurrency(trade.realizedPnL)}",
                color = pnlColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Realized P&L • ${Formatters.formatTimestamp(trade.closedAt, "HH:mm:ss")}",
                color = Color(0xFF6E7681),
                fontSize = 11.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountStatementBottomSheet(
    statement: com.trading.app.domain.model.AccountStatement?,
    portfolio: com.trading.app.domain.model.Portfolio?,
    transactions: List<com.trading.app.domain.model.Transaction>,
    closedTrades: List<com.trading.app.domain.model.ClosedTrade>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF101318)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Account & Transaction Statement",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${statement?.account?.id ?: "DEMO-884920"} • ${statement?.account?.ownerName ?: "Demo Trader"}",
                        color = Color(0xFF8B949E),
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            listOf(
                "Opening Balance" to Formatters.formatCurrency(statement?.openingBalance ?: 100_000.0),
                "Total Deposits" to Formatters.formatCurrency(statement?.deposits ?: 100_000.0),
                "Total Withdrawals" to Formatters.formatCurrency(statement?.withdrawals ?: 0.0),
                "Realized Trade P&L" to Formatters.formatCurrency(statement?.realizedPnL ?: 0.0),
                "Floating Unrealized P&L" to Formatters.formatCurrency(portfolio?.totalUnrealizedPnL ?: 0.0),
                "Used Margin" to Formatters.formatCurrency(portfolio?.totalUsedMargin ?: 0.0),
                "Free Margin" to Formatters.formatCurrency(portfolio?.freeMargin ?: 100_000.0),
                "Closing Equity" to Formatters.formatCurrency(statement?.equity ?: 100_000.0),
                "Closed Trades Count" to "${closedTrades.size} trades",
                "Ledger Entries" to "${transactions.size} transactions"
            ).forEach { (label, valStr) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, color = Color(0xFF8B949E), fontSize = 13.sp)
                    Text(valStr, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Button(
                onClick = {
                    val summary = buildString {
                        appendLine("=== TRADING ACCOUNT STATEMENT ===")
                        appendLine("Account: ${statement?.account?.id ?: "DEMO-884920"} (${statement?.account?.type ?: "DEMO"})")
                        appendLine("Opening Balance: ${Formatters.formatCurrency(statement?.openingBalance ?: 100_000.0)}")
                        appendLine("Deposits: ${Formatters.formatCurrency(statement?.deposits ?: 100_000.0)}")
                        appendLine("Withdrawals: ${Formatters.formatCurrency(statement?.withdrawals ?: 0.0)}")
                        appendLine("Realized P&L: ${Formatters.formatCurrency(statement?.realizedPnL ?: 0.0)}")
                        appendLine("Closing Equity: ${Formatters.formatCurrency(statement?.equity ?: 100_000.0)}")
                        appendLine("Transactions (${transactions.size}):")
                        transactions.take(10).forEach { tx ->
                            appendLine(" - [${tx.type.label}] ${tx.description}: ${Formatters.formatCurrency(tx.amount)}")
                        }
                    }
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Account Statement", summary))
                    Toast.makeText(context, "Statement copied to clipboard for export", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Export / Copy Statement", color = Color.Black, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun AccountSecurityLockOverlay(
    pinError: String?,
    onUnlockWithPin: (String) -> Unit,
    onBiometricUnlock: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF50A0D12))
            .clickable(enabled = true, onClick = {}),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF161B22))
                .border(1.dp, Color(0xFF30363D), RoundedCornerShape(20.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Badge distinguishing this as a demo screen lock, not banking security
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF388BFD).copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "DEMO TERMINAL LOCKOUT",
                    color = PrimaryBlue,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }

            Text(
                text = "Terminal Demo Lock",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Simulated workstation screen lock for educational and paper trading purposes (not production banking credentials).\nDefault PIN: 1234 or 0000.",
                color = Color(0xFF8B949E),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 17.sp
            )

            OutlinedTextField(
                value = enteredPin,
                onValueChange = { if (it.length <= 4) enteredPin = it },
                label = { Text("4-Digit Demo PIN") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (pinError != null) {
                Text(pinError, color = DesignLossRed, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onUnlockWithPin(enteredPin) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Text("Unlock PIN", color = Color.Black, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
                Button(
                    onClick = onBiometricUnlock,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = DesignVerifiedGreen),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Text("Demo Biometric", color = Color.Black, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}
