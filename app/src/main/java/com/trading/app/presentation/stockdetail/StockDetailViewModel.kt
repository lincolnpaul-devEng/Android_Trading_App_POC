package com.trading.app.presentation.stockdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trading.app.domain.model.Order
import com.trading.app.domain.model.OrderSide
import com.trading.app.domain.model.OrderType
import com.trading.app.domain.model.TimeInForce
import com.trading.app.domain.repository.MarketDataRepository
import com.trading.app.domain.repository.OrderRepository
import com.trading.app.domain.repository.PortfolioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class StockDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val marketDataRepository: MarketDataRepository,
    private val portfolioRepository: PortfolioRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {

    val symbol: String = savedStateHandle.get<String>("symbol")
        ?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) } ?: "AAPL"

    private val _uiState = MutableStateFlow(StockDetailUiState())
    val uiState: StateFlow<StockDetailUiState> = _uiState.asStateFlow()

    private var candleJob: Job? = null

    init {
        loadQuote()
        loadCandles("1D")
        loadOrderBook()
        loadPosition()
    }

    private fun loadQuote() {
        viewModelScope.launch {
            marketDataRepository.getLiveQuote(symbol).collect { quote ->
                _uiState.update { it.copy(quote = quote, isLoading = false) }
            }
        }
    }

    private fun loadPosition() {
        viewModelScope.launch {
            portfolioRepository.getPosition(symbol).collect { pos ->
                _uiState.update { it.copy(openPosition = pos) }
            }
        }
    }

    fun closeOpenPosition() {
        viewModelScope.launch {
            orderRepository.closePosition(symbol)
        }
    }

    fun selectInterval(interval: String) {
        _uiState.update { it.copy(selectedInterval = interval) }
        loadCandles(interval)
    }

    private fun loadCandles(interval: String) {
        candleJob?.cancel()
        candleJob = viewModelScope.launch {
            marketDataRepository.getCandlestickData(symbol, interval).collect { candles ->
                _uiState.update { it.copy(candles = candles) }
            }
        }
    }

    private fun loadOrderBook() {
        viewModelScope.launch {
            marketDataRepository.getOrderBook(symbol).collect { book ->
                _uiState.update { it.copy(orderBook = book) }
            }
        }
    }

    fun toggleEma20() {
        _uiState.update { it.copy(showEma20 = !it.showEma20) }
    }

    fun toggleEma50() {
        _uiState.update { it.copy(showEma50 = !it.showEma50) }
    }

    fun toggleBollingerBands() {
        _uiState.update { it.copy(showBollingerBands = !it.showBollingerBands) }
    }

    fun quickMarketOrder(isBuy: Boolean, quantity: Int = 1) {
        viewModelScope.launch {
            val q = _uiState.value.quote
            val fillPrice = if (isBuy) (q?.ask ?: q?.lastPrice ?: 0.0) else (q?.bid ?: q?.lastPrice ?: 0.0)
            orderRepository.placeOrder(
                Order(
                    id = UUID.randomUUID().toString(),
                    symbol = symbol,
                    side = if (isBuy) OrderSide.BUY else OrderSide.SELL,
                    type = OrderType.MARKET,
                    timeInForce = TimeInForce.DAY,
                    quantity = quantity,
                    price = fillPrice
                )
            )
        }
    }
}
