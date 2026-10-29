package com.trading.app.presentation.stockdetail

import com.trading.app.domain.model.Candlestick
import com.trading.app.domain.model.OrderBook
import com.trading.app.domain.model.Position
import com.trading.app.domain.model.StockQuote

data class StockDetailUiState(
    val quote: StockQuote? = null,
    val candles: List<Candlestick> = emptyList(),
    val orderBook: OrderBook? = null,
    val openPosition: Position? = null,
    val selectedInterval: String = "1D",
    val showEma20: Boolean = false,
    val showEma50: Boolean = false,
    val showBollingerBands: Boolean = false,
    val isLoading: Boolean = true
)
