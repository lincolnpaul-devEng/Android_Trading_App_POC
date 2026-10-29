package com.trading.app.domain.repository

import com.trading.app.domain.model.*
import kotlinx.coroutines.flow.Flow

interface MarketDataRepository {
    fun getLiveQuote(symbol: String): Flow<StockQuote>
    fun getLiveQuotes(symbols: List<String>): Flow<List<StockQuote>>
    fun getCandlestickData(symbol: String, interval: String): Flow<List<Candlestick>>
    fun getOrderBook(symbol: String): Flow<OrderBook>
    fun getMarketIndices(): Flow<List<MarketIndex>>
    fun getSectorPerformance(): Flow<List<SectorPerformance>>
    fun getTopMovers(): Flow<List<StockQuote>>
    fun getQuotesByAssetClass(assetClass: AssetClass? = null): Flow<List<StockQuote>>
    fun searchSymbols(query: String): Flow<List<StockQuote>>
}
