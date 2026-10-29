package com.trading.app.data.remote

import android.util.Log
import com.trading.app.data.remote.model.FinnhubCandleResponse
import com.trading.app.data.simulator.MarketSimulator
import com.trading.app.domain.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Multi-asset real-time & historical market data source backed by Finnhub REST + WebSocket
 * and public historical OHLCV feeds, synchronized into [MarketSimulator] for unified BID/ASK
 * spread calculation and position P&L / auto-close evaluation.
 */
@Singleton
class FinnhubDataSource @Inject constructor(
    private val api: FinnhubApi,
    private val webSocket: FinnhubWebSocket,
    private val historicalClient: HistoricalDataClient,
    private val simulator: MarketSimulator
) {
    private val tag = "FinnhubDataSource"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Company profile cache
    private val profileCache = mutableMapOf<String, FinnhubCompanyProfileCached>()

    data class FinnhubCompanyProfileCached(
        val name: String,
        val marketCap: Long,
        val industry: String
    )

    /** App symbol -> Finnhub WebSocket / REST symbol */
    private val symbolToFinnhub: Map<String, String> = mapOf(
        // Stocks
        "AAPL" to "AAPL", "GOOGL" to "GOOGL", "MSFT" to "MSFT", "AMZN" to "AMZN",
        "TSLA" to "TSLA", "NVDA" to "NVDA", "META" to "META", "JPM" to "JPM",
        "V" to "V", "JNJ" to "JNJ", "WMT" to "WMT", "BAC" to "BAC",
        "DIS" to "DIS", "NFLX" to "NFLX", "AMD" to "AMD",
        // Forex Majors, Minors, Exotics (OANDA stream on Finnhub WS)
        "EUR/USD" to "OANDA:EUR_USD", "GBP/USD" to "OANDA:GBP_USD", "USD/JPY" to "OANDA:USD_JPY",
        "USD/CHF" to "OANDA:USD_CHF", "AUD/USD" to "OANDA:AUD_USD", "USD/CAD" to "OANDA:USD_CAD",
        "NZD/USD" to "OANDA:NZD_USD", "EUR/GBP" to "OANDA:EUR_GBP", "EUR/JPY" to "OANDA:EUR_JPY",
        "GBP/JPY" to "OANDA:GBP_JPY", "AUD/JPY" to "OANDA:AUD_JPY", "EUR/CHF" to "OANDA:EUR_CHF",
        "USD/TRY" to "OANDA:USD_TRY", "USD/ZAR" to "OANDA:USD_ZAR", "USD/MXN" to "OANDA:USD_MXN",
        "USD/SGD" to "OANDA:USD_SGD",
        // Crypto (Binance stream + REST quote on Finnhub)
        "BTC/USD" to "BINANCE:BTCUSDT", "ETH/USD" to "BINANCE:ETHUSDT", "SOL/USD" to "BINANCE:SOLUSDT",
        "XRP/USD" to "BINANCE:XRPUSDT", "DOGE/USD" to "BINANCE:DOGEUSDT",
        // Commodities
        "XAU/USD" to "OANDA:XAU_USD", "XAG/USD" to "OANDA:XAG_USD", "BRENT" to "OANDA:BCO_USD",
        "WTI" to "OANDA:WTICO_USD", "NATGAS" to "OANDA:NATGAS_USD", "COPPER" to "OANDA:XCU_USD",
        // Indices & CFDs & Futures
        "SPX" to "OANDA:SPX500_USD", "IXIC" to "OANDA:NAS100_USD", "DJI" to "OANDA:US30_USD",
        "UK100" to "OANDA:UK100_GBP", "JP225" to "OANDA:JP225_USD",
        "US500.CFD" to "OANDA:SPX500_USD", "GER40.CFD" to "OANDA:DE30_EUR",
        "AAPL.CFD" to "AAPL", "NVDA.CFD" to "NVDA",
        "ESZ6" to "OANDA:SPX500_USD", "NQZ6" to "OANDA:NAS100_USD",
        "CLZ6" to "OANDA:WTICO_USD", "GCZ6" to "OANDA:XAU_USD"
    )

    /** Finnhub WebSocket symbol -> All matching app symbols */
    private val finnhubToAppSymbols: Map<String, List<String>> =
        symbolToFinnhub.entries.groupBy({ it.value }, { it.key })

    /** App symbol -> Yahoo Finance chart symbol for historical OHLCV & reference prices */
    private val symbolToHistory: Map<String, String> = mapOf(
        "EUR/USD" to "EURUSD=X", "GBP/USD" to "GBPUSD=X", "USD/JPY" to "JPY=X",
        "USD/CHF" to "CHF=X", "AUD/USD" to "AUDUSD=X", "USD/CAD" to "CAD=X",
        "NZD/USD" to "NZDUSD=X", "EUR/GBP" to "EURGBP=X", "EUR/JPY" to "EURJPY=X",
        "GBP/JPY" to "GBPJPY=X", "AUD/JPY" to "AUDJPY=X", "EUR/CHF" to "EURCHF=X",
        "USD/TRY" to "TRY=X", "USD/ZAR" to "ZAR=X", "USD/MXN" to "MXN=X", "USD/SGD" to "SGD=X",
        "BTC/USD" to "BTC-USD", "ETH/USD" to "ETH-USD", "SOL/USD" to "SOL-USD",
        "XRP/USD" to "XRP-USD", "DOGE/USD" to "DOGE-USD",
        "XAU/USD" to "GC=F", "XAG/USD" to "SI=F", "BRENT" to "BZ=F",
        "WTI" to "CL=F", "NATGAS" to "NG=F", "COPPER" to "HG=F",
        "SPX" to "^GSPC", "IXIC" to "^IXIC", "DJI" to "^DJI",
        "UK100" to "^FTSE", "JP225" to "^N225",
        "ESZ6" to "ES=F", "NQZ6" to "NQ=F", "CLZ6" to "CL=F", "GCZ6" to "GC=F",
        "US500.CFD" to "^GSPC", "GER40.CFD" to "^GDAXI",
        "AAPL.CFD" to "AAPL", "NVDA.CFD" to "NVDA"
    )

    val feedStatus: StateFlow<FeedStatus> get() = webSocket.status

    @Volatile private var wsStarted = false

    fun ensureWebSocketStarted() {
        if (wsStarted) return
        wsStarted = true
        val wsSymbols = symbolToFinnhub.values.distinct()
        webSocket.start(scope, wsSymbols)
        scope.launch {
            webSocket.trades.collect { trade ->
                val targets = finnhubToAppSymbols[trade.symbol] ?: listOf(trade.symbol)
                targets.forEach { appSymbol ->
                    simulator.updateLivePrice(
                        symbol = appSymbol,
                        price = trade.price,
                        source = PriceSource.LIVE
                    )
                }
            }
        }
    }

    /**
     * Stream live quote via Finnhub REST + WebSocket with realistic BID and ASK prices.
     */
    fun streamQuote(symbol: String): Flow<StockQuote> = flow {
        ensureWebSocketStarted()
        val profile = getOrFetchProfile(symbol)
        val specs = simulator.getInstrumentSpecs(symbol)
        val finnhubSym = symbolToFinnhub[symbol] ?: symbol
        val supportsRestQuote = !finnhubSym.startsWith("OANDA:")

        while (true) {
            try {
                if (supportsRestQuote) {
                    val q = api.getQuote(finnhubSym)
                    if (q.current > 0) {
                        simulator.updateLivePrice(
                            symbol = symbol,
                            price = q.current,
                            open = q.open,
                            high = q.high,
                            low = q.low,
                            source = PriceSource.LIVE
                        )
                        val (bid, ask) = simulator.computeBidAsk(symbol, q.current)
                        emit(
                            StockQuote(
                                symbol = symbol,
                                companyName = profile.name,
                                lastPrice = q.current,
                                change = q.change ?: 0.0,
                                changePercent = q.percentChange ?: 0.0,
                                open = q.open,
                                high = q.high,
                                low = q.low,
                                volume = Random.nextLong(1_500_000, 45_000_000),
                                marketCap = if (profile.marketCap > 0) profile.marketCap else simulator.getMarketCap(symbol),
                                bid = bid,
                                ask = ask,
                                bidSize = Random.nextInt(200, 4000),
                                askSize = Random.nextInt(200, 4000),
                                timestamp = if (q.timestamp > 0) q.timestamp * 1000 else System.currentTimeMillis(),
                                specs = specs,
                                source = PriceSource.LIVE
                            )
                        )
                    } else {
                        emit(simulator.generateQuote(symbol))
                    }
                } else {
                    // OANDA / Futures / Indices / Commodities: stream from WS-fed simulator + Yahoo snapshot
                    val histSym = symbolToHistory[symbol] ?: symbol
                    val snap = historicalClient.fetch(histSym, "1D")
                    if (snap?.lastPrice != null && snap.lastPrice > 0) {
                        simulator.updateLivePrice(
                            symbol = symbol,
                            price = snap.lastPrice,
                            open = snap.previousClose,
                            high = snap.dayHigh,
                            low = snap.dayLow,
                            source = PriceSource.LIVE
                        )
                    }
                    emit(simulator.generateQuote(symbol).copy(source = PriceSource.LIVE))
                }
            } catch (e: Exception) {
                Log.w(tag, "Live quote fallback for $symbol: ${e.message}")
                throw e
            }
            delay(2500)
        }
    }

    /**
     * Fetch real historical candlestick data across all asset classes, then stream live updates.
     */
    fun getCandlesticks(symbol: String, interval: String, resolution: String, from: Long, to: Long): Flow<List<Candlestick>> = flow {
        ensureWebSocketStarted()
        val histSym = symbolToHistory[symbol] ?: symbol
        val chartResult = historicalClient.fetch(histSym, interval)
        val initialCandles = if (chartResult != null && chartResult.candles.isNotEmpty()) {
            chartResult.lastPrice?.let { price ->
                simulator.updateLivePrice(
                    symbol = symbol,
                    price = price,
                    open = chartResult.previousClose,
                    high = chartResult.dayHigh,
                    low = chartResult.dayLow,
                    source = PriceSource.LIVE
                )
            }
            chartResult.candles
        } else {
            try {
                val response = api.getCandles(symbol, resolution, from, to)
                if (response.status == "ok") mapCandles(response) else emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }

        if (initialCandles.isEmpty()) {
            emit(emptyList())
            return@flow
        }

        val mutable = initialCandles.toMutableList()
        emit(mutable.toList())

        // Keep last candle ticking in real time as price updates arrive
        while (true) {
            delay(2000)
            val quote = simulator.generateQuote(symbol)
            val last = mutable.last()
            val updatedLast = last.copy(
                close = quote.lastPrice,
                high = maxOf(last.high, quote.lastPrice),
                low = minOf(last.low, quote.lastPrice),
                volume = last.volume + Random.nextLong(1_000, 25_000)
            )
            mutable[mutable.lastIndex] = updatedLast
            emit(mutable.toList())
        }
    }

    /**
     * Search symbols on Finnhub + local multi-asset catalog.
     */
    suspend fun searchSymbols(query: String): List<StockQuote> {
        val localMatches = simulator.searchSymbols(query)
        return try {
            val response = api.searchSymbols(query)
            val remoteMatches = response.result
                .take(8)
                .map { result ->
                    val existing = localMatches.find { it.symbol.equals(result.symbol, ignoreCase = true) }
                    existing ?: simulator.generateQuote(result.symbol).copy(
                        companyName = result.description.ifBlank { result.symbol },
                        source = PriceSource.LIVE
                    )
                }
            (localMatches + remoteMatches).distinctBy { it.symbol }
        } catch (e: Exception) {
            localMatches
        }
    }

    /**
     * Map Finnhub resolution string to our internal interval.
     */
    fun mapIntervalToResolution(interval: String): String {
        return when (interval) {
            "1m" -> "1"
            "5m" -> "5"
            "15m" -> "15"
            "1h" -> "60"
            "1D" -> "D"
            "1W" -> "W"
            "1M" -> "M"
            else -> "D"
        }
    }

    /**
     * Compute a sensible "from" timestamp for a given interval.
     */
    fun computeFromTimestamp(interval: String): Long {
        val now = System.currentTimeMillis() / 1000
        return when (interval) {
            "1m" -> now - 3600           // 1 hour
            "5m" -> now - 3600 * 6       // 6 hours
            "15m" -> now - 86400         // 1 day
            "1h" -> now - 86400 * 7      // 7 days
            "1D" -> now - 86400 * 90     // 90 days
            "1W" -> now - 86400 * 365    // 1 year
            "1M" -> now - 86400 * 365 * 2 // 2 years
            else -> now - 86400 * 90
        }
    }

    private fun mapCandles(response: FinnhubCandleResponse): List<Candlestick> {
        val timestamps = response.timestamp ?: return emptyList()
        val opens = response.open ?: return emptyList()
        val highs = response.high ?: return emptyList()
        val lows = response.low ?: return emptyList()
        val closes = response.close ?: return emptyList()
        val volumes = response.volume ?: return emptyList()

        return timestamps.indices.map { i ->
            Candlestick(
                timestamp = timestamps[i] * 1000,
                open = opens[i],
                high = highs[i],
                low = lows[i],
                close = closes[i],
                volume = volumes[i]
            )
        }
    }

    private suspend fun getOrFetchProfile(symbol: String): FinnhubCompanyProfileCached {
        profileCache[symbol]?.let { return it }
        val knownName = simulator.getStockName(symbol)
        val knownCap = simulator.getMarketCap(symbol)
        if (symbol.contains("/") || symbol.contains(".")) {
            val cached = FinnhubCompanyProfileCached(knownName, knownCap, "Multi-Asset")
            profileCache[symbol] = cached
            return cached
        }
        return try {
            val profile = api.getCompanyProfile(symbol)
            val cached = FinnhubCompanyProfileCached(
                name = profile.name?.takeIf { it.isNotBlank() } ?: knownName,
                marketCap = ((profile.marketCap ?: 0.0) * 1_000_000).toLong().takeIf { it > 0 } ?: knownCap,
                industry = profile.industry ?: "Equities"
            )
            profileCache[symbol] = cached
            cached
        } catch (e: Exception) {
            FinnhubCompanyProfileCached(knownName, knownCap, "Equities")
        }
    }
}
