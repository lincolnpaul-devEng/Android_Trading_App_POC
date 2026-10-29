package com.trading.app.presentation.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trading.app.domain.model.AlertCondition
import com.trading.app.domain.model.MarketNewsArticle
import com.trading.app.domain.model.PriceAlert
import com.trading.app.domain.model.WatchlistItem
import com.trading.app.domain.repository.MarketDataRepository
import com.trading.app.domain.repository.NewsRepository
import com.trading.app.domain.repository.PortfolioRepository
import com.trading.app.domain.repository.WatchlistRepository
import com.trading.app.util.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WatchlistUiState(
    val items: List<WatchlistItem> = emptyList(),
    val priceAlerts: List<PriceAlert> = emptyList(),
    val marketNews: List<MarketNewsArticle> = emptyList(),
    val isNewsLoading: Boolean = false,
    val newsError: String? = null,
    val isNetworkOnline: Boolean = true,
    val alertSettingSymbol: String? = null,
    val alertSettingPrice: Double = 0.0,
    val searchQuery: String = "",
    val searchResults: List<com.trading.app.domain.model.StockQuote> = emptyList(),
    val isSearching: Boolean = false
)

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val watchlistRepository: WatchlistRepository,
    private val marketDataRepository: MarketDataRepository,
    private val portfolioRepository: PortfolioRepository,
    private val newsRepository: NewsRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(WatchlistUiState())
    val uiState: StateFlow<WatchlistUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            watchlistRepository.getWatchlist().collect { items ->
                _uiState.update { it.copy(items = items) }
            }
        }
        viewModelScope.launch {
            portfolioRepository.getPriceAlerts().collect { alerts ->
                _uiState.update { it.copy(priceAlerts = alerts) }
            }
        }
        viewModelScope.launch {
            newsRepository.getNewsStream().collect { news ->
                _uiState.update { current ->
                    current.copy(
                        marketNews = news,
                        newsError = if (news.isEmpty() && !current.isNewsLoading) "Error loading news or no internet connection" else null
                    )
                }
            }
        }
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _uiState.update { it.copy(isNetworkOnline = online) }
                if (online) {
                    reloadNews()
                } else {
                    _uiState.update { it.copy(newsError = "Error loading news or no internet connection") }
                }
            }
        }
        viewModelScope.launch {
            while (true) {
                if (_uiState.value.isNetworkOnline) {
                    val symbols = _uiState.value.items.map { it.symbol }
                    newsRepository.pollBreakingNewsAndNotify(symbols)
                }
                delay(60_000L)
            }
        }
    }

    fun reloadNews() {
        viewModelScope.launch {
            _uiState.update { it.copy(isNewsLoading = true, newsError = null) }
            if (!networkMonitor.isConnected()) {
                _uiState.update {
                    it.copy(
                        isNewsLoading = false,
                        newsError = "Error loading news or no internet connection"
                    )
                }
                return@launch
            }
            val symbols = _uiState.value.items.map { it.symbol }
            val result = newsRepository.refreshNews(symbols)
            _uiState.update {
                it.copy(
                    isNewsLoading = false,
                    newsError = if (result.isEmpty()) "Error loading news or no internet connection" else null
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.length >= 1) {
            viewModelScope.launch {
                marketDataRepository.searchSymbols(query).collect { results ->
                    _uiState.update { it.copy(searchResults = results, isSearching = true) }
                }
            }
        } else {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
        }
    }

    fun addToWatchlist(symbol: String, companyName: String) {
        viewModelScope.launch {
            watchlistRepository.addToWatchlist(symbol, companyName)
            _uiState.update { it.copy(searchQuery = "", searchResults = emptyList(), isSearching = false) }
        }
    }

    fun removeFromWatchlist(symbol: String) {
        viewModelScope.launch {
            watchlistRepository.removeFromWatchlist(symbol)
        }
    }

    fun openSetAlert(symbol: String, currentPrice: Double) {
        _uiState.update { it.copy(alertSettingSymbol = symbol, alertSettingPrice = currentPrice) }
    }

    fun closeSetAlert() {
        _uiState.update { it.copy(alertSettingSymbol = null) }
    }

    fun addPriceAlert(symbol: String, targetPrice: Double, condition: AlertCondition) {
        viewModelScope.launch {
            portfolioRepository.addPriceAlert(symbol, targetPrice, condition)
            closeSetAlert()
        }
    }

    fun removePriceAlert(alertId: String) {
        viewModelScope.launch {
            portfolioRepository.removePriceAlert(alertId)
        }
    }
}
