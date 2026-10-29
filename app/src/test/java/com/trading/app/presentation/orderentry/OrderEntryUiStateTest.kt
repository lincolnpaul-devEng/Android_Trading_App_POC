package com.trading.app.presentation.orderentry

import com.trading.app.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class OrderEntryUiStateTest {

    private fun state(
        quantity: String = "100",
        type: OrderType = OrderType.MARKET,
        currentPrice: Double = 150.0,
        limitPrice: String = "",
        side: OrderSide = OrderSide.BUY
    ) = OrderEntryUiState(
        symbol = "AAPL",
        companyName = "Apple Inc.",
        currentPrice = currentPrice,
        side = side,
        type = type,
        quantity = quantity,
        limitPrice = limitPrice,
        buyingPower = 100_000.0
    )

    @Test
    fun `estimatedCost for market order uses current price`() {
        val s = state(quantity = "100", type = OrderType.MARKET, currentPrice = 150.0)
        assertEquals(15_000.0, s.estimatedCost, 0.001)
    }

    @Test
    fun `estimatedCost for limit order uses limit price`() {
        val s = state(quantity = "100", type = OrderType.LIMIT, currentPrice = 150.0, limitPrice = "145.50")
        assertEquals(14_550.0, s.estimatedCost, 0.001)
    }

    @Test
    fun `estimatedCost for limit order falls back to current price when limit empty`() {
        val s = state(quantity = "100", type = OrderType.LIMIT, currentPrice = 150.0, limitPrice = "")
        assertEquals(15_000.0, s.estimatedCost, 0.001)
    }

    @Test
    fun `estimatedCost for stop order uses current price`() {
        val s = state(quantity = "50", type = OrderType.STOP, currentPrice = 200.0)
        assertEquals(10_000.0, s.estimatedCost, 0.001)
    }

    @Test
    fun `estimatedCost for stop-limit order uses limit price`() {
        val s = state(quantity = "50", type = OrderType.STOP_LIMIT, currentPrice = 200.0, limitPrice = "195.00")
        assertEquals(9_750.0, s.estimatedCost, 0.001)
    }

    @Test
    fun `estimatedCost is zero when quantity is empty`() {
        val s = state(quantity = "")
        assertEquals(0.0, s.estimatedCost, 0.001)
    }

    @Test
    fun `estimatedCost is zero when quantity is non-numeric`() {
        val s = state(quantity = "abc")
        assertEquals(0.0, s.estimatedCost, 0.001)
    }

    @Test
    fun `estimatedCost handles trailing stop`() {
        val s = state(quantity = "100", type = OrderType.TRAILING_STOP, currentPrice = 300.0)
        assertEquals(30_000.0, s.estimatedCost, 0.001)
    }

    @Test
    fun `requiredMargin correctly calculates with leverage`() {
        val s = state(quantity = "10", currentPrice = 100.0).copy(leverage = 5)
        // Notional = 1000. 5x leverage => requiredMargin = 200
        assertEquals(1000.0, s.estimatedCost, 0.001)
        assertEquals(200.0, s.requiredMargin, 0.001)
    }

    @Test
    fun `calculated TP and SL for short position matching reference math`() {
        // Short at $100 with 2x leverage:
        // +10% PnL on 2x leverage = 5% price drop -> TP = $95.00
        // -10% PnL on 2x leverage = 5% price rise -> SL = $105.00
        val s = state(quantity = "1", currentPrice = 100.0, side = OrderSide.SELL).copy(
            leverage = 2,
            autoCloseMode = com.trading.app.presentation.components.AutoCloseMode.PERCENT_PNL,
            takeProfit = "10",
            stopLoss = "10"
        )
        assertEquals(95.0, s.calculatedTpPrice!!, 0.001)
        assertEquals(105.0, s.calculatedSlPrice!!, 0.001)
    }

    @Test
    fun `calculated TP and SL for long position with percentage PnL`() {
        // Long at $100 with 5x leverage:
        // +25% PnL on 5x leverage = 5% price rise -> TP = $105.00
        // -25% PnL on 5x leverage = 5% price drop -> SL = $95.00
        val s = state(quantity = "1", currentPrice = 100.0, side = OrderSide.BUY).copy(
            leverage = 5,
            autoCloseMode = com.trading.app.presentation.components.AutoCloseMode.PERCENT_PNL,
            takeProfit = "25",
            stopLoss = "25"
        )
        assertEquals(105.0, s.calculatedTpPrice!!, 0.001)
        assertEquals(95.0, s.calculatedSlPrice!!, 0.001)
    }
}
