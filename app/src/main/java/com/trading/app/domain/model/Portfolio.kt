package com.trading.app.domain.model

data class Position(
    val symbol: String,
    val companyName: String,
    val quantity: Int,
    val averageCost: Double,
    val currentPrice: Double,
    val side: PositionSide = PositionSide.LONG,
    val leverage: Int = 1,
    val takeProfitPrice: Double? = null,
    val stopLossPrice: Double? = null,
    val specs: InstrumentSpecs = InstrumentSpecs(),
    val id: String = symbol,
    val openedAt: Long = System.currentTimeMillis(),
    val fxRate: Double = 1.0
) {
    val notionalValue: Double get() = quantity * currentPrice * specs.contractMultiplier * fxRate
    val requiredMargin: Double get() = (quantity * averageCost * specs.contractMultiplier * fxRate) / leverage
    val marketValue: Double get() = notionalValue
    val costBasis: Double get() = quantity * averageCost * specs.contractMultiplier * fxRate
    val unrealizedPnL: Double get() = when (side) {
        PositionSide.LONG -> (currentPrice - averageCost) * quantity * specs.contractMultiplier * fxRate
        PositionSide.SHORT -> (averageCost - currentPrice) * quantity * specs.contractMultiplier * fxRate
    }
    val unrealizedPnLPercent: Double get() = if (requiredMargin != 0.0) (unrealizedPnL / requiredMargin) * 100 else 0.0
    val isProfit: Boolean get() = unrealizedPnL >= 0

    val liquidationPrice: Double get() = when (side) {
        PositionSide.LONG -> averageCost * (1.0 - (1.0 / leverage) * 0.9)
        PositionSide.SHORT -> averageCost * (1.0 + (1.0 / leverage) * 0.9)
    }
}

data class Portfolio(
    val positions: List<Position>,
    val cashBalance: Double,
    val dayPnL: Double,
    val realizedPnL: Double = 0.0
) {
    val totalMarketValue: Double get() = positions.sumOf { it.marketValue }
    val totalCostBasis: Double get() = positions.sumOf { it.costBasis }
    val totalUnrealizedPnL: Double get() = positions.sumOf { it.unrealizedPnL }
    val totalUsedMargin: Double get() = positions.sumOf { it.requiredMargin }
    /** Settled account balance (available cash + margin reserved in open positions). */
    val accountBalance: Double get() = cashBalance + totalUsedMargin
    /** Real-time account equity = settled account balance + floating unrealized P&L. */
    val equity: Double get() = accountBalance + totalUnrealizedPnL
    /** Total net worth (equity). At 1x leverage this equals cashBalance + totalMarketValue. */
    val totalValue: Double get() = equity
    /** Free margin = equity − used margin (floored at 0). */
    val freeMargin: Double get() = maxOf(0.0, equity - totalUsedMargin)
    val buyingPower: Double get() = cashBalance
    /** Margin level % = (equity / used margin) × 100. */
    val marginLevel: Double get() = if (totalUsedMargin > 0) (equity / totalUsedMargin) * 100 else 0.0
    /** True when margin level drops below 100% warning threshold. */
    val isMarginCallWarning: Boolean get() = totalUsedMargin > 0 && marginLevel < 100.0
}
