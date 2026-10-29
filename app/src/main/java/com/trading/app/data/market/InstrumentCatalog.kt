package com.trading.app.data.market

import com.trading.app.domain.model.AssetClass
import com.trading.app.domain.model.InstrumentSpecs

/**
 * A tradable instrument and how to source its market data.
 *
 * @param feedSymbol   Finnhub symbol streamed over the WebSocket (null = no direct feed).
 * @param restQuote    Finnhub `/quote` REST endpoint supports this symbol on the free tier.
 * @param historySymbol Symbol for the public historical-chart endpoint (Yahoo Finance).
 * @param derivedFrom  Symbol of a live underlying this instrument is priced from (futures basis / stock CFDs).
 * @param spreadBps    Dealer spread in basis points of price, used to build BID/ASK around the live price.
 * @param fallbackPrice Offline starting price for the simulator fallback.
 */
data class Instrument(
    val symbol: String,
    val name: String,
    val specs: InstrumentSpecs,
    val fallbackPrice: Double,
    val feedSymbol: String? = null,
    val restQuote: Boolean = false,
    val historySymbol: String? = null,
    val derivedFrom: String? = null,
    val spreadBps: Double = 2.0,
    val marketCap: Long = 0L
) {
    val assetClass: AssetClass get() = specs.assetClass
}

object InstrumentCatalog {

    private fun stock(symbol: String, name: String, price: Double, cap: Long) = Instrument(
        symbol = symbol, name = name, fallbackPrice = price, marketCap = cap,
        specs = InstrumentSpecs(assetClass = AssetClass.EQUITY, maxLeverage = 5),
        feedSymbol = symbol, restQuote = true, historySymbol = symbol, spreadBps = 2.0
    )

    private fun fx(
        base: String, quote: String, price: Double, category: String, spreadBps: Double, maxLev: Int
    ): Instrument {
        val jpy = quote == "JPY"
        return Instrument(
            symbol = "$base/$quote",
            name = "${currencyName(base)} / ${currencyName(quote)}",
            fallbackPrice = price,
            specs = InstrumentSpecs(
                assetClass = AssetClass.FOREX,
                unitOfMeasure = "Units",
                pipSize = if (jpy || quote in setOf("TRY", "ZAR", "MXN")) 0.01 else 0.0001,
                tickSize = if (jpy || quote in setOf("TRY", "ZAR", "MXN")) 0.001 else 0.00001,
                standardLotUnits = 100_000.0,
                overnightSwapLong = -0.006, overnightSwapShort = 0.002,
                maxLeverage = maxLev,
                subCategory = category,
                quoteCurrency = quote
            ),
            feedSymbol = "OANDA:${base}_$quote",
            historySymbol = "$base$quote=X",
            spreadBps = spreadBps
        )
    }

    private fun oanda(
        symbol: String, name: String, assetClass: AssetClass, price: Double, feed: String,
        history: String, quoteCcy: String, unit: String, maxLev: Int, spreadBps: Double,
        tick: Double = 0.01, multiplier: Double = 1.0
    ) = Instrument(
        symbol = symbol, name = name, fallbackPrice = price,
        specs = InstrumentSpecs(
            assetClass = assetClass, unitOfMeasure = unit, pipSize = tick, tickSize = tick,
            contractMultiplier = multiplier, maxLeverage = maxLev, quoteCurrency = quoteCcy,
            overnightSwapLong = -0.012, overnightSwapShort = 0.003
        ),
        feedSymbol = "OANDA:$feed", historySymbol = history, spreadBps = spreadBps
    )

    private fun crypto(symbol: String, name: String, price: Double, binance: String, yahoo: String, cap: Long) =
        Instrument(
            symbol = symbol, name = name, fallbackPrice = price, marketCap = cap,
            specs = InstrumentSpecs(
                assetClass = AssetClass.CRYPTO, unitOfMeasure = symbol.substringBefore("/"),
                pipSize = 0.01, tickSize = 0.01, fundingRate24h = 0.01, maxLeverage = 2
            ),
            feedSymbol = "BINANCE:$binance", restQuote = true, historySymbol = yahoo, spreadBps = 5.0
        )

    private fun future(
        symbol: String, name: String, underlying: String, yahoo: String, price: Double,
        multiplier: Double, tick: Double, maxLev: Int
    ) = Instrument(
        symbol = symbol, name = name, fallbackPrice = price,
        specs = InstrumentSpecs(
            assetClass = AssetClass.FUTURES, unitOfMeasure = "Contracts", contractMultiplier = multiplier,
            pipSize = tick, tickSize = tick, expiryDate = "DEC 2026", maxLeverage = maxLev
        ),
        historySymbol = yahoo, derivedFrom = underlying, spreadBps = 1.0
    )

    private fun stockCfd(underlying: String, name: String, price: Double) = Instrument(
        symbol = "$underlying.CFD", name = "$name CFD", fallbackPrice = price,
        specs = InstrumentSpecs(
            assetClass = AssetClass.CFD, unitOfMeasure = "Contracts", maxLeverage = 5,
            overnightSwapLong = -0.0185, overnightSwapShort = 0.0045
        ),
        historySymbol = underlying, derivedFrom = underlying, spreadBps = 4.0
    )

    val all: List<Instrument> = listOf(
        // ---- Stocks & equities ----
        stock("AAPL", "Apple Inc.", 230.0, 3_500_000_000_000),
        stock("MSFT", "Microsoft Corp.", 420.0, 3_100_000_000_000),
        stock("GOOGL", "Alphabet Inc.", 170.0, 2_100_000_000_000),
        stock("AMZN", "Amazon.com Inc.", 190.0, 2_000_000_000_000),
        stock("NVDA", "NVIDIA Corp.", 135.0, 3_300_000_000_000),
        stock("TSLA", "Tesla Inc.", 250.0, 800_000_000_000),
        stock("META", "Meta Platforms Inc.", 560.0, 1_400_000_000_000),
        stock("JPM", "JPMorgan Chase & Co.", 220.0, 620_000_000_000),
        stock("V", "Visa Inc.", 290.0, 580_000_000_000),
        stock("NFLX", "Netflix Inc.", 700.0, 300_000_000_000),
        stock("AMD", "Advanced Micro Devices", 160.0, 260_000_000_000),
        stock("DIS", "The Walt Disney Co.", 95.0, 175_000_000_000),

        // ---- Forex: majors ----
        fx("EUR", "USD", 1.0850, "Major", 0.8, 30),
        fx("GBP", "USD", 1.2950, "Major", 1.0, 30),
        fx("USD", "JPY", 150.20, "Major", 1.0, 30),
        fx("USD", "CHF", 0.8650, "Major", 1.2, 30),
        fx("AUD", "USD", 0.6650, "Major", 1.2, 30),
        fx("USD", "CAD", 1.3750, "Major", 1.2, 30),
        fx("NZD", "USD", 0.6050, "Major", 1.6, 30),
        // ---- Forex: minors (crosses) ----
        fx("EUR", "GBP", 0.8360, "Minor", 1.5, 20),
        fx("EUR", "JPY", 162.90, "Minor", 1.6, 20),
        fx("GBP", "JPY", 194.50, "Minor", 2.2, 20),
        fx("AUD", "JPY", 99.80, "Minor", 2.0, 20),
        fx("EUR", "CHF", 0.9380, "Minor", 1.8, 20),
        // ---- Forex: exotics ----
        fx("USD", "TRY", 34.30, "Exotic", 15.0, 10),
        fx("USD", "ZAR", 17.80, "Exotic", 10.0, 10),
        fx("USD", "MXN", 19.90, "Exotic", 8.0, 10),
        fx("USD", "SGD", 1.3200, "Exotic", 3.0, 10),

        // ---- Stock market indices (cash index CFDs) ----
        oanda("US500", "S&P 500 Index", AssetClass.INDEX, 5800.0, "SPX500_USD", "^GSPC", "USD", "Index pts", 20, 0.8),
        oanda("US100", "Nasdaq 100 Index", AssetClass.INDEX, 20400.0, "NAS100_USD", "^NDX", "USD", "Index pts", 20, 1.0),
        oanda("US30", "Dow Jones 30 Index", AssetClass.INDEX, 42000.0, "US30_USD", "^DJI", "USD", "Index pts", 20, 1.0),
        oanda("GER40", "Germany 40 (DAX)", AssetClass.INDEX, 19400.0, "DE30_EUR", "^GDAXI", "EUR", "Index pts", 20, 1.0),
        oanda("UK100", "UK 100 (FTSE)", AssetClass.INDEX, 8200.0, "UK100_GBP", "^FTSE", "GBP", "Index pts", 20, 1.2),
        oanda("JP225", "Japan 225 (Nikkei)", AssetClass.INDEX, 39000.0, "JP225_USD", "^N225", "USD", "Index pts", 20, 1.5),

        // ---- Commodities ----
        oanda("XAU/USD", "Gold Spot (troy oz)", AssetClass.COMMODITY, 2700.0, "XAU_USD", "GC=F", "USD", "oz", 20, 1.5),
        oanda("XAG/USD", "Silver Spot (troy oz)", AssetClass.COMMODITY, 32.0, "XAG_USD", "SI=F", "USD", "oz", 10, 3.0, tick = 0.001),
        oanda("BRENT", "Brent Crude Oil (bbl)", AssetClass.COMMODITY, 74.0, "BCO_USD", "BZ=F", "USD", "bbl", 10, 4.0),
        oanda("WTI", "WTI Crude Oil (bbl)", AssetClass.COMMODITY, 71.0, "WTICO_USD", "CL=F", "USD", "bbl", 10, 4.0),
        oanda("NATGAS", "Natural Gas (MMBtu)", AssetClass.COMMODITY, 2.60, "NATGAS_USD", "NG=F", "USD", "MMBtu", 10, 10.0, tick = 0.001),
        oanda("COPPER", "Copper (lb)", AssetClass.COMMODITY, 4.30, "XCU_USD", "HG=F", "USD", "lb", 10, 6.0, tick = 0.0001),

        // ---- Futures (priced from the live underlying + basis to the futures settlement) ----
        future("ESZ6", "E-mini S&P 500 Futures", "US500", "ES=F", 5830.0, 50.0, 0.25, 20),
        future("NQZ6", "E-mini Nasdaq 100 Futures", "US100", "NQ=F", 20500.0, 20.0, 0.25, 20),
        future("CLZ6", "Crude Oil WTI Futures", "WTI", "CL=F", 71.0, 1000.0, 0.01, 10),
        future("GCZ6", "Gold Futures", "XAU/USD", "GC=F", 2720.0, 100.0, 0.1, 20),

        // ---- Cryptocurrencies ----
        crypto("BTC/USD", "Bitcoin", 80000.0, "BTCUSDT", "BTC-USD", 1_600_000_000_000),
        crypto("ETH/USD", "Ethereum", 3000.0, "ETHUSDT", "ETH-USD", 360_000_000_000),
        crypto("SOL/USD", "Solana", 170.0, "SOLUSDT", "SOL-USD", 80_000_000_000),
        crypto("XRP/USD", "XRP", 0.60, "XRPUSDT", "XRP-USD", 34_000_000_000),
        crypto("DOGE/USD", "Dogecoin", 0.15, "DOGEUSDT", "DOGE-USD", 22_000_000_000),

        // ---- CFDs (contracts for difference on single stocks) ----
        stockCfd("AAPL", "Apple", 230.0),
        stockCfd("TSLA", "Tesla", 250.0),
        stockCfd("NVDA", "NVIDIA", 135.0),
        stockCfd("AMZN", "Amazon", 190.0)
    )

    private val bySymbol: Map<String, Instrument> = all.associateBy { it.symbol }
    private val byFeed: Map<String, List<Instrument>> =
        all.filter { it.feedSymbol != null }.groupBy { it.feedSymbol!! }

    operator fun get(symbol: String): Instrument? = bySymbol[symbol]
    fun contains(symbol: String): Boolean = bySymbol.containsKey(symbol)
    fun byFeedSymbol(feed: String): List<Instrument> = byFeed[feed].orEmpty()
    fun derivedFrom(symbol: String): List<Instrument> = all.filter { it.derivedFrom == symbol }
    fun feedSymbols(): List<String> = byFeed.keys.toList()

    /** Dashboard indices strip: symbol -> display name. */
    val headlineIndices = listOf("US500" to "S&P 500", "US100" to "NASDAQ 100", "US30" to "DOW 30")

    /** US sector SPDR ETFs used for real sector performance. */
    val sectorEtfs = listOf(
        "XLK" to "Technology", "XLV" to "Healthcare", "XLF" to "Financials",
        "XLY" to "Consumer Discretionary", "XLC" to "Communication Services", "XLI" to "Industrials",
        "XLP" to "Consumer Staples", "XLE" to "Energy", "XLU" to "Utilities",
        "XLRE" to "Real Estate", "XLB" to "Materials"
    )

    /**
     * Instrument that converts [currency] into USD, and whether its price must be inverted.
     * e.g. JPY -> (USD/JPY, invert = true); EUR -> (EUR/USD, invert = false).
     */
    fun usdConversion(currency: String): Pair<String, Boolean>? {
        if (currency == "USD") return null
        bySymbol["$currency/USD"]?.let { return it.symbol to false }
        bySymbol["USD/$currency"]?.let { return it.symbol to true }
        return null
    }

    private fun currencyName(code: String) = when (code) {
        "USD" -> "US Dollar"; "EUR" -> "Euro"; "GBP" -> "British Pound"; "JPY" -> "Japanese Yen"
        "CHF" -> "Swiss Franc"; "AUD" -> "Australian Dollar"; "CAD" -> "Canadian Dollar"
        "NZD" -> "New Zealand Dollar"; "TRY" -> "Turkish Lira"; "ZAR" -> "South African Rand"
        "MXN" -> "Mexican Peso"; "SGD" -> "Singapore Dollar"
        else -> code
    }
}
