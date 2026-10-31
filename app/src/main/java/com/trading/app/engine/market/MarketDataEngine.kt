package com.trading.app.engine.market

import com.trading.app.engine.core.MoneyMath
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Random

data class Candle(
    val timestampMs: Long,
    val open: BigDecimal,
    val high: BigDecimal,
    val low: BigDecimal,
    val close: BigDecimal,
    val volume: Long
)

data class OrderBookLevel(
    val price: BigDecimal,
    val quantity: BigDecimal,
    val orderCount: Int
)

data class OrderBook(
    val bids: List<OrderBookLevel>,
    val asks: List<OrderBookLevel>,
    val spread: BigDecimal,
    val spreadBps: Double
)

class MarketDataEngine(
    private val seed: Long = 42L,
    private val symbol: String = "AAPL",
    startingPrice: BigDecimal = BigDecimal("150.00"),
    private val annualVolatility: Double = 0.28,
    private val spreadBps: Double = 5.0
) {
    private val rng = Random(seed)
    var currentPrice: BigDecimal = startingPrice
        private set

    var bestBid: BigDecimal = BigDecimal.ZERO
        private set
    var bestAsk: BigDecimal = BigDecimal.ZERO
        private set

    private val _candles = mutableListOf<Candle>()
    val candles: List<Candle> get() = _candles.toList()

    private var currentCandleOpen: BigDecimal = startingPrice
    private var currentCandleHigh: BigDecimal = startingPrice
    private var currentCandleLow: BigDecimal = startingPrice
    private var currentCandleVolume: Long = 0L
    private var lastCandleTimeMs: Long = 0L

    init {
        updateBidAsk()
        generateHistoricalCandles(count = 25)
    }

    private fun updateBidAsk() {
        val halfSpread = currentPrice.multiply(BigDecimal.valueOf(spreadBps / 20000.0))
        bestBid = currentPrice.subtract(halfSpread).setScale(2, RoundingMode.HALF_DOWN)
        bestAsk = currentPrice.add(halfSpread).setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * Pre-populates realistic historical candles for chart viewing.
     */
    fun generateHistoricalCandles(count: Int) {
        _candles.clear()
        var price = currentPrice.multiply(BigDecimal.valueOf(0.92))
        var time = System.currentTimeMillis() - (count * 60_000L)

        for (i in 0 until count) {
            val open = price
            val shock = (rng.nextGaussian() * (annualVolatility / Math.sqrt(252.0 * 390.0))).toFloat()
            val close = open.multiply(BigDecimal.valueOf(1.0 + shock)).setScale(2, RoundingMode.HALF_UP)
            val high = maxOf(open, close).multiply(BigDecimal.valueOf(1.0 + (rng.nextDouble() * 0.004))).setScale(2, RoundingMode.HALF_UP)
            val low = minOf(open, close).multiply(BigDecimal.valueOf(1.0 - (rng.nextDouble() * 0.004))).setScale(2, RoundingMode.HALF_UP)
            val vol = (500L + rng.nextInt(2500))

            _candles.add(Candle(time, open, high, low, close, vol))
            price = close
            time += 60_000L
        }
        currentPrice = price
        updateBidAsk()
    }

    /**
     * Advances price by deltaTime using Geometric Brownian Motion with drift and volatility.
     */
    fun tick(deltaSeconds: Double): BigDecimal {
        val dtYears = deltaSeconds / (365.25 * 24.0 * 3600.0)
        val drift = 0.05 * dtYears // 5% expected annual return
        val randomShock = rng.nextGaussian() * annualVolatility * Math.sqrt(dtYears)
        val priceMultiplier = Math.exp(drift + randomShock)

        val newPrice = currentPrice.multiply(BigDecimal.valueOf(priceMultiplier)).setScale(2, RoundingMode.HALF_UP)
        currentPrice = newPrice
        updateBidAsk()

        // Update live candle
        currentCandleHigh = maxOf(currentCandleHigh, newPrice)
        currentCandleLow = minOf(currentCandleLow, newPrice)
        currentCandleVolume += (10L + rng.nextInt(50))

        val now = System.currentTimeMillis()
        if (now - lastCandleTimeMs > 60_000L) {
            _candles.add(
                Candle(
                    timestampMs = now,
                    open = currentCandleOpen,
                    high = currentCandleHigh,
                    low = currentCandleLow,
                    close = newPrice,
                    volume = currentCandleVolume
                )
            )
            if (_candles.size > 50) _candles.removeAt(0)
            currentCandleOpen = newPrice
            currentCandleHigh = newPrice
            currentCandleLow = newPrice
            currentCandleVolume = 0L
            lastCandleTimeMs = now
        }

        return currentPrice
    }

    fun applyShock(shockPct: Double) {
        val multiplier = 1.0 + (shockPct / 100.0)
        currentPrice = currentPrice.multiply(BigDecimal.valueOf(multiplier)).setScale(2, RoundingMode.HALF_UP)
        updateBidAsk()
    }

    fun getOrderBook(depthLevels: Int = 5): OrderBook {
        val bids = mutableListOf<OrderBookLevel>()
        val asks = mutableListOf<OrderBookLevel>()

        for (i in 0 until depthLevels) {
            val bidStep = BigDecimal.valueOf(0.05 * (i + 1))
            val askStep = BigDecimal.valueOf(0.05 * (i + 1))

            bids.add(
                OrderBookLevel(
                    price = bestBid.subtract(bidStep).setScale(2, RoundingMode.HALF_DOWN),
                    quantity = BigDecimal.valueOf(100.0 + (i * 75.0) + rng.nextInt(50)),
                    orderCount = 2 + i + rng.nextInt(3)
                )
            )
            asks.add(
                OrderBookLevel(
                    price = bestAsk.add(askStep).setScale(2, RoundingMode.HALF_UP),
                    quantity = BigDecimal.valueOf(100.0 + (i * 75.0) + rng.nextInt(50)),
                    orderCount = 2 + i + rng.nextInt(3)
                )
            )
        }

        val spread = bestAsk.subtract(bestBid).setScale(2, RoundingMode.HALF_UP)
        val spreadBps = if (currentPrice > BigDecimal.ZERO) {
            spread.toDouble() / currentPrice.toDouble() * 10000.0
        } else 0.0

        return OrderBook(bids, asks, spread, spreadBps)
    }
}
