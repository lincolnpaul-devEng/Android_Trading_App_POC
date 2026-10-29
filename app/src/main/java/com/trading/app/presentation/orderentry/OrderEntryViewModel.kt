package com.trading.app.presentation.orderentry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trading.app.domain.model.*
import com.trading.app.domain.repository.MarketDataRepository
import com.trading.app.domain.repository.OrderRepository
import com.trading.app.domain.repository.PortfolioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class OrderEntryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val marketDataRepository: MarketDataRepository,
    private val orderRepository: OrderRepository,
    private val portfolioRepository: PortfolioRepository
) : ViewModel() {

    private val symbol: String = savedStateHandle.get<String>("symbol")
        ?.let { runCatching { java.net.URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) } ?: "AAPL"

    private val _uiState = MutableStateFlow(OrderEntryUiState(symbol = symbol))
    val uiState: StateFlow<OrderEntryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            marketDataRepository.getLiveQuote(symbol).collect { quote ->
                _uiState.update {
                    it.copy(
                        currentPrice = quote.lastPrice,
                        bidPrice = quote.bid,
                        askPrice = quote.ask,
                        companyName = quote.companyName,
                        specs = quote.specs
                    )
                }
            }
        }
        viewModelScope.launch {
            portfolioRepository.getPortfolio().collect { portfolio ->
                val pos = portfolio.positions.find { it.symbol == symbol }
                _uiState.update {
                    it.copy(buyingPower = portfolio.buyingPower, currentPosition = pos?.quantity ?: 0)
                }
            }
        }
    }

    fun setSide(side: OrderSide) = _uiState.update { it.copy(side = side) }
    fun setType(type: OrderType) = _uiState.update { it.copy(type = type) }
    fun setTimeInForce(tif: TimeInForce) = _uiState.update { it.copy(timeInForce = tif) }
    fun setQuantity(qty: String) = _uiState.update { it.copy(quantity = qty) }
    fun setLimitPrice(price: String) = _uiState.update { it.copy(limitPrice = price) }
    fun setStopPrice(price: String) = _uiState.update { it.copy(stopPrice = price) }
    fun setLeverage(leverage: Int) = _uiState.update { it.copy(leverage = leverage) }

    fun openAutoCloseSheet() = _uiState.update { it.copy(showAutoCloseSheet = true) }
    fun dismissAutoCloseSheet() = _uiState.update { it.copy(showAutoCloseSheet = false) }

    fun saveAutoClose(mode: com.trading.app.presentation.components.AutoCloseMode, tp: String, sl: String) {
        _uiState.update {
            it.copy(
                autoCloseMode = mode,
                takeProfit = tp,
                stopLoss = sl,
                showAutoCloseSheet = false
            )
        }
    }

    fun clearAutoClose() {
        _uiState.update {
            it.copy(takeProfit = "", stopLoss = "")
        }
    }

    fun setQuantityPercent(percent: Double) {
        val state = _uiState.value
        val effectiveBuyingPower = state.buyingPower * state.leverage
        val qty = if (state.side == OrderSide.BUY || state.currentPosition == 0) {
            if (state.effectivePrice > 0) ((effectiveBuyingPower * percent) / state.effectivePrice).toInt() else 0
        } else {
            (state.currentPosition * percent).toInt()
        }
        _uiState.update { it.copy(quantity = qty.toString()) }
    }

    fun setQuantityByRiskPercent(riskPct: Double) {
        val state = _uiState.value
        val slPrice = state.calculatedSlPrice
        if (slPrice == null || slPrice <= 0.0) {
            _uiState.update { it.copy(error = "Configure a Stop Loss first to calculate risk-based sizing") }
            return
        }
        val priceDiff = kotlin.math.abs(state.effectivePrice - slPrice)
        if (priceDiff <= 0.0) return

        val riskDollars = state.buyingPower * (riskPct / 100.0)
        val targetUnits = (riskDollars / (priceDiff * state.multiplier)).toInt().coerceAtLeast(1)
        _uiState.update { it.copy(quantity = targetUnits.toString(), error = null) }
    }

    fun showConfirmation() {
        val state = _uiState.value
        // Validation
        val qty = state.quantity.toIntOrNull()
        if (qty == null || qty <= 0) {
            _uiState.update { it.copy(error = "Enter a valid quantity") }
            return
        }
        val requiredCapital = state.requiredMargin + state.estimatedFee
        if (state.side == OrderSide.BUY && requiredCapital > state.buyingPower) {
            _uiState.update { it.copy(error = "Insufficient buying power") }
            return
        }
        if (state.side == OrderSide.SELL) {
            if (state.leverage == 1 && state.currentPosition > 0 && qty > state.currentPosition) {
                _uiState.update { it.copy(error = "Insufficient shares (you own ${state.currentPosition})") }
                return
            }
            if (state.currentPosition == 0 && requiredCapital > state.buyingPower) {
                _uiState.update { it.copy(error = "Insufficient margin to open short") }
                return
            }
        }
        if (state.type == OrderType.LIMIT || state.type == OrderType.STOP_LIMIT) {
            if (state.limitPrice.toDoubleOrNull() == null) {
                _uiState.update { it.copy(error = "Enter a valid limit price") }
                return
            }
        }
        _uiState.update { it.copy(showConfirmation = true, error = null) }
    }

    fun dismissConfirmation() = _uiState.update { it.copy(showConfirmation = false) }
    fun clearResult() = _uiState.update { it.copy(orderResult = null, error = null) }

    fun submitOrder() {
        val state = _uiState.value
        val positionSide = if (state.side == OrderSide.SELL && state.currentPosition == 0) {
            PositionSide.SHORT
        } else {
            PositionSide.LONG
        }
        val order = Order(
            id = UUID.randomUUID().toString(),
            symbol = state.symbol,
            side = state.side,
            type = state.type,
            timeInForce = state.timeInForce,
            quantity = state.quantity.toIntOrNull() ?: 0,
            price = state.limitPrice.toDoubleOrNull(),
            stopPrice = state.stopPrice.toDoubleOrNull(),
            leverage = state.leverage,
            positionSide = positionSide,
            takeProfitPrice = state.calculatedTpPrice,
            stopLossPrice = state.calculatedSlPrice,
            specs = state.specs
        )
        viewModelScope.launch {
            val result = orderRepository.placeOrder(order)
            result.fold(
                onSuccess = { filled ->
                    val autoCloseInfo = if (filled.hasAutoClose) {
                        " [TP: ${filled.takeProfitPrice?.let { p -> String.format("%.2f", p) } ?: "—"} / SL: ${filled.stopLossPrice?.let { p -> String.format("%.2f", p) } ?: "—"}]"
                    } else ""
                    _uiState.update {
                        it.copy(
                            showConfirmation = false,
                            orderResult = "${filled.side} ${filled.filledQuantity} ${filled.symbol} (${filled.leverage}x) @ ${filled.avgFillPrice?.let { p -> String.format("%.2f", p) } ?: "MKT"} — ${filled.status}$autoCloseInfo",
                            quantity = "",
                            limitPrice = "",
                            stopPrice = "",
                            takeProfit = "",
                            stopLoss = ""
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(showConfirmation = false, error = e.message) }
                }
            )
        }
    }
}
