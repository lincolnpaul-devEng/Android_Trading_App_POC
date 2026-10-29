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
                _uiState.update {
                    it.copy(
                        totalEquity = p.equity,
                        availableMargin = p.buyingPower,
                        unrealizedPnL = p.totalUnrealizedPnL
                    )
                }
            }
        }
    }

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
}
