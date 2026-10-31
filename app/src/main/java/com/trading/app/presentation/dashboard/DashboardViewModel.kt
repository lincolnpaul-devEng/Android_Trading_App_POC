package com.trading.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trading.app.domain.model.AssetClass
import com.trading.app.domain.model.StockQuote
import com.trading.app.domain.repository.MarketDataRepository
import com.trading.app.domain.repository.PortfolioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val marketDataRepository: MarketDataRepository,
    private val portfolioRepository: PortfolioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var assetJob: Job? = null

    init {
        loadMarketData()
        loadMultiAssetQuotes(null)
        loadAccountSummary()
    }

    private fun loadAccountSummary() {
        viewModelScope.launch {
            portfolioRepository.getPortfolio().collect { p ->
                val totalCost = p.totalCostBasis
                val returnPct = if (totalCost > 0.0) {
                    (p.totalUnrealizedPnL / totalCost) * 100.0
                } else if (p.equity > 0.0 && p.dayPnL != 0.0) {
                    (p.dayPnL / p.equity) * 100.0
                } else {
                    0.0
                }
                _uiState.update {
                    it.copy(
                        portfolio = p,
                        positions = p.positions,
                        totalEquity = p.equity,
                        availableMargin = p.buyingPower,
                        unrealizedPnL = p.totalUnrealizedPnL,
                        dayPnL = p.dayPnL,
                        todayReturnPct = returnPct,
                        totalCostBasis = totalCost,
                        investedAmount = totalCost,
                        totalProfitAmount = p.totalUnrealizedPnL
                    )
                }
            }
        }
    }

    fun depositFunds(amount: Double) {
        viewModelScope.launch {
            portfolioRepository.depositFunds(amount)
        }
    }

    fun withdrawFunds(amount: Double) {
        viewModelScope.launch {
            portfolioRepository.withdrawFunds(amount)
        }
    }

    fun setShowDepositSheet(show: Boolean) = _uiState.update { it.copy(showDepositSheet = show) }
    fun setShowWithdrawSheet(show: Boolean) = _uiState.update { it.copy(showWithdrawSheet = show) }

    private fun loadMarketData() {
        viewModelScope.launch {
            marketDataRepository.getMarketIndices().collect { indices ->
                _uiState.update { it.copy(indices = indices, isLoading = false) }
            }
        }
        viewModelScope.launch {
            marketDataRepository.getTopMovers().collect { movers ->
                _uiState.update { it.copy(topMovers = movers) }
            }
        }
        viewModelScope.launch {
            marketDataRepository.getSectorPerformance().collect { sectors ->
                _uiState.update { it.copy(sectors = sectors) }
            }
        }
    }

    fun selectAssetClass(assetClass: AssetClass?) {
        _uiState.update { it.copy(selectedAssetClass = assetClass) }
        loadMultiAssetQuotes(assetClass)
    }

    private fun loadMultiAssetQuotes(assetClass: AssetClass?) {
        assetJob?.cancel()
        assetJob = viewModelScope.launch {
            marketDataRepository.getQuotesByAssetClass(assetClass).collect { quotes ->
                _uiState.update { it.copy(multiAssetQuotes = quotes) }
            }
        }
    }

    fun showSpecs(quote: StockQuote) = _uiState.update { it.copy(selectedSpecsQuote = quote) }
    fun dismissSpecs() = _uiState.update { it.copy(selectedSpecsQuote = null) }

    fun setViewMode(mode: DashboardViewMode) = _uiState.update { it.copy(selectedViewMode = mode) }
    fun toggleViewMode() = _uiState.update { 
        it.copy(
            selectedViewMode = if (it.selectedViewMode == DashboardViewMode.OVERVIEW) DashboardViewMode.ANALYTICS_WHEEL else DashboardViewMode.OVERVIEW
        )
    }
}
