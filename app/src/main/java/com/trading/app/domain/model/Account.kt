package com.trading.app.domain.model

import kotlin.math.abs

enum class AccountType { DEMO, LIVE }

data class TradingAccount(
    val id: String,
    val ownerName: String,
    val type: AccountType,
    val currency: String,
    val startingBalance: Double,
    val createdAt: Long
)

enum class CloseReason(val label: String) {
    MANUAL("Closed manually"),
    STOP_LOSS("Stop loss"),
    TAKE_PROFIT("Take profit"),
    STOP_OUT("Margin stop-out")
}

data class ClosedTrade(
    val ticket: String,
    val symbol: String,
    val name: String,
    val side: PositionSide,
    val quantity: Int,
    val entryPrice: Double,
    val exitPrice: Double,
    val leverage: Int,
    val openedAt: Long,
    val closedAt: Long,
    val realizedPnL: Double,
    val reason: CloseReason,
    val assetClass: AssetClass = AssetClass.EQUITY
) {
    val isProfit: Boolean get() = realizedPnL >= 0
    val durationMillis: Long get() = maxOf(1_000L, closedAt - openedAt)
    val roePercent: Double get() {
        val margin = if (leverage > 0) (quantity * entryPrice) / leverage else (quantity * entryPrice)
        return if (margin > 0) (realizedPnL / margin) * 100.0 else 0.0
    }
}

enum class TransactionType(val label: String) {
    DEPOSIT("Deposit"),
    WITHDRAWAL("Withdrawal"),
    REALIZED_PNL("Trade P&L"),
    RESET("Account reset")
}

data class Transaction(
    val id: String,
    val type: TransactionType,
    val amount: Double,
    val balanceAfter: Double,
    val description: String,
    val timestamp: Long,
    val reference: String? = null
)

data class EquityPoint(val timestamp: Long, val equity: Double)

enum class AlertCondition(val label: String) {
    ABOVE("Crosses Above"),
    BELOW("Crosses Below")
}

data class PriceAlert(
    val id: String,
    val symbol: String,
    val targetPrice: Double,
    val condition: AlertCondition,
    val isTriggered: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val triggeredAt: Long? = null
)

/**
 * Institutional trader self-review & performance analytics modeled on Binance P&L Analysis
 * and FxPro cTrader Analyze.
 */
data class TraderPerformanceAnalytics(
    val totalTrades: Int = 0,
    val winningTrades: Int = 0,
    val losingTrades: Int = 0,
    val winRatePercent: Double = 0.0,
    val grossProfit: Double = 0.0,
    val grossLoss: Double = 0.0,
    val netRealizedPnL: Double = 0.0,
    val profitFactor: Double = 0.0,
    val averageWin: Double = 0.0,
    val averageLoss: Double = 0.0,
    val payoffRatio: Double = 0.0,
    val expectancyPerTrade: Double = 0.0,
    val largestWin: Double = 0.0,
    val largestLoss: Double = 0.0,
    val maxDrawdownPercent: Double = 0.0,
    val maxDrawdownAmount: Double = 0.0,
    val currentStreak: Int = 0, // Positive = win streak, Negative = loss streak
    val bestWinStreak: Int = 0,
    val worstLossStreak: Int = 0,
    val longTradesCount: Int = 0,
    val longWinRatePercent: Double = 0.0,
    val shortTradesCount: Int = 0,
    val shortWinRatePercent: Double = 0.0,
    val pnlByAssetClass: Map<AssetClass, Double> = emptyMap(),
    val traderGrade: String = "Developing"
) {
    companion object {
        fun compute(
            closedTrades: List<ClosedTrade>,
            equityHistory: List<EquityPoint>,
            startingBalance: Double = 100_000.0
        ): TraderPerformanceAnalytics {
            if (closedTrades.isEmpty()) {
                return TraderPerformanceAnalytics(traderGrade = "Unranked (0 Trades)")
            }

            // Sort chronological (oldest -> newest) for streak & drawdown calculation
            val chronological = closedTrades.sortedBy { it.closedAt }
            val total = chronological.size
            val wins = chronological.filter { it.realizedPnL >= 0.0 }
            val losses = chronological.filter { it.realizedPnL < 0.0 }

            val winCount = wins.size
            val lossCount = losses.size
            val winRate = (winCount.toDouble() / total) * 100.0

            val grossProfit = wins.sumOf { it.realizedPnL }
            val grossLoss = abs(losses.sumOf { it.realizedPnL })
            val netPnL = grossProfit - grossLoss

            val profitFactor = when {
                grossLoss > 0.0 -> grossProfit / grossLoss
                grossProfit > 0.0 -> 99.9
                else -> 0.0
            }

            val avgWin = if (winCount > 0) grossProfit / winCount else 0.0
            val avgLoss = if (lossCount > 0) grossLoss / lossCount else 0.0
            val payoff = if (avgLoss > 0.0) avgWin / avgLoss else if (avgWin > 0.0) 9.9 else 0.0
            val expectancy = netPnL / total

            val largestWin = wins.maxOfOrNull { it.realizedPnL } ?: 0.0
            val largestLoss = losses.minOfOrNull { it.realizedPnL } ?: 0.0

            // Streaks
            var curStreak = 0
            var bestWin = 0
            var worstLoss = 0
            for (trade in chronological) {
                if (trade.realizedPnL >= 0.0) {
                    curStreak = if (curStreak > 0) curStreak + 1 else 1
                    if (curStreak > bestWin) bestWin = curStreak
                } else {
                    curStreak = if (curStreak < 0) curStreak - 1 else -1
                    if (abs(curStreak) > worstLoss) worstLoss = abs(curStreak)
                }
            }

            // Max Drawdown from equity curve or cumulative trade equity
            val curve = if (equityHistory.size >= 2) {
                equityHistory.map { it.equity }
            } else {
                var eq = startingBalance
                buildList {
                    add(eq)
                    for (t in chronological) {
                        eq += t.realizedPnL
                        add(eq)
                    }
                }
            }
            var peak = curve.firstOrNull() ?: startingBalance
            var maxDdAmt = 0.0
            var maxDdPct = 0.0
            for (eq in curve) {
                if (eq > peak) peak = eq
                val ddAmt = peak - eq
                val ddPct = if (peak > 0.0) (ddAmt / peak) * 100.0 else 0.0
                if (ddAmt > maxDdAmt) maxDdAmt = ddAmt
                if (ddPct > maxDdPct) maxDdPct = ddPct
            }

            // Long vs Short performance
            val longs = chronological.filter { it.side == PositionSide.LONG }
            val shorts = chronological.filter { it.side == PositionSide.SHORT }
            val longWinRate = if (longs.isNotEmpty()) (longs.count { it.isProfit }.toDouble() / longs.size) * 100.0 else 0.0
            val shortWinRate = if (shorts.isNotEmpty()) (shorts.count { it.isProfit }.toDouble() / shorts.size) * 100.0 else 0.0

            val byAsset = chronological.groupBy { it.assetClass }
                .mapValues { (_, list) -> list.sumOf { it.realizedPnL } }

            val grade = when {
                profitFactor >= 2.0 && winRate >= 55.0 && maxDdPct < 10.0 -> "Institutional Pro (A+)"
                profitFactor >= 1.4 && netPnL > 0.0 -> "Profitable Disciplined (A)"
                profitFactor >= 1.0 -> "Breakeven / Developing (B)"
                else -> "High Risk / Drawdown (C)"
            }

            return TraderPerformanceAnalytics(
                totalTrades = total,
                winningTrades = winCount,
                losingTrades = lossCount,
                winRatePercent = winRate,
                grossProfit = grossProfit,
                grossLoss = grossLoss,
                netRealizedPnL = netPnL,
                profitFactor = profitFactor,
                averageWin = avgWin,
                averageLoss = avgLoss,
                payoffRatio = payoff,
                expectancyPerTrade = expectancy,
                largestWin = largestWin,
                largestLoss = largestLoss,
                maxDrawdownPercent = maxDdPct,
                maxDrawdownAmount = maxDdAmt,
                currentStreak = curStreak,
                bestWinStreak = bestWin,
                worstLossStreak = worstLoss,
                longTradesCount = longs.size,
                longWinRatePercent = longWinRate,
                shortTradesCount = shorts.size,
                shortWinRatePercent = shortWinRate,
                pnlByAssetClass = byAsset,
                traderGrade = grade
            )
        }
    }
}

/** Everything the UI needs to render the account, in one consistent snapshot. */
data class AccountSnapshot(
    val account: TradingAccount,
    val portfolio: Portfolio,
    val pendingOrders: List<Order>,
    val orderHistory: List<Order>,
    val closedTrades: List<ClosedTrade>,
    val transactions: List<Transaction>,
    val equityHistory: List<EquityPoint>,
    val marginCall: Boolean
) {
    val realizedPnL: Double get() = closedTrades.sumOf { it.realizedPnL }
}

/** Account statement over a period. */
data class AccountStatement(
    val account: TradingAccount,
    val from: Long,
    val to: Long,
    val openingBalance: Double,
    val deposits: Double,
    val withdrawals: Double,
    val realizedPnL: Double,
    val closingBalance: Double,
    val equity: Double,
    val transactions: List<Transaction>,
    val closedTrades: List<ClosedTrade>
)
