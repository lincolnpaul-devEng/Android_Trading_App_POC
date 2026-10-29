package com.trading.app.data.remote

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.trading.app.domain.model.Candlestick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

/**
 * Historical OHLCV + session reference prices from the public Yahoo Finance chart endpoint.
 * Used because Finnhub's candle endpoints are premium-only; covers stocks, FX, indices,
 * futures, commodities and crypto. Uses its own OkHttp client (no Finnhub token attached).
 */
class HistoricalDataClient(private val client: OkHttpClient) {

    private val gson = Gson()

    data class ChartResult(
        val candles: List<Candlestick>,
        val lastPrice: Double?,
        val previousClose: Double?,
        val dayHigh: Double?,
        val dayLow: Double?,
        val volume: Long?
    )

    /** @param interval one of the app intervals: 1m, 5m, 15m, 1h, 1D, 1W, 1M */
    suspend fun fetch(symbol: String, interval: String): ChartResult? = withContext(Dispatchers.IO) {
        val (yInterval, range) = mapInterval(interval)
        val url = "https://query1.finance.yahoo.com/v8/finance/chart/" +
            URLEncoder.encode(symbol, "UTF-8") + "?interval=$yInterval&range=$range"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) TradingApp/1.0")
            .build()
        try {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                parse(resp.body?.string() ?: return@withContext null)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun parse(json: String): ChartResult? {
        val root = gson.fromJson(json, ChartEnvelope::class.java) ?: return null
        val result = root.chart?.result?.firstOrNull() ?: return null
        val ts = result.timestamp.orEmpty()
        val q = result.indicators?.quote?.firstOrNull()
        val candles = ts.indices.mapNotNull { i ->
            val o = q?.open?.getOrNull(i) ?: return@mapNotNull null
            val h = q.high?.getOrNull(i) ?: return@mapNotNull null
            val l = q.low?.getOrNull(i) ?: return@mapNotNull null
            val c = q.close?.getOrNull(i) ?: return@mapNotNull null
            Candlestick(ts[i] * 1000, o, h, l, c, q.volume?.getOrNull(i) ?: 0L)
        }
        val m = result.meta
        return ChartResult(
            candles = candles,
            lastPrice = m?.regularMarketPrice,
            previousClose = m?.previousClose ?: m?.chartPreviousClose,
            dayHigh = m?.regularMarketDayHigh,
            dayLow = m?.regularMarketDayLow,
            volume = m?.regularMarketVolume
        )
    }

    companion object {
        fun mapInterval(interval: String): Pair<String, String> = when (interval) {
            "1m" -> "1m" to "1d"
            "5m" -> "5m" to "5d"
            "15m" -> "15m" to "5d"
            "1h" -> "60m" to "1mo"
            "1W" -> "1wk" to "2y"
            "1M" -> "1mo" to "5y"
            else -> "1d" to "6mo" // 1D
        }

        fun intervalMillis(interval: String): Long = when (interval) {
            "1m" -> 60_000L
            "5m" -> 300_000L
            "15m" -> 900_000L
            "1h" -> 3_600_000L
            "1W" -> 604_800_000L
            "1M" -> 2_592_000_000L
            else -> 86_400_000L
        }
    }

    private data class ChartEnvelope(val chart: Chart?)
    private data class Chart(val result: List<Result>?)
    private data class Result(
        val meta: Meta?,
        val timestamp: List<Long>?,
        val indicators: Indicators?
    )
    private data class Meta(
        @SerializedName("regularMarketPrice") val regularMarketPrice: Double?,
        @SerializedName("chartPreviousClose") val chartPreviousClose: Double?,
        @SerializedName("previousClose") val previousClose: Double?,
        @SerializedName("regularMarketDayHigh") val regularMarketDayHigh: Double?,
        @SerializedName("regularMarketDayLow") val regularMarketDayLow: Double?,
        @SerializedName("regularMarketVolume") val regularMarketVolume: Long?
    )
    private data class Indicators(val quote: List<Quote>?)
    private data class Quote(
        val open: List<Double?>?,
        val high: List<Double?>?,
        val low: List<Double?>?,
        val close: List<Double?>?,
        val volume: List<Long?>?
    )
}
