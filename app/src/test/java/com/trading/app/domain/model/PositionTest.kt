package com.trading.app.domain.model

import org.junit.Assert.*
import org.junit.Test

class PositionTest {

    private fun position(
        qty: Int = 100,
        avgCost: Double = 150.0,
        currentPrice: Double = 160.0
    ) = Position(
        symbol = "AAPL",
        companyName = "Apple Inc.",
        quantity = qty,
        averageCost = avgCost,
        currentPrice = currentPrice
    )

    @Test
    fun `marketValue is quantity times current price`() {
        val pos = position(qty = 50, currentPrice = 200.0)
        assertEquals(10_000.0, pos.marketValue, 0.001)
    }

    @Test
    fun `costBasis is quantity times average cost`() {
        val pos = position(qty = 50, avgCost = 100.0)
        assertEquals(5_000.0, pos.costBasis, 0.001)
    }

    @Test
    fun `unrealizedPnL is positive when current price above average cost`() {
        val pos = position(qty = 100, avgCost = 100.0, currentPrice = 110.0)
        assertEquals(1_000.0, pos.unrealizedPnL, 0.001)
    }

    @Test
    fun `unrealizedPnL is negative when current price below average cost`() {
        val pos = position(qty = 100, avgCost = 100.0, currentPrice = 90.0)
        assertEquals(-1_000.0, pos.unrealizedPnL, 0.001)
    }

    @Test
    fun `unrealizedPnLPercent computed correctly`() {
        val pos = position(qty = 100, avgCost = 100.0, currentPrice = 110.0)
        assertEquals(10.0, pos.unrealizedPnLPercent, 0.001)
    }

    @Test
    fun `unrealizedPnLPercent is zero when cost basis is zero`() {
        val pos = position(qty = 100, avgCost = 0.0, currentPrice = 10.0)
        assertEquals(0.0, pos.unrealizedPnLPercent, 0.001)
    }

    @Test
    fun `isProfit returns true when in the green`() {
        assertTrue(position(avgCost = 100.0, currentPrice = 110.0).isProfit)
    }

    @Test
    fun `isProfit returns true when break-even`() {
        assertTrue(position(avgCost = 100.0, currentPrice = 100.0).isProfit)
    }

    @Test
    fun `isProfit returns false when in the red`() {
        assertFalse(position(avgCost = 110.0, currentPrice = 100.0).isProfit)
    }

    @Test
    fun `short position profits when price drops`() {
        val shortPos = Position(
            symbol = "AAPL",
            companyName = "Apple Inc.",
            quantity = 100,
            averageCost = 150.0,
            currentPrice = 140.0,
            side = PositionSide.SHORT,
            leverage = 2
        )
        assertEquals(1_000.0, shortPos.unrealizedPnL, 0.001)
        assertTrue(shortPos.isProfit)
        // requiredMargin = (100 * 150) / 2 = 7500. Return on margin = 1000 / 7500 = 13.333%
        assertEquals(13.333, shortPos.unrealizedPnLPercent, 0.01)
    }

    @Test
    fun `short position suffers loss when price rises`() {
        val shortPos = Position(
            symbol = "AAPL",
            companyName = "Apple Inc.",
            quantity = 50,
            averageCost = 100.0,
            currentPrice = 110.0,
            side = PositionSide.SHORT
        )
        assertEquals(-500.0, shortPos.unrealizedPnL, 0.001)
        assertFalse(shortPos.isProfit)
    }

    @Test
    fun `liquidationPrice calculates correctly for long and short`() {
        val longPos = Position("AAPL", "Apple", 10, 100.0, 100.0, PositionSide.LONG, leverage = 10)
        // 10x leverage => 100 * (1 - 0.09) = 91.0
        assertEquals(91.0, longPos.liquidationPrice, 0.001)

        val shortPos = Position("AAPL", "Apple", 10, 100.0, 100.0, PositionSide.SHORT, leverage = 10)
        // 10x leverage short => 100 * (1 + 0.09) = 109.0
        assertEquals(109.0, shortPos.liquidationPrice, 0.001)
    }
}
