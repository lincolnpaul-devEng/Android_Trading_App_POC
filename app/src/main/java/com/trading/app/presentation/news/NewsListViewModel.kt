package com.trading.app.presentation.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trading.app.domain.model.MarketNewsArticle
import com.trading.app.domain.model.NewsSentiment
import com.trading.app.domain.model.StockQuote
import com.trading.app.domain.repository.MarketDataRepository
import com.trading.app.domain.repository.NewsRepository
import com.trading.app.util.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class NewsFilterCategory(val label: String) {
    ALL("All"),
    TECH("Tech & Equities"),
    CRYPTO("Crypto"),
    FOREX("Forex"),
    BULLISH("Bullish"),
    BEARISH("Bearish")
}

data class NewsListUiState(
    val articles: List<MarketNewsArticle> = emptyList(),
    val filteredArticles: List<MarketNewsArticle> = emptyList(),
    val selectedFilter: NewsFilterCategory = NewsFilterCategory.ALL,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val loadError: String? = null,
    val isNetworkOnline: Boolean = true,
    val selectedArticle: MarketNewsArticle? = null,
    val relatedQuotes: List<StockQuote> = emptyList()
)

@HiltViewModel
class NewsListViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
    private val marketDataRepository: MarketDataRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewsListUiState())
    val uiState: StateFlow<NewsListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            newsRepository.getNewsStream().collect { articles ->
                _uiState.update { current ->
                    current.copy(
                        articles = articles,
                        filteredArticles = filterArticles(articles, current.selectedFilter, current.searchQuery),
                        loadError = if (articles.isEmpty() && !current.isLoading) "Error loading news or no internet connection" else null
                    )
                }
            }
        }
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _uiState.update { it.copy(isNetworkOnline = online) }
                if (online) {
                    refreshNews()
                } else {
                    _uiState.update { it.copy(loadError = "Error loading news or no internet connection") }
                }
            }
        }
        refreshNews()
    }

    fun refreshNews() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadError = null) }
            if (!networkMonitor.isConnected()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadError = "Error loading news or no internet connection"
                    )
                }
                return@launch
            }
            val result = newsRepository.refreshNews()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    loadError = if (result.isEmpty()) "Error loading news or no internet connection" else null
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { current ->
            current.copy(
                searchQuery = query,
                filteredArticles = filterArticles(current.articles, current.selectedFilter, query)
            )
        }
    }

    fun selectFilter(filter: NewsFilterCategory) {
        _uiState.update { current ->
            current.copy(
                selectedFilter = filter,
                filteredArticles = filterArticles(current.articles, filter, current.searchQuery)
            )
        }
    }

    fun selectArticle(article: MarketNewsArticle) {
        val symbolsToFetch = if (article.entities.isNotEmpty()) {
            article.entities.map { it.symbol }
        } else {
            listOfNotNull(article.symbol, "NVDA", "AAPL", "MSFT")
        }

        viewModelScope.launch {
            marketDataRepository.getLiveQuotes(symbolsToFetch).firstOrNull()?.let { quotes ->
                _uiState.update { it.copy(selectedArticle = article, relatedQuotes = quotes) }
            } ?: run {
                _uiState.update { it.copy(selectedArticle = article, relatedQuotes = emptyList()) }
            }
        }
    }

    fun dismissArticle() {
        _uiState.update { it.copy(selectedArticle = null, relatedQuotes = emptyList()) }
    }

    private fun filterArticles(
        articles: List<MarketNewsArticle>,
        filter: NewsFilterCategory,
        query: String
    ): List<MarketNewsArticle> {
        val queryLower = query.trim().lowercase()

        return articles.filter { article ->
            val matchesFilter = when (filter) {
                NewsFilterCategory.ALL -> true
                NewsFilterCategory.TECH -> article.entities.any { it.symbol in listOf("AAPL", "MSFT", "NVDA", "GOOGL", "AMZN", "META", "TSLA") } ||
                        article.title.contains("tech", ignoreCase = true) || article.title.contains("ai", ignoreCase = true)
                NewsFilterCategory.CRYPTO -> article.entities.any { it.symbol in listOf("BTC", "ETH", "SOL", "DOGE") } ||
                        article.title.contains("crypto", ignoreCase = true) || article.title.contains("bitcoin", ignoreCase = true) || article.title.contains("ethereum", ignoreCase = true)
                NewsFilterCategory.FOREX -> article.entities.any { it.symbol.contains('/') || it.symbol in listOf("EUR", "USD", "GBP", "JPY") } ||
                        article.title.contains("forex", ignoreCase = true) || article.title.contains("bank", ignoreCase = true) || article.title.contains("rate", ignoreCase = true)
                NewsFilterCategory.BULLISH -> article.sentiment == NewsSentiment.BULLISH
                NewsFilterCategory.BEARISH -> article.sentiment == NewsSentiment.BEARISH
            }

            val matchesQuery = if (queryLower.isBlank()) {
                true
            } else {
                article.title.lowercase().contains(queryLower) ||
                        article.summary.lowercase().contains(queryLower) ||
                        article.source.lowercase().contains(queryLower) ||
                        article.entities.any { it.symbol.lowercase().contains(queryLower) }
            }

            matchesFilter && matchesQuery
        }
    }
}
