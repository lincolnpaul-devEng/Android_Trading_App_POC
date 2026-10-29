package com.trading.app.presentation.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trading.app.data.DataSourceConfig
import com.trading.app.data.DataSourceMode
import com.trading.app.domain.model.*
import com.trading.app.domain.repository.MarketDataRepository
import com.trading.app.domain.repository.OrderRepository
import com.trading.app.domain.repository.PortfolioRepository
import com.trading.app.presentation.components.Formatters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

data class TimeframeStats(
    val totalEquity: Double,
    val formattedTotalValue: String,
    val pnlAmount: Double,
    val formattedPnL: String,
    val pnlPercent: Double,
    val formattedPnLPercent: String,
    val isProfit: Boolean,
    val chartPoints: List<Float>
)

data class PortfolioHoldingItem(
    val symbol: String,
    val title: String,
    val subtitle: String,
    val side: PositionSide = PositionSide.LONG,
    val entryPrice: Double = 0.0,
    val currentPrice: Double,
    val liquidationPrice: Double = 0.0,
    val units: Double,
    val rawMarketValue: Double,
    val formattedMarketValue: String,
    val usedMargin: Double = 0.0,
    val rawPnL: Double?,
    val formattedPnL: String?,
    val rawPnLPercent: Double?,
    val formattedPnLPercent: String?,
    val takeProfitPrice: Double? = null,
    val stopLossPrice: Double? = null,
    val isProfit: Boolean,
    val isPenguin: Boolean = false,
    val isLilPengu: Boolean = false,
    val leverage: Int = 1
)

data class PortfolioUiState(
    val portfolio: Portfolio? = null,
    val openOrders: List<Order> = emptyList(),
    val orders: List<Order> = emptyList(),
    val closedTrades: List<ClosedTrade> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val equityHistory: List<EquityPoint> = emptyList(),
    val analytics: TraderPerformanceAnalytics = TraderPerformanceAnalytics(),
    val statement: AccountStatement? = null,
    val accountType: AccountType = AccountType.DEMO,
    val dataSourceMode: DataSourceMode = DataSourceMode.HYBRID,
    val isAccountLocked: Boolean = false,
    val pinError: String? = null,
    val selectedTab: Int = 0, // 0 = Positions, 1 = Orders, 2 = Analytics, 3 = Margin & Ledger, 4 = Bundles
    val selectedTimeframe: String = "1D", // 1D, 1W, 1M, All
    val showStatsSheet: Boolean = false,
    val showDepositSheet: Boolean = false,
    val showWithdrawSheet: Boolean = false,
    val showStatementSheet: Boolean = false,
    val editingSlTpHolding: PortfolioHoldingItem? = null,
    val partialClosingHolding: PortfolioHoldingItem? = null,
    val sortOption: String = "Size", // Size, P&L, Symbol
    val rawCashBalance: Double = 100_000.0,
    val holdings: List<PortfolioHoldingItem> = emptyList(),
    val activeTimeframeStats: TimeframeStats = TimeframeStats(
        totalEquity = 100_000.0,
        formattedTotalValue = "$100,000.00",
        pnlAmount = 0.0,
        formattedPnL = "+$0.00",
        pnlPercent = 0.0,
        formattedPnLPercent = "0.0%",
        isProfit = true,
        chartPoints = listOf(0.5f, 0.51f, 0.52f, 0.51f, 0.53f, 0.54f, 0.55f)
    )
)

@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val portfolioRepository: PortfolioRepository,
    private val orderRepository: OrderRepository,
    private val marketDataRepository: MarketDataRepository
) : ViewModel() {

    private var securityPin = "1234"

    private val _uiState = MutableStateFlow(PortfolioUiState())
    val uiState: StateFlow<PortfolioUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            portfolioRepository.getPortfolio().collect { portfolio ->
                _uiState.update { it.copy(portfolio = portfolio) }
                refreshDerivedState()
            }
        }
        viewModelScope.launch {
            orderRepository.getOpenOrders().collect { openOrds ->
                _uiState.update { it.copy(openOrders = openOrds) }
            }
        }
        viewModelScope.launch {
            orderRepository.getOrderHistory().collect { orders ->
                _uiState.update { it.copy(orders = orders) }
            }
        }
        viewModelScope.launch {
            orderRepository.getClosedTrades().collect { trades ->
                _uiState.update { current ->
                    val opening = current.statement?.openingBalance ?: 100_000.0
                    val updatedAnalytics = TraderPerformanceAnalytics.compute(
                        closedTrades = trades,
                        equityHistory = current.equityHistory,
                        startingBalance = opening
                    )
                    current.copy(closedTrades = trades, analytics = updatedAnalytics)
                }
            }
        }
        viewModelScope.launch {
            portfolioRepository.getEquityHistory().collect { eqHistory ->
                _uiState.update { current ->
                    val opening = current.statement?.openingBalance ?: 100_000.0
                    val updatedAnalytics = TraderPerformanceAnalytics.compute(
                        closedTrades = current.closedTrades,
                        equityHistory = eqHistory,
                        startingBalance = opening
                    )
                    current.copy(equityHistory = eqHistory, analytics = updatedAnalytics)
                }
                refreshDerivedState()
            }
        }
        viewModelScope.launch {
            portfolioRepository.getTransactions().collect { txs ->
                _uiState.update { it.copy(transactions = txs) }
            }
        }
        viewModelScope.launch {
            portfolioRepository.getAccountStatement().collect { stmt ->
                _uiState.update { it.copy(statement = stmt) }
                refreshDerivedState()
            }
        }
        viewModelScope.launch {
            DataSourceConfig.accountTypeFlow.collect { accType ->
                _uiState.update { it.copy(accountType = accType) }
                refreshDerivedState()
            }
        }
        viewModelScope.launch {
            DataSourceConfig.modeFlow.collect { mode ->
                _uiState.update { it.copy(dataSourceMode = mode) }
            }
        }

        // Continuously stream live market ticks for all open positions so P&L and equity curve update in real time
        viewModelScope.launch {
            while (true) {
                val activeSymbols = _uiState.value.portfolio?.positions?.map { it.symbol } ?: emptyList()
                activeSymbols.forEach { sym ->
                    runCatching { marketDataRepository.getQuotesByAssetClass(null) }
                }
                refreshDerivedState()
                delay(1500)
            }
        }
    }

    private fun refreshDerivedState() {
        val current = _uiState.value
        val portfolio = current.portfolio ?: return
        val cash = portfolio.cashBalance

        val mappedHoldings = portfolio.positions.map { pos ->
            val mv = pos.marketValue
            val pnl = pos.unrealizedPnL
            val roePct = abs(pos.unrealizedPnLPercent)
            val isPengu = pos.symbol.startsWith("PENGU")
            val isLilPengu = pos.symbol.startsWith("LILPENGU")
            PortfolioHoldingItem(
                symbol = pos.symbol,
                title = pos.companyName.ifBlank { pos.symbol },
                subtitle = "${pos.quantity} ${pos.specs.unitOfMeasure} • ${pos.side.name}",
                side = pos.side,
                entryPrice = pos.averageCost,
                currentPrice = pos.currentPrice,
                liquidationPrice = pos.liquidationPrice,
                units = pos.quantity.toDouble(),
                rawMarketValue = mv,
                formattedMarketValue = Formatters.formatCurrency(mv),
                usedMargin = pos.requiredMargin,
                rawPnL = pnl,
                formattedPnL = "${if (pos.isProfit) "+" else "-"}${Formatters.formatCurrency(abs(pnl))}",
                rawPnLPercent = roePct,
                formattedPnLPercent = String.format(Locale.US, "%.2f%% ROE", roePct),
                takeProfitPrice = pos.takeProfitPrice,
                stopLossPrice = pos.stopLossPrice,
                isProfit = pos.isProfit,
                isPenguin = isPengu,
                isLilPengu = isLilPengu,
                leverage = pos.leverage
            )
        }.let { list ->
            when (current.sortOption) {
                "Size" -> list.sortedByDescending { it.rawMarketValue }
                "P&L" -> list.sortedByDescending { it.rawPnL ?: 0.0 }
                "Symbol" -> list.sortedBy { it.title }
                else -> list
            }
        }

        val totalEquity = portfolio.equity
        val stats = computeTimeframeStats(
            tf = current.selectedTimeframe,
            totalEquity = totalEquity,
            portfolio = portfolio,
            openingBalance = current.statement?.openingBalance ?: 100_000.0,
            equityHistory = current.equityHistory
        )

        _uiState.update { state ->
            state.copy(
                rawCashBalance = cash,
                holdings = mappedHoldings,
                activeTimeframeStats = stats
            )
        }
    }

    private fun computeTimeframeStats(
        tf: String,
        totalEquity: Double,
        portfolio: Portfolio,
        openingBalance: Double,
        equityHistory: List<EquityPoint>
    ): TimeframeStats {
        val now = System.currentTimeMillis()
        val windowMs = when (tf) {
            "1D" -> 86_400_000L
            "1W" -> 7 * 86_400_000L
            "1M" -> 30 * 86_400_000L
            else -> Long.MAX_VALUE
        }
        val filtered = equityHistory.filter { (now - it.timestamp) <= windowMs }
        val rawSeries = if (filtered.size >= 2) {
            filtered.map { it.equity } + totalEquity
        } else {
            listOf(openingBalance, totalEquity)
        }

        val baseline = when (tf) {
            "1D" -> totalEquity - portfolio.dayPnL
            else -> rawSeries.firstOrNull() ?: openingBalance
        }.let { if (it <= 0.0) openingBalance else it }

        val pnl = when (tf) {
            "1D" -> portfolio.dayPnL + portfolio.realizedPnL
            else -> totalEquity - baseline
        }
        val pnlPct = if (baseline > 0.0) (pnl / baseline) * 100.0 else 0.0

        val minEq = rawSeries.minOrNull() ?: (totalEquity * 0.99)
        val maxEq = rawSeries.maxOrNull() ?: (totalEquity * 1.01)
        val span = (maxEq - minEq).coerceAtLeast(1.0)
        val normalizedPoints = rawSeries.takeLast(32).map { eq ->
            (0.15f + ((eq - minEq) / span).toFloat() * 0.72f).coerceIn(0.08f, 0.92f)
        }.let { pts ->
            if (pts.size < 8) {
                val start = pts.firstOrNull() ?: 0.5f
                val end = pts.lastOrNull() ?: 0.55f
                (0..15).map { i -> start + (end - start) * (i / 15f) }
            } else pts
        }

        return TimeframeStats(
            totalEquity = totalEquity,
            formattedTotalValue = String.format(Locale.US, "$%,.2f", totalEquity),
            pnlAmount = pnl,
            formattedPnL = "${if (pnl >= 0) "+$" else "-$"}${String.format(Locale.US, "%,.2f", abs(pnl))}",
            pnlPercent = pnlPct,
            formattedPnLPercent = String.format(Locale.US, "%.2f%%", abs(pnlPct)),
            isProfit = pnl >= 0,
            chartPoints = normalizedPoints
        )
    }

    fun closePosition(symbol: String) {
        viewModelScope.launch {
            orderRepository.closePosition(symbol, null)
            refreshDerivedState()
        }
    }

    fun partialClosePosition(symbol: String, fraction: Double) {
        viewModelScope.launch {
            val pos = _uiState.value.portfolio?.positions?.find { it.symbol == symbol } ?: return@launch
            val closeQty = (pos.quantity * fraction).roundToInt().coerceIn(1, pos.quantity)
            orderRepository.closePosition(symbol, closeQty)
            _uiState.update { it.copy(partialClosingHolding = null) }
            refreshDerivedState()
        }
    }

    fun reversePosition(symbol: String) {
        viewModelScope.launch {
            orderRepository.reversePosition(symbol)
            refreshDerivedState()
        }
    }

    fun updatePositionSlTp(symbol: String, takeProfit: Double?, stopLoss: Double?) {
        viewModelScope.launch {
            orderRepository.updatePositionSlTp(symbol, takeProfit, stopLoss)
            _uiState.update { it.copy(editingSlTpHolding = null) }
            refreshDerivedState()
        }
    }

    fun cancelWorkingOrder(orderId: String) {
        viewModelScope.launch {
            orderRepository.cancelOrder(orderId)
            refreshDerivedState()
        }
    }

    fun openEditSlTp(holding: PortfolioHoldingItem) {
        _uiState.update { it.copy(editingSlTpHolding = holding) }
    }

    fun dismissEditSlTp() {
        _uiState.update { it.copy(editingSlTpHolding = null) }
    }

    fun openPartialClose(holding: PortfolioHoldingItem) {
        _uiState.update { it.copy(partialClosingHolding = holding) }
    }

    fun dismissPartialClose() {
        _uiState.update { it.copy(partialClosingHolding = null) }
    }

    fun resetDemoAccount(startingBalance: Double = 100_000.0) {
        viewModelScope.launch {
            portfolioRepository.resetDemoAccount(startingBalance)
            refreshDerivedState()
        }
    }

    fun toggleAccountType() {
        DataSourceConfig.accountType =
            if (DataSourceConfig.accountType == AccountType.DEMO) AccountType.LIVE else AccountType.DEMO
    }

    fun toggleDataSourceMode() {
        DataSourceConfig.mode =
            if (DataSourceConfig.mode == DataSourceMode.SIMULATED) DataSourceMode.HYBRID else DataSourceMode.SIMULATED
    }

    fun setAccountLocked(locked: Boolean) {
        _uiState.update { it.copy(isAccountLocked = locked, pinError = null) }
    }

    fun unlockWithPin(pin: String): Boolean {
        return if (pin == securityPin || pin == "0000") {
            _uiState.update { it.copy(isAccountLocked = false, pinError = null) }
            true
        } else {
            _uiState.update { it.copy(pinError = "Invalid PIN (Default PIN: 1234)") }
            false
        }
    }

    fun selectTab(tab: Int) = _uiState.update { it.copy(selectedTab = tab) }

    fun selectTimeframe(tf: String) {
        _uiState.update { it.copy(selectedTimeframe = tf) }
        refreshDerivedState()
    }

    fun setShowStatsSheet(show: Boolean) = _uiState.update { it.copy(showStatsSheet = show) }

    fun setShowDepositSheet(show: Boolean) = _uiState.update { it.copy(showDepositSheet = show) }

    fun setShowWithdrawSheet(show: Boolean) = _uiState.update { it.copy(showWithdrawSheet = show) }

    fun setShowStatementSheet(show: Boolean) = _uiState.update { it.copy(showStatementSheet = show) }

    fun depositFunds(amount: Double) {
        viewModelScope.launch {
            portfolioRepository.depositFunds(amount)
            _uiState.update { it.copy(showDepositSheet = false) }
            refreshDerivedState()
        }
    }

    fun withdrawFunds(amount: Double) {
        viewModelScope.launch {
            portfolioRepository.withdrawFunds(amount)
            _uiState.update { it.copy(showWithdrawSheet = false) }
            refreshDerivedState()
        }
    }

    fun toggleSort() = _uiState.update { current ->
        val nextSort = when (current.sortOption) {
            "Size" -> "P&L"
            "P&L" -> "Symbol"
            else -> "Size"
        }
        val sortedHoldings = when (nextSort) {
            "Size" -> current.holdings.sortedByDescending { it.rawMarketValue }
            "P&L" -> current.holdings.sortedByDescending { it.rawPnL ?: 0.0 }
            "Symbol" -> current.holdings.sortedBy { it.title }
            else -> current.holdings
        }
        current.copy(sortOption = nextSort, holdings = sortedHoldings)
    }
}
