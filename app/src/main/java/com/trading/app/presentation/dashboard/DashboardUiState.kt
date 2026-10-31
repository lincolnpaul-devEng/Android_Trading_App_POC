package com.trading.app.presentation.dashboard

import com.trading.app.domain.model.AssetClass
import com.trading.app.domain.model.MarketIndex
import com.trading.app.domain.model.Portfolio
import com.trading.app.domain.model.Position
import com.trading.app.domain.model.SectorPerformance
import com.trading.app.domain.model.StockQuote

data class DashboardUiState(
    val indices: List<MarketIndex> = emptyList(),
    val topMovers: List<StockQuote> = emptyList(),
    val sectors: List<SectorPerformance> = emptyList(),
    val selectedAssetClass: AssetClass? = null,
    val multiAssetQuotes: List<StockQuote> = emptyList(),
    val selectedSpecsQuote: StockQuote? = null,
    val portfolio: Portfolio? = null,
    val positions: List<Position> = emptyList(),
    val totalEquity: Double = 100_000.0,
    val availableMargin: Double = 100_000.0,
    val unrealizedPnL: Double = 0.0,
    val dayPnL: Double = 0.0,
    val todayReturnPct: Double = 0.0,
    val totalCostBasis: Double = 0.0,
    val investedAmount: Double = 0.0,
    val totalProfitAmount: Double = 0.0,
    val selectedViewMode: DashboardViewMode = DashboardViewMode.OVERVIEW,
    val showDepositSheet: Boolean = false,
    val showWithdrawSheet: Boolean = false,
    val isLoading: Boolean = false
)

enum class DashboardViewMode {
    OVERVIEW,
    ANALYTICS_WHEEL
}
