package com.trading.app.data.simulator

import app.cash.turbine.test
import com.trading.app.domain.model.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MarketSimulatorTest {

    private lateinit var simulator: MarketSimulator

    @Before
    fun setup() {
        simulator = MarketSimulator()
    }

    // --- Symbol Catalog ---

    @Test
    fun `getAllSymbols returns all seeded symbols`() {
        val symbols = simulator.getAllSymbols()
        assertTrue(symbols.size >= 15)
        assertTrue(symbols.contains("AAPL"))
        assertTrue(symbols.contains("MSFT"))
        assertTrue(symbols.contains("TSLA"))
        assertTrue(symbols.contains("NVDA"))
    }

    @Test
    fun `getStockName returns correct company name`() {
        assertEquals("Apple Inc.", simulator.getStockName("AAPL"))
        assertEquals("Microsoft Corp.", simulator.getStockName("MSFT"))
    }

    @Test
    fun `getStockName returns symbol for unknown stocks`() {
        assertEquals("UNKNOWN", simulator.getStockName("UNKNOWN"))
    }

    // --- Quote Generation ---

    @Test
    fun `generateQuote returns valid quote for known symbol`() {
        val quote = simulator.generateQuote("AAPL")
        assertEquals("AAPL", quote.symbol)
        assertEquals("Apple Inc.", quote.companyName)
        assertTrue(quote.lastPrice > 0)
        assertTrue(quote.volume > 0)
        assertTrue(quote.high >= quote.low)
    }

    @Test
    fun `generateQuote price changes between calls`() {
        val price1 = simulator.generateQuote("TSLA").lastPrice
        val price2 = simulator.generateQuote("TSLA").lastPrice
        val price3 = simulator.generateQuote("TSLA").lastPrice
        // At least one of these should differ due to random walk
        val allSame = (price1 == price2 && price2 == price3)
        assertFalse("Prices should change over time", allSame)
    }

    @Test
    fun `generateQuote high is greater than or equal to low`() {
        repeat(20) {
            val quote = simulator.generateQuote("AAPL")
            assertTrue("high ${quote.high} should >= low ${quote.low}", quote.high >= quote.low)
        }
    }

    @Test
    fun `generateQuote bid is less than ask`() {
        repeat(10) {
            val quote = simulator.generateQuote("MSFT")
            assertTrue("bid ${quote.bid} should < ask ${quote.ask}", quote.bid < quote.ask)
        }
    }

    // --- Streaming ---

    @Test
    fun `streamQuote emits quotes for symbol`() = runTest {
        simulator.streamQuote("AAPL").test {
            val first = awaitItem()
            assertEquals("AAPL", first.symbol)
            assertTrue(first.lastPrice > 0)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `streamQuotes emits list of quotes`() = runTest {
        simulator.streamQuotes(listOf("AAPL", "MSFT")).test {
            val quotes = awaitItem()
            assertEquals(2, quotes.size)
            assertEquals("AAPL", quotes[0].symbol)
            assertEquals("MSFT", quotes[1].symbol)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `streamIndices emits three market indices`() = runTest {
        simulator.streamIndices().test {
            val indices = awaitItem()
            assertEquals(3, indices.size)
            assertTrue(indices.any { it.name == "S&P 500" })
            assertTrue(indices.any { it.name == "NASDAQ" })
            assertTrue(indices.any { it.name == "DOW" })
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `streamSectorPerformance emits all sectors`() = runTest {
        simulator.streamSectorPerformance().test {
            val sectors = awaitItem()
            assertTrue(sectors.size >= 10)
            assertTrue(sectors.any { it.name == "Technology" })
            assertTrue(sectors.any { it.name == "Healthcare" })
            cancelAndConsumeRemainingEvents()
        }
    }

    // --- Candlestick Generation ---

    @Test
    fun `generateCandlesticks emits candles for 1D interval`() = runTest {
        simulator.generateCandlesticks("AAPL", "1D").test {
            val candles = awaitItem()
            assertEquals(90, candles.size)
            candles.forEach { c ->
                assertTrue(c.high >= c.low)
                assertTrue(c.volume > 0)
            }
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `generateCandlesticks emits 60 candles for 1m interval`() = runTest {
        simulator.generateCandlesticks("TSLA", "1m").test {
            val candles = awaitItem()
            assertEquals(60, candles.size)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `candlestick timestamps are sequential`() = runTest {
        simulator.generateCandlesticks("MSFT", "1D").test {
            val candles = awaitItem()
            for (i in 1 until candles.size) {
                assertTrue(candles[i].timestamp > candles[i - 1].timestamp)
            }
            cancelAndConsumeRemainingEvents()
        }
    }

    // --- Order Book ---

    @Test
    fun `generateOrderBook emits valid order book`() = runTest {
        simulator.generateOrderBook("AAPL").test {
            val book = awaitItem()
            assertEquals("AAPL", book.symbol)
            assertTrue(book.bids.isNotEmpty())
            assertTrue(book.asks.isNotEmpty())
            // Bids should be sorted descending
            for (i in 1 until book.bids.size) {
                assertTrue(book.bids[i - 1].price >= book.bids[i].price)
            }
            // Asks should be sorted ascending
            for (i in 1 until book.asks.size) {
                assertTrue(book.asks[i - 1].price <= book.asks[i].price)
            }
            cancelAndConsumeRemainingEvents()
        }
    }

    // --- Search ---

    @Test
    fun `searchSymbols finds by symbol prefix`() {
        val results = simulator.searchSymbols("AA")
        assertTrue(results.any { it.symbol == "AAPL" })
    }

    @Test
    fun `searchSymbols finds by company name`() {
        val results = simulator.searchSymbols("Apple")
        assertTrue(results.any { it.symbol == "AAPL" })
    }

    @Test
    fun `searchSymbols is case insensitive`() {
        val results = simulator.searchSymbols("apple")
        assertTrue(results.any { it.symbol == "AAPL" })
    }

    @Test
    fun `searchSymbols returns empty for unknown query`() {
        val results = simulator.searchSymbols("ZZZZZ")
        assertTrue(results.isEmpty())
    }

    // --- Order Execution ---

    @Test
    fun `place market buy order succeeds`() {
        val order = Order(
            id = "", symbol = "AAPL", side = OrderSide.BUY,
            type = OrderType.MARKET, timeInForce = TimeInForce.DAY,
            quantity = 10
        )
        val result = simulator.placeOrder(order)
        assertTrue(result.isSuccess)
        val filled = result.getOrThrow()
        assertEquals(OrderStatus.FILLED, filled.status)
        assertEquals(10, filled.filledQuantity)
        assertTrue(filled.avgFillPrice!! > 0)
    }

    @Test
    fun `place buy order reduces cash balance`() {
        val initialCash = simulator.getCashBalance()
        val order = Order(
            id = "", symbol = "AAPL", side = OrderSide.BUY,
            type = OrderType.MARKET, timeInForce = TimeInForce.DAY,
            quantity = 10
        )
        simulator.placeOrder(order)
        assertTrue(simulator.getCashBalance() < initialCash)
    }

    @Test
    fun `place sell order fails when no position`() {
        val order = Order(
            id = "", symbol = "AAPL", side = OrderSide.SELL,
            type = OrderType.MARKET, timeInForce = TimeInForce.DAY,
            quantity = 10
        )
        val result = simulator.placeOrder(order)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Insufficient shares") == true)
    }

    @Test
    fun `buy then sell creates and clears position`() {
        val buy = Order(
            id = "", symbol = "AAPL", side = OrderSide.BUY,
            type = OrderType.MARKET, timeInForce = TimeInForce.DAY,
            quantity = 10
        )
        simulator.placeOrder(buy)
        assertEquals(10, simulator.getPositions().find { it.symbol == "AAPL" }?.quantity)

        val sell = Order(
            id = "", symbol = "AAPL", side = OrderSide.SELL,
            type = OrderType.MARKET, timeInForce = TimeInForce.DAY,
            quantity = 10
        )
        simulator.placeOrder(sell)
        assertNull(simulator.getPositions().find { it.symbol == "AAPL" })
    }

    @Test
    fun `buy order exceeding buying power fails`() {
        val order = Order(
            id = "", symbol = "AAPL", side = OrderSide.BUY,
            type = OrderType.MARKET, timeInForce = TimeInForce.DAY,
            quantity = 1_000_000 // Way more than $100K allows
        )
        val result = simulator.placeOrder(order)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Insufficient buying power") == true)
    }

    @Test
    fun `zero quantity order fails`() {
        val order = Order(
            id = "", symbol = "AAPL", side = OrderSide.BUY,
            type = OrderType.MARKET, timeInForce = TimeInForce.DAY,
            quantity = 0
        )
        val result = simulator.placeOrder(order)
        assertTrue(result.isFailure)
    }

    @Test
    fun `negative quantity order fails`() {
        val order = Order(
            id = "", symbol = "AAPL", side = OrderSide.BUY,
            type = OrderType.MARKET, timeInForce = TimeInForce.DAY,
            quantity = -5
        )
        val result = simulator.placeOrder(order)
        assertTrue(result.isFailure)
    }

    @Test
    fun `unknown symbol order fails`() {
        val order = Order(
            id = "", symbol = "ZZZZZ", side = OrderSide.BUY,
            type = OrderType.MARKET, timeInForce = TimeInForce.DAY,
            quantity = 10
        )
        val result = simulator.placeOrder(order)
        assertTrue(result.isFailure)
    }

    @Test
    fun `order history tracks placed orders`() {
        val order = Order(
            id = "", symbol = "AAPL", side = OrderSide.BUY,
            type = OrderType.MARKET, timeInForce = TimeInForce.DAY,
            quantity = 5
        )
        assertTrue(simulator.getOrderHistory().isEmpty())
        simulator.placeOrder(order)
        assertEquals(1, simulator.getOrderHistory().size)
    }

    @Test
    fun `multiple buys accumulate position with weighted average cost`() {
        val buy1 = Order(
            id = "", symbol = "AAPL", side = OrderSide.BUY,
            type = OrderType.MARKET, timeInForce = TimeInForce.DAY,
            quantity = 10
        )
        val buy2 = Order(
            id = "", symbol = "AAPL", side = OrderSide.BUY,
            type = OrderType.MARKET, timeInForce = TimeInForce.DAY,
            quantity = 10
        )
        simulator.placeOrder(buy1)
        simulator.placeOrder(buy2)

        val pos = simulator.getPositions().find { it.symbol == "AAPL" }
        assertNotNull(pos)
        assertEquals(20, pos!!.quantity)
        assertTrue(pos.averageCost > 0)
    }

    // --- Portfolio ---

    @Test
    fun `initial cash balance is 100K`() {
        assertEquals(100_000.0, simulator.getCashBalance(), 0.001)
    }

    @Test
    fun `getPortfolio returns valid Portfolio`() {
        val portfolio = simulator.getPortfolio()
        assertEquals(100_000.0, portfolio.cashBalance, 0.001)
        assertTrue(portfolio.positions.isEmpty())
        assertEquals(100_000.0, portfolio.totalValue, 0.001)
    }

    @Test
    fun `getPortfolio reflects positions after trades`() {
        val buy = Order(
            id = "", symbol = "MSFT", side = OrderSide.BUY,
            type = OrderType.MARKET, timeInForce = TimeInForce.DAY,
            quantity = 5
        )
        simulator.placeOrder(buy)
        val portfolio = simulator.getPortfolio()
        assertEquals(1, portfolio.positions.size)
        assertEquals("MSFT", portfolio.positions[0].symbol)
        assertTrue(portfolio.cashBalance < 100_000.0)
    }

    // --- Top Movers ---

    @Test
    fun `getTopMovers emits sorted list`() = runTest {
        simulator.getTopMovers().test {
            val movers = awaitItem()
            assertTrue(movers.isNotEmpty())
            assertTrue(movers.size <= 10)
            // Should be sorted by absolute percent change descending
            for (i in 1 until movers.size) {
                assertTrue(
                    kotlin.math.abs(movers[i - 1].changePercent) >=
                            kotlin.math.abs(movers[i].changePercent)
                )
            }
            cancelAndConsumeRemainingEvents()
        }
    }

    // --- Leverage & Auto Close ---

    @Test
    fun `short order with leverage creates short position and deducts margin`() {
        val initialCash = simulator.getCashBalance()
        val shortOrder = Order(
            id = "", symbol = "TSLA", side = OrderSide.SELL,
            type = OrderType.MARKET, timeInForce = TimeInForce.DAY,
            quantity = 10, leverage = 5, positionSide = PositionSide.SHORT
        )
        val result = simulator.placeOrder(shortOrder)
        assertTrue(result.isSuccess)
        val pos = simulator.getPositions().find { it.symbol == "TSLA" }
        assertNotNull(pos)
        assertEquals(PositionSide.SHORT, pos!!.side)
        assertEquals(5, pos.leverage)
        assertEquals(10, pos.quantity)
        val expectedDeduction = (10 * pos.averageCost) / 5
        assertEquals(initialCash - expectedDeduction, simulator.getCashBalance(), 0.01)
    }

    @Test
    fun `order with auto close stores TP and SL targets on position`() {
        val buy = Order(
            id = "", symbol = "AAPL", side = OrderSide.BUY,
            type = OrderType.MARKET, timeInForce = TimeInForce.DAY,
            quantity = 5,
            takeProfitPrice = 250.0,
            stopLossPrice = 150.0
        )
        simulator.placeOrder(buy)
        val pos = simulator.getPositions().find { it.symbol == "AAPL" }
        assertNotNull(pos)
        assertEquals(250.0, pos!!.takeProfitPrice)
        assertEquals(150.0, pos.stopLossPrice)
    }

    // --- Multi-Asset Tests ---

    @Test
    fun `all 7 asset classes are present in simulator`() {
        val allQuotes = simulator.getQuotesByAssetClass(null)
        val assetClasses = allQuotes.map { it.assetClass }.toSet()
        assertEquals(7, assetClasses.size)
        assertTrue(assetClasses.contains(AssetClass.EQUITY))
        assertTrue(assetClasses.contains(AssetClass.FOREX))
        assertTrue(assetClasses.contains(AssetClass.INDEX))
        assertTrue(assetClasses.contains(AssetClass.CRYPTO))
        assertTrue(assetClasses.contains(AssetClass.COMMODITY))
        assertTrue(assetClasses.contains(AssetClass.FUTURES))
        assertTrue(assetClasses.contains(AssetClass.CFD))
    }

    @Test
    fun `closePosition closes open position and records realized trade and transaction`() {
        val buy = Order(
            id = "", symbol = "AAPL", side = OrderSide.BUY,
            type = OrderType.MARKET, timeInForce = TimeInForce.DAY,
            quantity = 10, leverage = 2
        )
        simulator.placeOrder(buy)
        assertNotNull(simulator.getPositions().find { it.symbol == "AAPL" })

        val closeResult = simulator.closePosition("AAPL")
        assertTrue(closeResult.isSuccess)
        assertNull(simulator.getPositions().find { it.symbol == "AAPL" })
        assertEquals(1, simulator.getClosedTrades().size)
        assertEquals("AAPL", simulator.getClosedTrades()[0].symbol)
        assertTrue(simulator.getTransactions().any { it.type == TransactionType.REALIZED_PNL })
    }

    @Test
    fun `resetDemoAccount restores 100K balance and clears positions`() {
        val buy = Order(
            id = "", symbol = "MSFT", side = OrderSide.BUY,
            type = OrderType.MARKET, timeInForce = TimeInForce.DAY,
            quantity = 10
        )
        simulator.placeOrder(buy)
        val portfolio = simulator.resetDemoAccount(100_000.0)
        assertTrue(portfolio.positions.isEmpty())
        assertEquals(100_000.0, portfolio.cashBalance, 0.001)
        assertEquals(100_000.0, portfolio.freeMargin, 0.001)
    }

    @Test
    fun `getAccountStatement summarizes balance deposits withdrawals and trades`() {
        simulator.depositFunds(5_000.0)
        val stmt = simulator.getAccountStatement()
        assertEquals(105_000.0, stmt.closingBalance, 0.01)
        assertTrue(stmt.transactions.isNotEmpty())
    }

    @Test
    fun `getQuotesByAssetClass filters correctly`() {
        val forex = simulator.getQuotesByAssetClass(AssetClass.FOREX)
        assertTrue(forex.isNotEmpty())
        assertTrue(forex.all { it.assetClass == AssetClass.FOREX })
        assertTrue(forex.any { it.symbol == "EUR/USD" })
        assertTrue(forex.any { it.symbol == "USD/TRY" })

        val crypto = simulator.getQuotesByAssetClass(AssetClass.CRYPTO)
        assertTrue(crypto.isNotEmpty())
        assertTrue(crypto.all { it.assetClass == AssetClass.CRYPTO })
        assertTrue(crypto.any { it.symbol == "BTC/USD" })

        val commodities = simulator.getQuotesByAssetClass(AssetClass.COMMODITY)
        assertTrue(commodities.any { it.symbol == "XAU/USD" })

        val futures = simulator.getQuotesByAssetClass(AssetClass.FUTURES)
        assertTrue(futures.any { it.symbol == "ESZ6" })

        val cfds = simulator.getQuotesByAssetClass(AssetClass.CFD)
        assertTrue(cfds.any { it.symbol == "US500.CFD" })
    }

    @Test
    fun `futures order factors in contract multiplier for margin`() {
        val initialCash = simulator.getCashBalance()
        // ESZ6 has 50x multiplier
        val futuresOrder = Order(
            id = "", symbol = "ESZ6", side = OrderSide.BUY,
            type = OrderType.MARKET, timeInForce = TimeInForce.DAY,
            quantity = 1, leverage = 10
        )
        val result = simulator.placeOrder(futuresOrder)
        assertTrue(result.isSuccess)
        val pos = simulator.getPositions().find { it.symbol == "ESZ6" }
        assertNotNull(pos)
        // requiredMargin = (1 * price * 50) / 10
        val expectedMargin = (pos!!.averageCost * 50.0) / 10.0
        assertEquals(expectedMargin, pos.requiredMargin, 0.01)
        assertEquals(initialCash - expectedMargin, simulator.getCashBalance(), 0.01)
    }
}
