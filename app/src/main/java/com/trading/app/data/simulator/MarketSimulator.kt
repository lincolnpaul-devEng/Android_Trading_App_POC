package com.trading.app.data.simulator

import com.trading.app.data.DataSourceConfig
import com.trading.app.domain.model.*
import com.trading.app.notification.NotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*
import kotlin.random.Random

@Singleton
class MarketSimulator {

    private val notificationHelper: NotificationHelper?

    @Inject
    constructor(notificationHelper: NotificationHelper) {
        this.notificationHelper = notificationHelper
    }

    constructor() {
        this.notificationHelper = null
    }

    private val javaRandom = java.util.Random()
    private var lastMarginWarningTime = 0L
    private val positionMilestones = ConcurrentHashMap<String, Int>()

    private val stocks = mapOf(
        // Equities
        "AAPL" to StockSeed("Apple Inc.", 189.50, 2_980_000_000_000),
        "GOOGL" to StockSeed("Alphabet Inc.", 141.80, 1_780_000_000_000),
        "MSFT" to StockSeed("Microsoft Corp.", 378.90, 2_810_000_000_000),
        "AMZN" to StockSeed("Amazon.com Inc.", 178.25, 1_860_000_000_000),
        "TSLA" to StockSeed("Tesla Inc.", 248.50, 790_000_000_000),
        "NVDA" to StockSeed("NVIDIA Corp.", 875.30, 2_150_000_000_000),
        "META" to StockSeed("Meta Platforms Inc.", 505.75, 1_290_000_000_000),
        "JPM" to StockSeed("JPMorgan Chase & Co.", 195.40, 565_000_000_000),
        "V" to StockSeed("Visa Inc.", 279.60, 575_000_000_000),
        "JNJ" to StockSeed("Johnson & Johnson", 156.80, 378_000_000_000),
        "WMT" to StockSeed("Walmart Inc.", 165.20, 445_000_000_000),
        "BAC" to StockSeed("Bank of America Corp.", 35.80, 280_000_000_000),
        "DIS" to StockSeed("The Walt Disney Co.", 112.40, 205_000_000_000),
        "NFLX" to StockSeed("Netflix Inc.", 628.90, 272_000_000_000),
        "AMD" to StockSeed("Advanced Micro Devices", 178.60, 288_000_000_000),

        // Forex — Majors
        "EUR/USD" to StockSeed("Euro / US Dollar", 1.0854, 0L, InstrumentSpecs(
            assetClass = AssetClass.FOREX,
            unitOfMeasure = "Lots",
            pipSize = 0.0001,
            tickSize = 0.00001,
            standardLotUnits = 100_000.0,
            overnightSwapLong = -0.0065,
            overnightSwapShort = +0.0025,
            maxLeverage = 30,
            subCategory = "Major"
        )),
        "GBP/USD" to StockSeed("British Pound / US Dollar", 1.2982, 0L, InstrumentSpecs(
            assetClass = AssetClass.FOREX,
            unitOfMeasure = "Lots",
            pipSize = 0.0001,
            tickSize = 0.00001,
            standardLotUnits = 100_000.0,
            overnightSwapLong = -0.0050,
            overnightSwapShort = +0.0015,
            maxLeverage = 30,
            subCategory = "Major"
        )),
        "USD/JPY" to StockSeed("US Dollar / Japanese Yen", 152.38, 0L, InstrumentSpecs(
            assetClass = AssetClass.FOREX,
            unitOfMeasure = "Lots",
            pipSize = 0.01,
            tickSize = 0.001,
            standardLotUnits = 100_000.0,
            overnightSwapLong = +0.0120,
            overnightSwapShort = -0.0180,
            maxLeverage = 30,
            subCategory = "Major"
        )),
        "USD/CHF" to StockSeed("US Dollar / Swiss Franc", 0.8654, 0L, InstrumentSpecs(
            assetClass = AssetClass.FOREX,
            unitOfMeasure = "Lots",
            pipSize = 0.0001,
            tickSize = 0.00001,
            standardLotUnits = 100_000.0,
            overnightSwapLong = +0.0045,
            overnightSwapShort = -0.0095,
            maxLeverage = 30,
            subCategory = "Major"
        )),
        "AUD/USD" to StockSeed("Australian Dollar / US Dollar", 0.6642, 0L, InstrumentSpecs(
            assetClass = AssetClass.FOREX,
            unitOfMeasure = "Lots",
            pipSize = 0.0001,
            tickSize = 0.00001,
            standardLotUnits = 100_000.0,
            overnightSwapLong = -0.0035,
            overnightSwapShort = +0.0010,
            maxLeverage = 30,
            subCategory = "Major"
        )),
        "USD/CAD" to StockSeed("US Dollar / Canadian Dollar", 1.3745, 0L, InstrumentSpecs(
            assetClass = AssetClass.FOREX,
            unitOfMeasure = "Lots",
            pipSize = 0.0001,
            tickSize = 0.00001,
            standardLotUnits = 100_000.0,
            overnightSwapLong = +0.0020,
            overnightSwapShort = -0.0060,
            maxLeverage = 30,
            subCategory = "Major"
        )),
        "NZD/USD" to StockSeed("New Zealand Dollar / US Dollar", 0.6048, 0L, InstrumentSpecs(
            assetClass = AssetClass.FOREX,
            unitOfMeasure = "Lots",
            pipSize = 0.0001,
            tickSize = 0.00001,
            standardLotUnits = 100_000.0,
            overnightSwapLong = -0.0030,
            overnightSwapShort = +0.0008,
            maxLeverage = 30,
            subCategory = "Major"
        )),

        // Forex — Minors (Crosses)
        "EUR/GBP" to StockSeed("Euro / British Pound", 0.8361, 0L, InstrumentSpecs(
            assetClass = AssetClass.FOREX,
            unitOfMeasure = "Lots",
            pipSize = 0.0001,
            tickSize = 0.00001,
            standardLotUnits = 100_000.0,
            overnightSwapLong = -0.0040,
            overnightSwapShort = +0.0010,
            maxLeverage = 20,
            subCategory = "Minor"
        )),
        "EUR/JPY" to StockSeed("Euro / Japanese Yen", 165.38, 0L, InstrumentSpecs(
            assetClass = AssetClass.FOREX,
            unitOfMeasure = "Lots",
            pipSize = 0.01,
            tickSize = 0.001,
            standardLotUnits = 100_000.0,
            overnightSwapLong = +0.0085,
            overnightSwapShort = -0.0145,
            maxLeverage = 20,
            subCategory = "Minor"
        )),
        "GBP/JPY" to StockSeed("British Pound / Japanese Yen", 197.80, 0L, InstrumentSpecs(
            assetClass = AssetClass.FOREX,
            unitOfMeasure = "Lots",
            pipSize = 0.01,
            tickSize = 0.001,
            standardLotUnits = 100_000.0,
            overnightSwapLong = +0.0110,
            overnightSwapShort = -0.0175,
            maxLeverage = 20,
            subCategory = "Minor"
        )),
        "AUD/JPY" to StockSeed("Australian Dollar / Japanese Yen", 101.20, 0L, InstrumentSpecs(
            assetClass = AssetClass.FOREX,
            unitOfMeasure = "Lots",
            pipSize = 0.01,
            tickSize = 0.001,
            standardLotUnits = 100_000.0,
            overnightSwapLong = +0.0075,
            overnightSwapShort = -0.0130,
            maxLeverage = 20,
            subCategory = "Minor"
        )),
        "EUR/CHF" to StockSeed("Euro / Swiss Franc", 0.9390, 0L, InstrumentSpecs(
            assetClass = AssetClass.FOREX,
            unitOfMeasure = "Lots",
            pipSize = 0.0001,
            tickSize = 0.00001,
            standardLotUnits = 100_000.0,
            overnightSwapLong = +0.0025,
            overnightSwapShort = -0.0065,
            maxLeverage = 20,
            subCategory = "Minor"
        )),

        // Forex — Exotics
        "USD/TRY" to StockSeed("US Dollar / Turkish Lira (Exotic)", 34.28, 0L, InstrumentSpecs(
            assetClass = AssetClass.FOREX,
            unitOfMeasure = "Lots",
            pipSize = 0.01,
            tickSize = 0.001,
            standardLotUnits = 100_000.0,
            overnightSwapLong = +0.2500,
            overnightSwapShort = -0.3800,
            maxLeverage = 10,
            subCategory = "Exotic"
        )),
        "USD/ZAR" to StockSeed("US Dollar / South African Rand (Exotic)", 17.85, 0L, InstrumentSpecs(
            assetClass = AssetClass.FOREX,
            unitOfMeasure = "Lots",
            pipSize = 0.01,
            tickSize = 0.001,
            standardLotUnits = 100_000.0,
            overnightSwapLong = -0.0450,
            overnightSwapShort = +0.0180,
            maxLeverage = 10,
            subCategory = "Exotic"
        )),
        "USD/MXN" to StockSeed("US Dollar / Mexican Peso (Exotic)", 19.92, 0L, InstrumentSpecs(
            assetClass = AssetClass.FOREX,
            unitOfMeasure = "Lots",
            pipSize = 0.01,
            tickSize = 0.001,
            standardLotUnits = 100_000.0,
            overnightSwapLong = -0.0520,
            overnightSwapShort = +0.0210,
            maxLeverage = 10,
            subCategory = "Exotic"
        )),
        "USD/SGD" to StockSeed("US Dollar / Singapore Dollar (Exotic)", 1.3215, 0L, InstrumentSpecs(
            assetClass = AssetClass.FOREX,
            unitOfMeasure = "Lots",
            pipSize = 0.0001,
            tickSize = 0.00001,
            standardLotUnits = 100_000.0,
            overnightSwapLong = +0.0015,
            overnightSwapShort = -0.0045,
            maxLeverage = 10,
            subCategory = "Exotic"
        )),

        // Stock Market Indices (Tradable)
        "SPX" to StockSeed("S&P 500 Index", 5_821.84, 0L, InstrumentSpecs(
            assetClass = AssetClass.INDEX,
            unitOfMeasure = "Index Pts",
            pipSize = 0.1,
            tickSize = 0.01,
            overnightSwapLong = -0.0150,
            overnightSwapShort = +0.0040,
            maxLeverage = 20
        )),
        "IXIC" to StockSeed("NASDAQ Composite Index", 18_496.82, 0L, InstrumentSpecs(
            assetClass = AssetClass.INDEX,
            unitOfMeasure = "Index Pts",
            pipSize = 0.1,
            tickSize = 0.01,
            overnightSwapLong = -0.0160,
            overnightSwapShort = +0.0040,
            maxLeverage = 20
        )),
        "DJI" to StockSeed("Dow Jones Industrial Average", 42_131.53, 0L, InstrumentSpecs(
            assetClass = AssetClass.INDEX,
            unitOfMeasure = "Index Pts",
            pipSize = 1.0,
            tickSize = 0.1,
            overnightSwapLong = -0.0150,
            overnightSwapShort = +0.0040,
            maxLeverage = 20
        )),
        "UK100" to StockSeed("UK FTSE 100 Index", 8_245.50, 0L, InstrumentSpecs(
            assetClass = AssetClass.INDEX,
            unitOfMeasure = "Index Pts",
            pipSize = 0.1,
            tickSize = 0.1,
            overnightSwapLong = -0.0140,
            overnightSwapShort = +0.0035,
            maxLeverage = 20
        )),
        "JP225" to StockSeed("Japan Nikkei 225 Index", 38_920.0, 0L, InstrumentSpecs(
            assetClass = AssetClass.INDEX,
            unitOfMeasure = "Index Pts",
            pipSize = 1.0,
            tickSize = 1.0,
            overnightSwapLong = -0.0120,
            overnightSwapShort = +0.0030,
            maxLeverage = 20
        )),

        // Cryptocurrencies
        "BTC/USD" to StockSeed("Bitcoin Perpetual", 67840.0, 1_340_000_000_000L, InstrumentSpecs(
            assetClass = AssetClass.CRYPTO,
            unitOfMeasure = "BTC",
            pipSize = 1.0,
            tickSize = 0.1,
            fundingRate24h = +0.0115,
            maxLeverage = 50
        )),
        "ETH/USD" to StockSeed("Ethereum Perpetual", 2518.50, 303_000_000_000L, InstrumentSpecs(
            assetClass = AssetClass.CRYPTO,
            unitOfMeasure = "ETH",
            pipSize = 0.1,
            tickSize = 0.01,
            fundingRate24h = +0.0085,
            maxLeverage = 50
        )),
        "SOL/USD" to StockSeed("Solana Perpetual", 176.40, 82_000_000_000L, InstrumentSpecs(
            assetClass = AssetClass.CRYPTO,
            unitOfMeasure = "SOL",
            pipSize = 0.01,
            tickSize = 0.001,
            fundingRate24h = +0.0142,
            maxLeverage = 20
        )),
        "XRP/USD" to StockSeed("Ripple XRP Perpetual", 0.5840, 33_000_000_000L, InstrumentSpecs(
            assetClass = AssetClass.CRYPTO,
            unitOfMeasure = "XRP",
            pipSize = 0.0001,
            tickSize = 0.0001,
            fundingRate24h = +0.0095,
            maxLeverage = 20
        )),
        "DOGE/USD" to StockSeed("Dogecoin Perpetual", 0.1485, 21_000_000_000L, InstrumentSpecs(
            assetClass = AssetClass.CRYPTO,
            unitOfMeasure = "DOGE",
            pipSize = 0.0001,
            tickSize = 0.00001,
            fundingRate24h = +0.0120,
            maxLeverage = 20
        )),
        "PENGU/USD" to StockSeed("Pudgy Penguins Perpetual", 0.0325, 2_050_000_000L, InstrumentSpecs(
            assetClass = AssetClass.CRYPTO,
            unitOfMeasure = "PENGU",
            pipSize = 0.0001,
            tickSize = 0.00001,
            fundingRate24h = +0.0150,
            maxLeverage = 20
        )),
        "LILPENGU/USD" to StockSeed("Lil Pengu Index Token", 0.1840, 420_000_000L, InstrumentSpecs(
            assetClass = AssetClass.CRYPTO,
            unitOfMeasure = "LILPENGU",
            pipSize = 0.0001,
            tickSize = 0.00001,
            fundingRate24h = +0.0135,
            maxLeverage = 20
        )),

        // Commodities (Gold, Silver, Oil, Natural Gas, Copper)
        "XAU/USD" to StockSeed("Gold Spot (1 troy oz)", 2748.20, 0L, InstrumentSpecs(
            assetClass = AssetClass.COMMODITY,
            unitOfMeasure = "oz",
            pipSize = 0.1,
            tickSize = 0.01,
            overnightSwapLong = -0.012,
            overnightSwapShort = +0.004,
            maxLeverage = 20
        )),
        "XAG/USD" to StockSeed("Silver Spot (1 troy oz)", 33.75, 0L, InstrumentSpecs(
            assetClass = AssetClass.COMMODITY,
            unitOfMeasure = "oz",
            pipSize = 0.01,
            tickSize = 0.001,
            overnightSwapLong = -0.015,
            overnightSwapShort = +0.003,
            maxLeverage = 20
        )),
        "BRENT" to StockSeed("Brent Crude Oil (1 bbl)", 74.35, 0L, InstrumentSpecs(
            assetClass = AssetClass.COMMODITY,
            unitOfMeasure = "bbl",
            pipSize = 0.01,
            tickSize = 0.01,
            overnightSwapLong = -0.008,
            overnightSwapShort = +0.002,
            maxLeverage = 10
        )),
        "WTI" to StockSeed("WTI Crude Oil Spot (1 bbl)", 71.15, 0L, InstrumentSpecs(
            assetClass = AssetClass.COMMODITY,
            unitOfMeasure = "bbl",
            pipSize = 0.01,
            tickSize = 0.01,
            overnightSwapLong = -0.008,
            overnightSwapShort = +0.002,
            maxLeverage = 10
        )),
        "NATGAS" to StockSeed("Henry Hub Natural Gas (MMBtu)", 2.68, 0L, InstrumentSpecs(
            assetClass = AssetClass.COMMODITY,
            unitOfMeasure = "MMBtu",
            pipSize = 0.001,
            tickSize = 0.001,
            overnightSwapLong = -0.014,
            overnightSwapShort = +0.003,
            maxLeverage = 10
        )),
        "COPPER" to StockSeed("High Grade Copper (lb)", 4.38, 0L, InstrumentSpecs(
            assetClass = AssetClass.COMMODITY,
            unitOfMeasure = "lb",
            pipSize = 0.001,
            tickSize = 0.0005,
            overnightSwapLong = -0.010,
            overnightSwapShort = +0.002,
            maxLeverage = 10
        )),

        // Futures
        "ESZ6" to StockSeed("E-mini S&P 500 Futures", 5824.50, 0L, InstrumentSpecs(
            assetClass = AssetClass.FUTURES,
            unitOfMeasure = "Contracts",
            contractMultiplier = 50.0,
            pipSize = 0.25,
            tickSize = 0.25,
            expiryDate = "DEC 2026",
            maxLeverage = 20
        )),
        "NQZ6" to StockSeed("E-mini Nasdaq 100 Futures", 20480.0, 0L, InstrumentSpecs(
            assetClass = AssetClass.FUTURES,
            unitOfMeasure = "Contracts",
            contractMultiplier = 20.0,
            pipSize = 0.25,
            tickSize = 0.25,
            expiryDate = "DEC 2026",
            maxLeverage = 20
        )),
        "CLZ6" to StockSeed("Crude Oil WTI Futures", 71.90, 0L, InstrumentSpecs(
            assetClass = AssetClass.FUTURES,
            unitOfMeasure = "Contracts",
            contractMultiplier = 1000.0,
            pipSize = 0.01,
            tickSize = 0.01,
            expiryDate = "DEC 2026",
            maxLeverage = 10
        )),
        "GCZ6" to StockSeed("COMEX Gold Futures", 2758.40, 0L, InstrumentSpecs(
            assetClass = AssetClass.FUTURES,
            unitOfMeasure = "Contracts",
            contractMultiplier = 100.0,
            pipSize = 0.10,
            tickSize = 0.10,
            expiryDate = "DEC 2026",
            maxLeverage = 20
        )),

        // CFDs
        "US500.CFD" to StockSeed("US 500 Index CFD", 5818.0, 0L, InstrumentSpecs(
            assetClass = AssetClass.CFD,
            unitOfMeasure = "Contracts",
            contractMultiplier = 1.0,
            overnightSwapLong = -0.0185,
            overnightSwapShort = +0.0045,
            maxLeverage = 20
        )),
        "GER40.CFD" to StockSeed("Germany 40 Index CFD", 19460.0, 0L, InstrumentSpecs(
            assetClass = AssetClass.CFD,
            unitOfMeasure = "Contracts",
            contractMultiplier = 1.0,
            overnightSwapLong = -0.0210,
            overnightSwapShort = +0.0035,
            maxLeverage = 20
        )),
        "AAPL.CFD" to StockSeed("Apple Inc. Equity CFD", 189.55, 0L, InstrumentSpecs(
            assetClass = AssetClass.CFD,
            unitOfMeasure = "Contracts",
            contractMultiplier = 1.0,
            overnightSwapLong = -0.0165,
            overnightSwapShort = +0.0035,
            maxLeverage = 10
        )),
        "NVDA.CFD" to StockSeed("NVIDIA Corp. Equity CFD", 875.50, 0L, InstrumentSpecs(
            assetClass = AssetClass.CFD,
            unitOfMeasure = "Contracts",
            contractMultiplier = 1.0,
            overnightSwapLong = -0.0180,
            overnightSwapShort = +0.0040,
            maxLeverage = 10
        ))
    )

    private val currentPrices = ConcurrentHashMap<String, Double>().apply {
        stocks.forEach { (symbol, seed) -> put(symbol, seed.basePrice) }
    }

    private val openPrices = ConcurrentHashMap<String, Double>().apply {
        stocks.forEach { (symbol, seed) -> put(symbol, seed.basePrice * (1 + Random.nextDouble(-0.01, 0.01))) }
    }

    private val highPrices = ConcurrentHashMap<String, Double>()
    private val lowPrices = ConcurrentHashMap<String, Double>()
    private val priceSources = ConcurrentHashMap<String, PriceSource>()

    private val indices = listOf(
        MarketIndex("S&P 500", "SPX", 5_821.84, 0.0, 0.0),
        MarketIndex("NASDAQ", "IXIC", 18_496.82, 0.0, 0.0),
        MarketIndex("DOW", "DJI", 42_131.53, 0.0, 0.0),
    )

    private val sectors = listOf(
        "Technology", "Healthcare", "Financials", "Consumer Discretionary",
        "Communication Services", "Industrials", "Consumer Staples",
        "Energy", "Utilities", "Real Estate", "Materials"
    )

    // --- Portfolio & Account State ---
    private var cashBalance = 100_000.0
    private var initialDeposit = 100_000.0
    private val positions = ConcurrentHashMap<String, MutablePosition>()
    private val orders = CopyOnWriteArrayList<Order>()
    private val closedTrades = CopyOnWriteArrayList<ClosedTrade>()
    private val transactions = CopyOnWriteArrayList<Transaction>()
    private val equityHistory = CopyOnWriteArrayList<EquityPoint>()
    private val priceAlerts = CopyOnWriteArrayList<PriceAlert>()
    private var orderIdCounter = 1
    private var tradeIdCounter = 1
    private var txIdCounter = 1
    private var alertIdCounter = 1
    private val accountCreatedAt = System.currentTimeMillis() - 86_400_000L * 7

    init {
        if (!loadPersistedState()) {
            val isAndroidApp = runCatching { com.trading.app.TradingApplication.instance != null }.getOrDefault(false)
            seedStarterDemoAccount(100_000.0, seedPositions = isAndroidApp)
        }
    }

    private fun seedStarterDemoAccount(startingBalance: Double, seedPositions: Boolean = false) {
        positions.clear()
        orders.clear()
        closedTrades.clear()
        transactions.clear()
        equityHistory.clear()
        cashBalance = startingBalance
        initialDeposit = startingBalance

        recordTransaction(
            type = TransactionType.DEPOSIT,
            amount = startingBalance,
            description = "Initial Demo Account Funding ($${String.format("%,.2f", startingBalance)})",
            reference = "INIT-DEMO-001",
            timestamp = accountCreatedAt
        )

        // Seed 3 realistic starter positions in Demo mode when running in Android app
        if (seedPositions && startingBalance >= 10_000.0) {
            val penguSeed = stocks["PENGU/USD"]
            val lilSeed = stocks["LILPENGU/USD"]
            val solSeed = stocks["SOL/USD"]
            if (penguSeed != null && lilSeed != null && solSeed != null) {
                val pQty = 40_000 // ~$1,300 notional at $0.0325
                val pEntry = 0.0312
                cashBalance -= (pQty * pEntry) / 2
                positions["PENGU/USD"] = MutablePosition(
                    symbol = "PENGU/USD",
                    companyName = penguSeed.name,
                    quantity = pQty,
                    averageCost = pEntry,
                    side = PositionSide.LONG,
                    leverage = 2,
                    takeProfitPrice = 0.0420,
                    stopLossPrice = 0.0265,
                    specs = penguSeed.specs,
                    openedAt = System.currentTimeMillis() - 7_200_000L
                )

                val lQty = 5_000 // ~$920 notional at $0.1840
                val lEntry = 0.1750
                cashBalance -= (lQty * lEntry) / 2
                positions["LILPENGU/USD"] = MutablePosition(
                    symbol = "LILPENGU/USD",
                    companyName = lilSeed.name,
                    quantity = lQty,
                    averageCost = lEntry,
                    side = PositionSide.LONG,
                    leverage = 2,
                    takeProfitPrice = 0.2250,
                    stopLossPrice = 0.1480,
                    specs = lilSeed.specs,
                    openedAt = System.currentTimeMillis() - 5_400_000L
                )

                val sQty = 20 // ~$3,528 notional at $176.40 (5x leverage -> ~$705 margin)
                val sEntry = 172.80
                cashBalance -= (sQty * sEntry) / 5
                positions["SOL/USD"] = MutablePosition(
                    symbol = "SOL/USD",
                    companyName = solSeed.name,
                    quantity = sQty,
                    averageCost = sEntry,
                    side = PositionSide.LONG,
                    leverage = 5,
                    takeProfitPrice = 195.00,
                    stopLossPrice = 162.00,
                    specs = solSeed.specs,
                    openedAt = System.currentTimeMillis() - 3_600_000L
                )
            }
        }

        // Seed initial equity history points leading up to current equity
        val now = System.currentTimeMillis()
        val curEq = getPortfolio().equity
        for (i in 24 downTo 1) {
            val stepEq = startingBalance + (curEq - startingBalance) * ((24 - i) / 24.0) +
                Random.nextDouble(-startingBalance * 0.0012, startingBalance * 0.0014)
            equityHistory.add(EquityPoint(now - i * 3_600_000L, stepEq))
        }
        equityHistory.add(EquityPoint(now, curEq))
        persistState()
    }

    data class StockSeed(
        val name: String,
        val basePrice: Double,
        val marketCap: Long,
        val specs: InstrumentSpecs = InstrumentSpecs()
    )

    data class MutablePosition(
        val symbol: String,
        val companyName: String,
        var quantity: Int,
        var averageCost: Double,
        var side: PositionSide = PositionSide.LONG,
        var leverage: Int = 1,
        var takeProfitPrice: Double? = null,
        var stopLossPrice: Double? = null,
        var specs: InstrumentSpecs = InstrumentSpecs(),
        val openedAt: Long = System.currentTimeMillis()
    )

    /**
     * Synchronizes a live market tick (from Finnhub REST or WebSocket) into the trading engine
     * so open positions, P&L, Stop-Loss/Take-Profit, and Margin Stop-Outs react to real market prices.
     */
    fun updateLivePrice(
        symbol: String,
        price: Double,
        open: Double? = null,
        high: Double? = null,
        low: Double? = null,
        source: PriceSource = PriceSource.LIVE
    ) {
        if (price <= 0.0) return
        currentPrices[symbol] = price
        open?.takeIf { it > 0 }?.let { openPrices[symbol] = it }
        high?.takeIf { it > 0 }?.let { highPrices[symbol] = maxOf(highPrices[symbol] ?: it, price, it) }
        low?.takeIf { it > 0 }?.let { lowPrices[symbol] = minOf(lowPrices[symbol] ?: it, price, it) }
        priceSources[symbol] = source

        checkWorkingOrders(symbol, price)
        checkAutoCloseTriggers(symbol, price)
        checkPriceAlerts(symbol, price)
        checkMarginStopOut()
        checkTradeTrackingMilestones()
        recordEquitySnapshot()
    }

    // ---- Price simulation -----

    private fun simulatePriceMove(symbol: String): Double {
        val current = currentPrices[symbol] ?: stocks[symbol]?.basePrice ?: return 0.0
        val volatility = when (symbol) {
            "TSLA", "NVDA", "AMD", "BTC/USD", "SOL/USD", "DOGE/USD", "PENGU/USD", "LILPENGU/USD" -> 0.0028
            "AAPL", "MSFT", "GOOGL", "ETH/USD" -> 0.0014
            "EUR/USD", "GBP/USD", "USD/JPY", "USD/CHF", "EUR/GBP" -> 0.0005
            else -> 0.0016
        }
        val drift = Random.nextDouble(-0.0001, 0.0002)
        val shock = javaRandom.nextGaussian() * volatility
        val newPrice = current * (1 + drift + shock)
        val clamped = maxOf(newPrice, current * 0.95)  // prevent crash >5%
        currentPrices[symbol] = clamped
        highPrices[symbol] = maxOf(highPrices[symbol] ?: clamped, clamped)
        lowPrices[symbol] = minOf(lowPrices[symbol] ?: clamped, clamped)
        checkWorkingOrders(symbol, clamped)
        checkAutoCloseTriggers(symbol, clamped)
        checkPriceAlerts(symbol, clamped)
        checkMarginStopOut()
        checkTradeTrackingMilestones()
        recordEquitySnapshot()
        return clamped
    }

    private fun checkPriceAlerts(symbol: String, price: Double) {
        var changed = false
        for (i in priceAlerts.indices) {
            val alert = priceAlerts[i]
            if (!alert.isTriggered && alert.symbol == symbol) {
                val hit = when (alert.condition) {
                    AlertCondition.ABOVE -> price >= alert.targetPrice
                    AlertCondition.BELOW -> price <= alert.targetPrice
                }
                if (hit) {
                    priceAlerts[i] = alert.copy(
                        isTriggered = true,
                        triggeredAt = System.currentTimeMillis()
                    )
                    changed = true
                    notificationHelper?.showPriceAlert(
                        symbol = symbol,
                        condition = if (alert.condition == AlertCondition.ABOVE) "above" else "below",
                        targetPrice = alert.targetPrice,
                        currentPrice = price
                    )
                }
            }
        }
        if (changed) persistState()
    }

    private fun checkTradeTrackingMilestones() {
        for ((symbol, pos) in positions) {
            val price = currentPrices[symbol] ?: pos.averageCost
            val pnl = when (pos.side) {
                PositionSide.LONG -> (price - pos.averageCost) * pos.quantity * pos.specs.contractMultiplier
                PositionSide.SHORT -> (pos.averageCost - price) * pos.quantity * pos.specs.contractMultiplier
            }
            val margin = (pos.quantity * pos.averageCost * pos.specs.contractMultiplier) / pos.leverage
            val roePercent = if (margin > 0) (pnl / margin) * 100.0 else 0.0

            val currentTier = when {
                roePercent >= 50.0 -> 3
                roePercent >= 25.0 -> 2
                roePercent >= 10.0 -> 1
                roePercent <= -10.0 -> -1
                else -> 0
            }

            val lastTier = positionMilestones[symbol] ?: 0
            if (currentTier != 0 && currentTier != lastTier) {
                positionMilestones[symbol] = currentTier
                notificationHelper?.showTradeMilestone(symbol, roePercent, pnl)
            }
        }
    }

    private fun recordEquitySnapshot(force: Boolean = false) {
        val now = System.currentTimeMillis()
        val last = equityHistory.lastOrNull()
        if (force || last == null || (now - last.timestamp) >= 15_000L) {
            val eq = getPortfolio().equity
            equityHistory.add(EquityPoint(now, eq))
            while (equityHistory.size > 120) {
                equityHistory.removeAt(0)
            }
        }
    }

    private fun checkWorkingOrders(symbol: String, price: Double) {
        val activeIndices = orders.mapIndexedNotNull { index, ord ->
            if (ord.symbol == symbol && ord.isActive) index else null
        }
        for (idx in activeIndices) {
            val ord = orders[idx]
            val shouldTrigger = when (ord.type) {
                OrderType.LIMIT -> {
                    val limit = ord.price ?: price
                    if (ord.side == OrderSide.BUY) price <= limit else price >= limit
                }
                OrderType.STOP, OrderType.TRAILING_STOP -> {
                    val stop = ord.stopPrice ?: ord.price ?: price
                    if (ord.side == OrderSide.BUY) price >= stop else price <= stop
                }
                OrderType.STOP_LIMIT -> {
                    val stop = ord.stopPrice ?: price
                    val limit = ord.price ?: price
                    if (ord.side == OrderSide.BUY) (price >= stop && price <= limit)
                    else (price <= stop && price >= limit)
                }
                OrderType.MARKET -> true
            }
            if (shouldTrigger) {
                orders.removeAt(idx)
                executeOrderFill(ord, price)
                break
            }
        }
    }

    private fun checkAutoCloseTriggers(symbol: String, price: Double) {
        val pos = positions[symbol] ?: return
        val isTP = when (pos.side) {
            PositionSide.LONG -> pos.takeProfitPrice != null && price >= pos.takeProfitPrice!!
            PositionSide.SHORT -> pos.takeProfitPrice != null && price <= pos.takeProfitPrice!!
        }
        val isSL = when (pos.side) {
            PositionSide.LONG -> pos.stopLossPrice != null && price <= pos.stopLossPrice!!
            PositionSide.SHORT -> pos.stopLossPrice != null && price >= pos.stopLossPrice!!
        }

        if (isTP || isSL) {
            val reason = if (isTP) CloseReason.TAKE_PROFIT else CloseReason.STOP_LOSS
            executePositionClose(pos, pos.quantity, price, reason, orderPrefix = "AUTO")
        }
    }

    private fun checkMarginStopOut() {
        if (positions.isEmpty()) return
        val portfolio = getPortfolio()
        if (portfolio.totalUsedMargin > 0 && portfolio.marginLevel <= 50.0) {
            // Liquidate most underwater position first (standard broker 50% margin stop-out rule)
            val worst = portfolio.positions.minByOrNull { it.unrealizedPnL } ?: return
            val mp = positions[worst.symbol] ?: return
            val price = currentPrices[worst.symbol] ?: mp.averageCost
            executePositionClose(mp, mp.quantity, price, CloseReason.STOP_OUT, orderPrefix = "STOPOUT")
        } else if (portfolio.totalUsedMargin > 0 && portfolio.marginLevel <= 85.0) {
            val now = System.currentTimeMillis()
            if (now - lastMarginWarningTime > 180_000L) {
                lastMarginWarningTime = now
                notificationHelper?.showMarginCallWarning(portfolio.marginLevel)
            }
        }
    }

    private fun executePositionClose(
        pos: MutablePosition,
        closeQty: Int,
        fillPrice: Double,
        reason: CloseReason,
        orderPrefix: String
    ): Order {
        val multiplier = pos.specs.contractMultiplier
        val pnl = when (pos.side) {
            PositionSide.LONG -> (fillPrice - pos.averageCost) * closeQty * multiplier
            PositionSide.SHORT -> (pos.averageCost - fillPrice) * closeQty * multiplier
        }
        val returnedMargin = (closeQty * pos.averageCost * multiplier) / pos.leverage
        cashBalance += (returnedMargin + pnl)

        val closeOrder = Order(
            id = "$orderPrefix-${orderIdCounter++}",
            symbol = pos.symbol,
            side = if (pos.side == PositionSide.LONG) OrderSide.SELL else OrderSide.BUY,
            type = OrderType.MARKET,
            timeInForce = TimeInForce.GTC,
            quantity = closeQty,
            filledQuantity = closeQty,
            status = OrderStatus.FILLED,
            avgFillPrice = fillPrice,
            leverage = pos.leverage,
            positionSide = pos.side,
            specs = pos.specs,
            updatedAt = System.currentTimeMillis()
        )
        orders.add(closeOrder)

        val closedTrade = ClosedTrade(
            ticket = "TRD-${tradeIdCounter++}",
            symbol = pos.symbol,
            name = pos.companyName,
            side = pos.side,
            quantity = closeQty,
            entryPrice = pos.averageCost,
            exitPrice = fillPrice,
            leverage = pos.leverage,
            openedAt = pos.openedAt,
            closedAt = System.currentTimeMillis(),
            realizedPnL = pnl,
            reason = reason,
            assetClass = pos.specs.assetClass
        )
        closedTrades.add(0, closedTrade)

        recordTransaction(
            type = TransactionType.REALIZED_PNL,
            amount = pnl,
            description = "${reason.label}: ${pos.side.name} $closeQty ${pos.symbol} @ ${String.format("%.4f", fillPrice)}",
            reference = closedTrade.ticket
        )

        pos.quantity -= closeQty
        if (pos.quantity <= 0) {
            positions.remove(pos.symbol)
            positionMilestones.remove(pos.symbol)
        }
        recordEquitySnapshot(force = true)
        persistState()

        when (reason) {
            CloseReason.STOP_OUT -> notificationHelper?.showStopOutLiquidationAlert(pos.symbol, closeQty.toDouble())
            CloseReason.TAKE_PROFIT -> notificationHelper?.showTakeProfitTriggered(pos.symbol, pnl, closedTrade.roePercent)
            CloseReason.STOP_LOSS -> notificationHelper?.showStopLossTriggered(pos.symbol, pnl, closedTrade.roePercent)
            else -> {}
        }

        return closeOrder
    }

    private fun recordTransaction(
        type: TransactionType,
        amount: Double,
        description: String,
        reference: String? = null,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val settledBalance = getPortfolio().accountBalance
        transactions.add(
            0,
            Transaction(
                id = "TX-${txIdCounter++}",
                type = type,
                amount = amount,
                balanceAfter = settledBalance,
                description = description,
                timestamp = timestamp,
                reference = reference
            )
        )
    }

    fun getAllSymbols(): List<String> = stocks.keys.toList()

    fun getStockName(symbol: String): String = stocks[symbol]?.name ?: symbol

    fun getInstrumentSpecs(symbol: String): InstrumentSpecs = stocks[symbol]?.specs ?: InstrumentSpecs()

    fun getMarketCap(symbol: String): Long = stocks[symbol]?.marketCap ?: 0L

    /**
     * Computes realistic BID and ASK prices around [midPrice] for [symbol].
     */
    fun computeBidAsk(symbol: String, midPrice: Double): Pair<Double, Double> {
        val specs = stocks[symbol]?.specs ?: InstrumentSpecs()
        val halfSpread = when (specs.assetClass) {
            AssetClass.FOREX -> {
                val pips = when (specs.subCategory) {
                    "Major" -> 0.9
                    "Minor" -> 1.8
                    else -> 8.0
                }
                (specs.pipSize * pips) / 2.0
            }
            AssetClass.INDEX, AssetClass.CFD -> maxOf(specs.pipSize * 1.5, midPrice * 0.00015) / 2.0
            AssetClass.FUTURES -> specs.tickSize / 2.0
            AssetClass.COMMODITY -> maxOf(specs.tickSize * 2.0, midPrice * 0.00025) / 2.0
            AssetClass.CRYPTO -> maxOf(specs.tickSize, midPrice * 0.0003) / 2.0
            AssetClass.EQUITY -> maxOf(0.01, midPrice * 0.0002) / 2.0
        }
        return Pair(maxOf(0.00001, midPrice - halfSpread), midPrice + halfSpread)
    }

    fun generateQuote(symbol: String): StockQuote {
        val price = simulatePriceMove(symbol)
        val seed = stocks[symbol] ?: return StockQuote(symbol, symbol, price, 0.0, 0.0, price, price, price, 0, 0, price, price, 0, 0)
        val open = openPrices[symbol] ?: price
        val change = price - open
        val changePct = if (open != 0.0) (change / open) * 100 else 0.0
        val high = highPrices[symbol] ?: (maxOf(open, price) * (1 + Random.nextDouble(0.0, 0.005)))
        val low = lowPrices[symbol] ?: (minOf(open, price) * (1 - Random.nextDouble(0.0, 0.005)))
        val (bid, ask) = computeBidAsk(symbol, price)
        val source = if (DataSourceConfig.mode == com.trading.app.data.DataSourceMode.SIMULATED) {
            PriceSource.SIMULATED
        } else {
            priceSources[symbol] ?: PriceSource.SIMULATED
        }
        return StockQuote(
            symbol = symbol,
            companyName = seed.name,
            lastPrice = price,
            change = change,
            changePercent = changePct,
            open = open,
            high = maxOf(high, price),
            low = minOf(low, price),
            volume = Random.nextLong(1_000_000, 80_000_000),
            marketCap = seed.marketCap,
            bid = bid,
            ask = ask,
            bidSize = Random.nextInt(100, 5000),
            askSize = Random.nextInt(100, 5000),
            specs = seed.specs,
            source = source
        )
    }

    fun getQuotesByAssetClass(assetClass: AssetClass? = null): List<StockQuote> {
        return stocks.keys
            .map { generateQuote(it) }
            .filter { assetClass == null || it.assetClass == assetClass }
    }

    fun streamQuotesByAssetClass(assetClass: AssetClass? = null): Flow<List<StockQuote>> = flow {
        while (true) {
            emit(getQuotesByAssetClass(assetClass))
            delay(1200)
        }
    }

    fun streamQuote(symbol: String): Flow<StockQuote> = flow {
        while (true) {
            emit(generateQuote(symbol))
            delay(Random.nextLong(500, 1500))
        }
    }

    fun streamQuotes(symbols: List<String>): Flow<List<StockQuote>> = flow {
        while (true) {
            emit(symbols.map { generateQuote(it) })
            delay(1000)
        }
    }

    fun streamIndices(): Flow<List<MarketIndex>> = flow {
        while (true) {
            val result = indices.map { idx ->
                val quote = generateQuote(idx.symbol)
                idx.copy(
                    value = quote.lastPrice,
                    change = quote.change,
                    changePercent = quote.changePercent
                )
            }
            emit(result)
            delay(1800)
        }
    }

    fun streamSectorPerformance(): Flow<List<SectorPerformance>> = flow {
        val currentSectorPerf = sectors.associateWith { Random.nextDouble(-1.8, 2.4) }.toMutableMap()
        while (true) {
            val updated = sectors.map { name ->
                val prev = currentSectorPerf[name] ?: 0.0
                val next = (prev + Random.nextDouble(-0.25, 0.28)).coerceIn(-4.5, 4.8)
                currentSectorPerf[name] = next
                SectorPerformance(name, next)
            }
            emit(updated)
            delay(2500)
        }
    }

    fun generateCandlesticks(symbol: String, interval: String): Flow<List<Candlestick>> = flow {
        val count = when (interval) {
            "1m", "5m" -> 60
            "15m" -> 48
            "1h" -> 24
            "1D" -> 90
            "1W" -> 52
            "1M" -> 24
            else -> 60
        }
        val basePrice = currentPrices[symbol] ?: stocks[symbol]?.basePrice ?: 100.0
        var price = basePrice * 0.95
        val now = System.currentTimeMillis()
        val intervalMs = when (interval) {
            "1m" -> 60_000L
            "5m" -> 300_000L
            "15m" -> 900_000L
            "1h" -> 3_600_000L
            "1D" -> 86_400_000L
            "1W" -> 604_800_000L
            "1M" -> 2_592_000_000L
            else -> 60_000L
        }
        val candles = (0 until count).map { i ->
            val open = price
            val change = price * javaRandom.nextGaussian() * 0.015
            val close = price + change
            val high = maxOf(open, close) + abs(price * javaRandom.nextGaussian() * 0.005)
            val low = minOf(open, close) - abs(price * javaRandom.nextGaussian() * 0.005)
            price = close
            Candlestick(
                timestamp = now - (count - i) * intervalMs,
                open = open,
                high = high,
                low = low,
                close = close,
                volume = Random.nextLong(500_000, 10_000_000)
            )
        }.toMutableList()
        emit(candles.toList())

        // Continuously add / update live candle
        while (true) {
            delay(if (interval == "1m") 2000 else 3500)
            val last = currentPrices[symbol] ?: price
            val change = last * javaRandom.nextGaussian() * 0.004
            val close = last + change
            val high = maxOf(last, close) + abs(last * javaRandom.nextGaussian() * 0.002)
            val low = minOf(last, close) - abs(last * javaRandom.nextGaussian() * 0.002)
            price = close
            val newCandle = Candlestick(
                timestamp = System.currentTimeMillis(),
                open = last, high = high, low = low, close = close,
                volume = Random.nextLong(500_000, 10_000_000)
            )
            candles.add(newCandle)
            if (candles.size > count + 10) candles.removeAt(0)
            emit(candles.toList())
        }
    }

    fun generateOrderBook(symbol: String): Flow<OrderBook> = flow {
        while (true) {
            val mid = currentPrices[symbol] ?: stocks[symbol]?.basePrice ?: 100.0
            val (bestBid, bestAsk) = computeBidAsk(symbol, mid)
            val step = maxOf(stocks[symbol]?.specs?.tickSize ?: 0.01, mid * 0.00015)
            val bids = (0 until 15).map { i ->
                OrderBookEntry(
                    price = bestBid - i * step * Random.nextDouble(0.8, 1.6),
                    quantity = Random.nextInt(100, 10000)
                )
            }.sortedByDescending { it.price }
            val asks = (0 until 15).map { i ->
                OrderBookEntry(
                    price = bestAsk + i * step * Random.nextDouble(0.8, 1.6),
                    quantity = Random.nextInt(100, 10000)
                )
            }.sortedBy { it.price }
            emit(OrderBook(symbol, bids, asks))
            delay(800)
        }
    }

    fun getTopMovers(): Flow<List<StockQuote>> = flow {
        while (true) {
            val quotes = stocks.keys.map { generateQuote(it) }
                .sortedByDescending { abs(it.changePercent) }
                .take(10)
            emit(quotes)
            delay(3000)
        }
    }

    fun searchSymbols(query: String): List<StockQuote> {
        val q = query.uppercase()
        return stocks.filter { (symbol, seed) ->
            symbol.contains(q) || seed.name.uppercase().contains(q) ||
                seed.specs.assetClass.displayName.uppercase().contains(q) ||
                (seed.specs.subCategory?.uppercase()?.contains(q) == true)
        }.map { (symbol, _) -> generateQuote(symbol) }
    }

    // ---- Order, Position Close & Portfolio ----

    /**
     * Explicitly closes an open position (full or partial quantity) at current market Bid/Ask price.
     */
    fun closePosition(symbol: String, quantity: Int? = null): Result<Order> {
        val existing = positions[symbol]
            ?: return Result.failure(Exception("No open position for $symbol"))
        val closeQty = (quantity ?: existing.quantity).coerceIn(1, existing.quantity)
        val midPrice = currentPrices[symbol] ?: existing.averageCost
        val (bid, ask) = computeBidAsk(symbol, midPrice)
        // Closing a LONG sells at Bid; closing a SHORT buys back at Ask
        val fillPrice = if (existing.side == PositionSide.LONG) bid else ask
        val order = executePositionClose(
            pos = existing,
            closeQty = closeQty,
            fillPrice = fillPrice,
            reason = CloseReason.MANUAL,
            orderPrefix = "CLS"
        )
        return Result.success(order)
    }

    /**
     * Reverses an open position in-place (closes existing side and immediately opens opposite side with same quantity & leverage).
     */
    fun reversePosition(symbol: String): Result<Order> {
        val existing = positions[symbol]
            ?: return Result.failure(Exception("No open position for $symbol"))
        val qty = existing.quantity
        val lev = existing.leverage
        val newOrderSide = if (existing.side == PositionSide.LONG) OrderSide.SELL else OrderSide.BUY
        val newPosSide = if (existing.side == PositionSide.LONG) PositionSide.SHORT else PositionSide.LONG
        closePosition(symbol, qty)
        val reverseOrder = Order(
            id = "REV-${orderIdCounter++}",
            symbol = symbol,
            side = newOrderSide,
            type = OrderType.MARKET,
            timeInForce = TimeInForce.GTC,
            quantity = qty,
            leverage = lev,
            positionSide = newPosSide,
            specs = existing.specs
        )
        return placeOrder(reverseOrder)
    }

    /**
     * Updates Stop Loss and Take Profit levels on an existing open position.
     */
    fun updatePositionSlTp(symbol: String, takeProfit: Double?, stopLoss: Double?): Result<Position> {
        val existing = positions[symbol]
            ?: return Result.failure(Exception("No open position for $symbol"))
        existing.takeProfitPrice = takeProfit
        existing.stopLossPrice = stopLoss
        persistState()
        val updated = getPositions().find { it.symbol == symbol }
            ?: return Result.failure(Exception("Position not found"))
        return Result.success(updated)
    }

    fun placeOrder(order: Order): Result<Order> {
        val seed = stocks[order.symbol] ?: return Result.failure(Exception("Unknown symbol"))
        val midPrice = currentPrices[order.symbol] ?: return Result.failure(Exception("No price"))
        val (bid, ask) = computeBidAsk(order.symbol, midPrice)

        // Validation
        if (order.quantity <= 0) return Result.failure(Exception("Quantity must be positive"))

        val marketExecutionPrice = if (order.side == OrderSide.BUY) ask else bid
        val fillPrice = when (order.type) {
            OrderType.MARKET -> marketExecutionPrice
            OrderType.LIMIT -> order.price ?: marketExecutionPrice
            else -> order.stopPrice ?: order.price ?: marketExecutionPrice
        }

        val multiplier = seed.specs.contractMultiplier
        val requiredMargin = (order.quantity * fillPrice * multiplier) / order.leverage

        if (order.side == OrderSide.BUY) {
            if (order.leverage == 1 && order.positionSide == PositionSide.LONG) {
                val cost = order.quantity * (order.price ?: fillPrice) * multiplier
                if (cost > cashBalance) return Result.failure(Exception("Insufficient buying power"))
            } else {
                if (requiredMargin > cashBalance) return Result.failure(Exception("Insufficient buying power"))
            }
        }

        if (order.side == OrderSide.SELL) {
            if (order.leverage == 1 && order.positionSide == PositionSide.LONG) {
                val held = positions[order.symbol]?.quantity ?: 0
                if (order.quantity > held) return Result.failure(Exception("Insufficient shares"))
            } else {
                if (order.positionSide == PositionSide.SHORT && requiredMargin > cashBalance) {
                    return Result.failure(Exception("Insufficient buying power"))
                }
            }
        }

        // Check if this is a non-marketable STOP / LIMIT order that should rest on the order book until triggered
        val isPendingWorkingOrder = when (order.type) {
            OrderType.STOP, OrderType.STOP_LIMIT, OrderType.TRAILING_STOP -> {
                val stop = order.stopPrice
                stop != null && ((order.side == OrderSide.BUY && midPrice < stop) || (order.side == OrderSide.SELL && midPrice > stop))
            }
            OrderType.LIMIT -> {
                val limit = order.price
                limit != null &&
                    ((order.side == OrderSide.BUY && ask > limit) || (order.side == OrderSide.SELL && bid < limit))
            }
            OrderType.MARKET -> false
        }

        if (isPendingWorkingOrder) {
            val workingOrder = order.copy(
                id = "ORD-${orderIdCounter++}",
                status = OrderStatus.NEW,
                filledQuantity = 0,
                avgFillPrice = null,
                specs = seed.specs,
                updatedAt = System.currentTimeMillis()
            )
            orders.add(workingOrder)
            persistState()
            return Result.success(workingOrder)
        }

        return executeOrderFill(order, fillPrice)
    }

    private fun executeOrderFill(order: Order, fillPrice: Double): Result<Order> {
        val seed = stocks[order.symbol] ?: return Result.failure(Exception("Unknown symbol"))
        val multiplier = seed.specs.contractMultiplier
        val requiredMargin = (order.quantity * fillPrice * multiplier) / order.leverage

        val filledOrder = order.copy(
            id = if (order.id.startsWith("ORD-")) order.id else "ORD-${orderIdCounter++}",
            status = OrderStatus.FILLED,
            filledQuantity = order.quantity,
            avgFillPrice = fillPrice,
            specs = seed.specs,
            updatedAt = System.currentTimeMillis()
        )

        val existing = positions[order.symbol]

        if (order.side == OrderSide.BUY) {
            if (existing != null && existing.side == PositionSide.SHORT) {
                val closeQty = minOf(order.quantity, existing.quantity)
                executePositionClose(existing, closeQty, fillPrice, CloseReason.MANUAL, "ORD")
                return Result.success(filledOrder)
            } else {
                cashBalance -= requiredMargin
                if (existing != null && existing.side == PositionSide.LONG) {
                    val totalQty = existing.quantity + order.quantity
                    val totalCost = existing.quantity * existing.averageCost + order.quantity * fillPrice
                    existing.quantity = totalQty
                    existing.averageCost = totalCost / totalQty
                    existing.takeProfitPrice = order.takeProfitPrice ?: existing.takeProfitPrice
                    existing.stopLossPrice = order.stopLossPrice ?: existing.stopLossPrice
                } else {
                    positions[order.symbol] = MutablePosition(
                        symbol = order.symbol,
                        companyName = seed.name,
                        quantity = order.quantity,
                        averageCost = fillPrice,
                        side = PositionSide.LONG,
                        leverage = order.leverage,
                        takeProfitPrice = order.takeProfitPrice,
                        stopLossPrice = order.stopLossPrice,
                        specs = seed.specs
                    )
                }
            }
        } else {
            // SELL
            if (existing != null && existing.side == PositionSide.LONG) {
                val closeQty = minOf(order.quantity, existing.quantity)
                executePositionClose(existing, closeQty, fillPrice, CloseReason.MANUAL, "ORD")
                return Result.success(filledOrder)
            } else {
                // Short position
                cashBalance -= requiredMargin
                if (existing != null && existing.side == PositionSide.SHORT) {
                    val totalQty = existing.quantity + order.quantity
                    val totalCost = existing.quantity * existing.averageCost + order.quantity * fillPrice
                    existing.quantity = totalQty
                    existing.averageCost = totalCost / totalQty
                    existing.takeProfitPrice = order.takeProfitPrice ?: existing.takeProfitPrice
                    existing.stopLossPrice = order.stopLossPrice ?: existing.stopLossPrice
                } else {
                    positions[order.symbol] = MutablePosition(
                        symbol = order.symbol,
                        companyName = seed.name,
                        quantity = order.quantity,
                        averageCost = fillPrice,
                        side = PositionSide.SHORT,
                        leverage = order.leverage,
                        takeProfitPrice = order.takeProfitPrice,
                        stopLossPrice = order.stopLossPrice,
                        specs = seed.specs
                    )
                }
            }
        }

        orders.add(filledOrder)
        notificationHelper?.showOrderFilled(
            symbol = filledOrder.symbol,
            side = filledOrder.side.name,
            quantity = filledOrder.quantity.toDouble(),
            price = fillPrice,
            orderType = filledOrder.type.name.replace("_", " ")
        )
        recordEquitySnapshot(force = true)
        persistState()
        return Result.success(filledOrder)
    }

    fun cancelOrder(orderId: String): Result<Order> {
        val idx = orders.indexOfFirst { it.id == orderId && it.isActive }
        if (idx == -1) return Result.failure(Exception("Order not found or not cancellable"))
        val cancelled = orders[idx].copy(status = OrderStatus.CANCELLED, updatedAt = System.currentTimeMillis())
        orders[idx] = cancelled
        persistState()
        return Result.success(cancelled)
    }

    fun depositFunds(amount: Double): Portfolio {
        if (amount > 0) {
            cashBalance += amount
            recordTransaction(
                type = TransactionType.DEPOSIT,
                amount = amount,
                description = "Account Deposit (+$${String.format("%,.2f", amount)})",
                reference = "DEP-${System.currentTimeMillis() % 100000}"
            )
            recordEquitySnapshot(force = true)
            persistState()
        }
        return getPortfolio()
    }

    fun withdrawFunds(amount: Double): Result<Portfolio> {
        if (amount <= 0) return Result.failure(Exception("Invalid amount"))
        if (amount > cashBalance) return Result.failure(Exception("Insufficient free cash balance"))
        cashBalance -= amount
        recordTransaction(
            type = TransactionType.WITHDRAWAL,
            amount = -amount,
            description = "Account Withdrawal (-$${String.format("%,.2f", amount)})",
            reference = "WDR-${System.currentTimeMillis() % 100000}"
        )
        recordEquitySnapshot(force = true)
        persistState()
        return Result.success(getPortfolio())
    }

    /**
     * Resets the Demo Account to a fresh virtual balance (default $100,000), clearing open positions.
     */
    fun resetDemoAccount(startingBalance: Double = 100_000.0): Portfolio {
        DataSourceConfig.accountType = AccountType.DEMO
        seedStarterDemoAccount(startingBalance)
        return getPortfolio()
    }

    fun addPriceAlert(symbol: String, targetPrice: Double, condition: AlertCondition): PriceAlert {
        val alert = PriceAlert(
            id = "ALT-${alertIdCounter++}",
            symbol = symbol,
            targetPrice = targetPrice,
            condition = condition
        )
        priceAlerts.add(0, alert)
        persistState()
        return alert
    }

    fun removePriceAlert(alertId: String) {
        priceAlerts.removeAll { it.id == alertId }
        persistState()
    }

    fun getOpenOrders(): List<Order> = orders.filter { it.isActive }
    fun getOrderHistory(): List<Order> = orders.toList()
    fun getClosedTrades(): List<ClosedTrade> = closedTrades.toList()
    fun getTransactions(): List<Transaction> = transactions.toList()
    fun getEquityHistory(): List<EquityPoint> = equityHistory.toList()
    fun getPriceAlerts(): List<PriceAlert> = priceAlerts.toList()

    private fun persistState() {
        val app = com.trading.app.TradingApplication.instance ?: return
        try {
            val prefs = app.getSharedPreferences("cash_cage_trading_state", android.content.Context.MODE_PRIVATE)
            val root = org.json.JSONObject()
            root.put("cashBalance", cashBalance)
            root.put("initialDeposit", initialDeposit)
            root.put("orderIdCounter", orderIdCounter)
            root.put("tradeIdCounter", tradeIdCounter)
            root.put("txIdCounter", txIdCounter)
            root.put("alertIdCounter", alertIdCounter)

            val posArr = org.json.JSONArray()
            for (mp in positions.values) {
                val obj = org.json.JSONObject()
                obj.put("symbol", mp.symbol)
                obj.put("companyName", mp.companyName)
                obj.put("quantity", mp.quantity)
                obj.put("averageCost", mp.averageCost)
                obj.put("side", mp.side.name)
                obj.put("leverage", mp.leverage)
                if (mp.takeProfitPrice != null) obj.put("tp", mp.takeProfitPrice!!)
                if (mp.stopLossPrice != null) obj.put("sl", mp.stopLossPrice!!)
                obj.put("openedAt", mp.openedAt)
                posArr.put(obj)
            }
            root.put("positions", posArr)

            val closedArr = org.json.JSONArray()
            for (ct in closedTrades.take(100)) {
                val obj = org.json.JSONObject()
                obj.put("ticket", ct.ticket)
                obj.put("symbol", ct.symbol)
                obj.put("name", ct.name)
                obj.put("side", ct.side.name)
                obj.put("quantity", ct.quantity)
                obj.put("entryPrice", ct.entryPrice)
                obj.put("exitPrice", ct.exitPrice)
                obj.put("leverage", ct.leverage)
                obj.put("openedAt", ct.openedAt)
                obj.put("closedAt", ct.closedAt)
                obj.put("realizedPnL", ct.realizedPnL)
                obj.put("reason", ct.reason.name)
                obj.put("assetClass", ct.assetClass.name)
                closedArr.put(obj)
            }
            root.put("closedTrades", closedArr)

            val ordArr = org.json.JSONArray()
            for (ord in orders.takeLast(100)) {
                val obj = org.json.JSONObject()
                obj.put("id", ord.id)
                obj.put("symbol", ord.symbol)
                obj.put("side", ord.side.name)
                obj.put("type", ord.type.name)
                obj.put("timeInForce", ord.timeInForce.name)
                obj.put("quantity", ord.quantity)
                obj.put("filledQuantity", ord.filledQuantity)
                if (ord.price != null) obj.put("price", ord.price)
                if (ord.stopPrice != null) obj.put("stopPrice", ord.stopPrice)
                obj.put("leverage", ord.leverage)
                obj.put("positionSide", ord.positionSide.name)
                if (ord.takeProfitPrice != null) obj.put("tp", ord.takeProfitPrice)
                if (ord.stopLossPrice != null) obj.put("sl", ord.stopLossPrice)
                obj.put("status", ord.status.name)
                if (ord.avgFillPrice != null) obj.put("avgFillPrice", ord.avgFillPrice)
                obj.put("createdAt", ord.createdAt)
                obj.put("updatedAt", ord.updatedAt)
                ordArr.put(obj)
            }
            root.put("orders", ordArr)

            val txArr = org.json.JSONArray()
            for (tx in transactions.take(100)) {
                val obj = org.json.JSONObject()
                obj.put("id", tx.id)
                obj.put("type", tx.type.name)
                obj.put("amount", tx.amount)
                obj.put("balanceAfter", tx.balanceAfter)
                obj.put("description", tx.description)
                obj.put("timestamp", tx.timestamp)
                if (tx.reference != null) obj.put("reference", tx.reference)
                txArr.put(obj)
            }
            root.put("transactions", txArr)

            val eqArr = org.json.JSONArray()
            for (ep in equityHistory.takeLast(120)) {
                val obj = org.json.JSONObject()
                obj.put("t", ep.timestamp)
                obj.put("e", ep.equity)
                eqArr.put(obj)
            }
            root.put("equityHistory", eqArr)

            val alArr = org.json.JSONArray()
            for (al in priceAlerts.take(50)) {
                val obj = org.json.JSONObject()
                obj.put("id", al.id)
                obj.put("symbol", al.symbol)
                obj.put("targetPrice", al.targetPrice)
                obj.put("condition", al.condition.name)
                obj.put("isTriggered", al.isTriggered)
                obj.put("createdAt", al.createdAt)
                if (al.triggeredAt != null) obj.put("triggeredAt", al.triggeredAt)
                alArr.put(obj)
            }
            root.put("priceAlerts", alArr)

            prefs.edit().putString("state_v2", root.toString()).apply()
        } catch (_: Exception) {
        }
    }

    private fun loadPersistedState(): Boolean {
        val app = com.trading.app.TradingApplication.instance ?: return false
        try {
            val prefs = app.getSharedPreferences("cash_cage_trading_state", android.content.Context.MODE_PRIVATE)
            val raw = prefs.getString("state_v2", null) ?: return false
            val root = org.json.JSONObject(raw)
            cashBalance = root.optDouble("cashBalance", 100_000.0)
            initialDeposit = root.optDouble("initialDeposit", 100_000.0)
            orderIdCounter = root.optInt("orderIdCounter", 1)
            tradeIdCounter = root.optInt("tradeIdCounter", 1)
            txIdCounter = root.optInt("txIdCounter", 1)
            alertIdCounter = root.optInt("alertIdCounter", 1)

            positions.clear()
            val posArr = root.optJSONArray("positions")
            if (posArr != null) {
                for (i in 0 until posArr.length()) {
                    val o = posArr.getJSONObject(i)
                    val sym = o.getString("symbol")
                    val specs = stocks[sym]?.specs ?: InstrumentSpecs()
                    positions[sym] = MutablePosition(
                        symbol = sym,
                        companyName = o.optString("companyName", sym),
                        quantity = o.getInt("quantity"),
                        averageCost = o.getDouble("averageCost"),
                        side = PositionSide.valueOf(o.optString("side", "LONG")),
                        leverage = o.optInt("leverage", 1),
                        takeProfitPrice = if (o.has("tp")) o.getDouble("tp") else null,
                        stopLossPrice = if (o.has("sl")) o.getDouble("sl") else null,
                        specs = specs,
                        openedAt = o.optLong("openedAt", System.currentTimeMillis())
                    )
                }
            }

            closedTrades.clear()
            val closedArr = root.optJSONArray("closedTrades")
            if (closedArr != null) {
                for (i in 0 until closedArr.length()) {
                    val o = closedArr.getJSONObject(i)
                    val sym = o.getString("symbol")
                    val defaultAsset = stocks[sym]?.specs?.assetClass ?: AssetClass.EQUITY
                    closedTrades.add(
                        ClosedTrade(
                            ticket = o.getString("ticket"),
                            symbol = sym,
                            name = o.optString("name", sym),
                            side = PositionSide.valueOf(o.optString("side", "LONG")),
                            quantity = o.getInt("quantity"),
                            entryPrice = o.getDouble("entryPrice"),
                            exitPrice = o.getDouble("exitPrice"),
                            leverage = o.optInt("leverage", 1),
                            openedAt = o.optLong("openedAt", System.currentTimeMillis()),
                            closedAt = o.optLong("closedAt", System.currentTimeMillis()),
                            realizedPnL = o.getDouble("realizedPnL"),
                            reason = runCatching { CloseReason.valueOf(o.optString("reason", "MANUAL")) }.getOrDefault(CloseReason.MANUAL),
                            assetClass = runCatching { AssetClass.valueOf(o.optString("assetClass", defaultAsset.name)) }.getOrDefault(defaultAsset)
                        )
                    )
                }
            }

            orders.clear()
            val ordArr = root.optJSONArray("orders")
            if (ordArr != null) {
                for (i in 0 until ordArr.length()) {
                    val o = ordArr.getJSONObject(i)
                    val sym = o.getString("symbol")
                    val specs = stocks[sym]?.specs ?: InstrumentSpecs()
                    orders.add(
                        Order(
                            id = o.getString("id"),
                            symbol = sym,
                            side = OrderSide.valueOf(o.optString("side", "BUY")),
                            type = OrderType.valueOf(o.optString("type", "MARKET")),
                            timeInForce = TimeInForce.valueOf(o.optString("timeInForce", "DAY")),
                            quantity = o.getInt("quantity"),
                            filledQuantity = o.optInt("filledQuantity", 0),
                            price = if (o.has("price")) o.getDouble("price") else null,
                            stopPrice = if (o.has("stopPrice")) o.getDouble("stopPrice") else null,
                            leverage = o.optInt("leverage", 1),
                            positionSide = PositionSide.valueOf(o.optString("positionSide", "LONG")),
                            takeProfitPrice = if (o.has("tp")) o.getDouble("tp") else null,
                            stopLossPrice = if (o.has("sl")) o.getDouble("sl") else null,
                            specs = specs,
                            status = OrderStatus.valueOf(o.optString("status", "NEW")),
                            avgFillPrice = if (o.has("avgFillPrice")) o.getDouble("avgFillPrice") else null,
                            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            transactions.clear()
            val txArr = root.optJSONArray("transactions")
            if (txArr != null) {
                for (i in 0 until txArr.length()) {
                    val o = txArr.getJSONObject(i)
                    transactions.add(
                        Transaction(
                            id = o.getString("id"),
                            type = TransactionType.valueOf(o.optString("type", "DEPOSIT")),
                            amount = o.getDouble("amount"),
                            balanceAfter = o.getDouble("balanceAfter"),
                            description = o.getString("description"),
                            timestamp = o.getLong("timestamp"),
                            reference = if (o.has("reference")) o.getString("reference") else null
                        )
                    )
                }
            }

            equityHistory.clear()
            val eqArr = root.optJSONArray("equityHistory")
            if (eqArr != null) {
                for (i in 0 until eqArr.length()) {
                    val o = eqArr.getJSONObject(i)
                    equityHistory.add(EquityPoint(o.getLong("t"), o.getDouble("e")))
                }
            }

            priceAlerts.clear()
            val alArr = root.optJSONArray("priceAlerts")
            if (alArr != null) {
                for (i in 0 until alArr.length()) {
                    val o = alArr.getJSONObject(i)
                    priceAlerts.add(
                        PriceAlert(
                            id = o.getString("id"),
                            symbol = o.getString("symbol"),
                            targetPrice = o.getDouble("targetPrice"),
                            condition = AlertCondition.valueOf(o.optString("condition", "ABOVE")),
                            isTriggered = o.optBoolean("isTriggered", false),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                            triggeredAt = if (o.has("triggeredAt")) o.getLong("triggeredAt") else null
                        )
                    )
                }
            }
            return true
        } catch (_: Exception) {
            return false
        }
    }

    fun getAccountStatement(): AccountStatement {
        val portfolio = getPortfolio()
        val txList = transactions.toList()
        val totalDeposits = txList.filter { it.type == TransactionType.DEPOSIT || it.type == TransactionType.RESET }
            .sumOf { it.amount }
        val totalWithdrawals = txList.filter { it.type == TransactionType.WITHDRAWAL }
            .sumOf { abs(it.amount) }
        val realized = closedTrades.sumOf { it.realizedPnL }
        val account = TradingAccount(
            id = if (DataSourceConfig.accountType == AccountType.DEMO) "DEMO-884920" else "LIVE-109482",
            ownerName = if (DataSourceConfig.accountType == AccountType.DEMO) "Demo Trader ()" else "Verified Live Trader",
            type = DataSourceConfig.accountType,
            currency = "USD",
            startingBalance = initialDeposit,
            createdAt = accountCreatedAt
        )
        return AccountStatement(
            account = account,
            from = accountCreatedAt,
            to = System.currentTimeMillis(),
            openingBalance = initialDeposit,
            deposits = totalDeposits,
            withdrawals = totalWithdrawals,
            realizedPnL = realized,
            closingBalance = portfolio.accountBalance,
            equity = portfolio.equity,
            transactions = txList,
            closedTrades = closedTrades.toList()
        )
    }

    fun getPortfolio(): Portfolio {
        val positionsList = positions.values.map { mp ->
            Position(
                symbol = mp.symbol,
                companyName = mp.companyName,
                quantity = mp.quantity,
                averageCost = mp.averageCost,
                currentPrice = currentPrices[mp.symbol] ?: mp.averageCost,
                side = mp.side,
                leverage = mp.leverage,
                takeProfitPrice = mp.takeProfitPrice,
                stopLossPrice = mp.stopLossPrice,
                specs = mp.specs,
                openedAt = mp.openedAt
            )
        }
        val dayPnL = positionsList.sumOf {
            val open = openPrices[it.symbol] ?: it.currentPrice
            val mult = it.specs.contractMultiplier
            when (it.side) {
                PositionSide.LONG -> (it.currentPrice - open) * it.quantity * mult
                PositionSide.SHORT -> (open - it.currentPrice) * it.quantity * mult
            }
        }
        val realized = closedTrades.sumOf { it.realizedPnL }
        return Portfolio(positionsList, cashBalance, dayPnL, realized)
    }

    fun getPositions(): List<Position> {
        return positions.values.map { mp ->
            Position(
                symbol = mp.symbol,
                companyName = mp.companyName,
                quantity = mp.quantity,
                averageCost = mp.averageCost,
                currentPrice = currentPrices[mp.symbol] ?: mp.averageCost,
                side = mp.side,
                leverage = mp.leverage,
                takeProfitPrice = mp.takeProfitPrice,
                stopLossPrice = mp.stopLossPrice,
                specs = mp.specs,
                openedAt = mp.openedAt
            )
        }
    }

    fun getCashBalance(): Double = cashBalance
}
