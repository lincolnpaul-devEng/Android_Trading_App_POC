package com.trading.app.domain.model

enum class AssetClass(val displayName: String, val badge: String) {
    EQUITY("Stocks", "STK"),
    FOREX("Forex", "FX"),
    INDEX("Indices", "IDX"),
    CRYPTO("Crypto", "CRYPTO"),
    COMMODITY("Commodities", "COMM"),
    FUTURES("Futures", "FUT"),
    CFD("CFDs", "CFD")
}

/** Where the price currently shown for an instrument comes from. */
enum class PriceSource(val label: String) {
    /** Streaming ticks from the Finnhub WebSocket. */
    LIVE("LIVE"),
    /** Computed from a live underlying (e.g. futures basis, stock CFD). */
    DERIVED("DERIVED"),
    /** Last REST snapshot (market closed / no ticks yet). */
    DELAYED("DELAYED"),
    /** Offline random-walk fallback. */
    SIMULATED("SIM")
}

/** Health of the real-time streaming connection. */
enum class FeedStatus(val label: String) {
    CONNECTING("Connecting"),
    LIVE("Live"),
    RECONNECTING("Reconnecting"),
    OFFLINE("Offline"),
    SIMULATED("Simulated")
}

data class InstrumentSpecs(
    val assetClass: AssetClass = AssetClass.EQUITY,
    val contractSize: Double = 1.0,
    val unitOfMeasure: String = "Shares",
    val pipSize: Double = 0.01,
    val tickSize: Double = 0.01,
    val contractMultiplier: Double = 1.0,
    val expiryDate: String? = null,
    val overnightSwapLong: Double? = null,   // % daily financing
    val overnightSwapShort: Double? = null,
    val fundingRate24h: Double? = null,      // % 24h perp funding
    val minLotSize: Double = 0.01,
    val standardLotUnits: Double = 100_000.0,
    val maxLeverage: Int = 20,
    /** e.g. "Major", "Minor", "Exotic" for forex. */
    val subCategory: String? = null,
    /** Currency the price (and therefore P&L) is quoted in. Converted to USD for the account. */
    val quoteCurrency: String = "USD"
)

data class StockQuote(
    val symbol: String,
    val companyName: String,
    val lastPrice: Double,
    val change: Double,
    val changePercent: Double,
    val open: Double,
    val high: Double,
    val low: Double,
    val volume: Long,
    val marketCap: Long,
    val bid: Double,
    val ask: Double,
    val bidSize: Int,
    val askSize: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val specs: InstrumentSpecs = InstrumentSpecs(),
    val source: PriceSource = PriceSource.SIMULATED
) {
    val isPositive: Boolean get() = change >= 0
    val spread: Double get() = ask - bid
    val spreadPips: Double get() = if (specs.pipSize > 0) spread / specs.pipSize else 0.0
    val assetClass: AssetClass get() = specs.assetClass
}
