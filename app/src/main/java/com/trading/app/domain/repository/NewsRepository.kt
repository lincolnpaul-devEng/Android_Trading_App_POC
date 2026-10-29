package com.trading.app.domain.repository

import com.trading.app.domain.model.MarketNewsArticle
import kotlinx.coroutines.flow.Flow

interface NewsRepository {
    fun getNewsStream(symbols: List<String> = emptyList()): Flow<List<MarketNewsArticle>>
    suspend fun refreshNews(symbols: List<String> = emptyList()): List<MarketNewsArticle>
    suspend fun pollBreakingNewsAndNotify(watchlistSymbols: List<String>)
}
