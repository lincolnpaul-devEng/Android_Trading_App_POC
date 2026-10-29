package com.trading.app.presentation.dashboard

import com.trading.app.domain.model.AssetClass
import com.trading.app.domain.model.MarketIndex
import com.trading.app.domain.model.SectorPerformance
import com.trading.app.domain.model.StockQuote

data class DashboardUiState(
    val indices: List<MarketIndex> = emptyList(),
    val topMovers: List<StockQuote> = emptyList(),
    val sectors: List<SectorPerformance> = emptyList(),
    val selectedAssetClass: AssetClass? = null,
    val multiAssetQuotes: List<StockQuote> = emptyList(),
    val selectedSpecsQuote: StockQuote? = null,
    val totalEquity: Double = 100_000.0,
    val availableMargin: Double = 100_000.0,
    val unrealizedPnL: Double = 0.0,
    val isLoading: Boolean = true
)
