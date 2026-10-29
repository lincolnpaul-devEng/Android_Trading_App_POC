package com.trading.app.domain.model

import org.junit.Assert.*
import org.junit.Test

class PortfolioTest {

    private fun portfolio(
        positions: List<Position> = listOf(
            Position("AAPL", "Apple", 100, 150.0, 160.0),
            Position("MSFT", "Microsoft", 50, 300.0, 320.0)
        ),
        cash: Double = 50_000.0,
        dayPnL: Double = 500.0
    ) = Portfolio(positions, cash, dayPnL)

    @Test
    fun `totalMarketValue sums all position market values`() {
        val p = portfolio()
        // AAPL: 100*160=16000, MSFT: 50*320=16000 => 32000
        assertEquals(32_000.0, p.totalMarketValue, 0.001)
    }

    @Test
    fun `totalCostBasis sums all position cost bases`() {
        val p = portfolio()
        // AAPL: 100*150=15000, MSFT: 50*300=15000 => 30000
        assertEquals(30_000.0, p.totalCostBasis, 0.001)
    }

    @Test
    fun `totalUnrealizedPnL sums all position PnL`() {
        val p = portfolio()
        // AAPL: 1000, MSFT: 1000 => 2000
        assertEquals(2_000.0, p.totalUnrealizedPnL, 0.001)
    }

    @Test
    fun `totalValue is market value plus cash`() {
        val p = portfolio()
        assertEquals(82_000.0, p.totalValue, 0.001)
        assertEquals(82_000.0, p.equity, 0.001)
    }

    @Test
    fun `freeMargin is equity minus used margin`() {
        val p = portfolio(cash = 50_000.0)
        // accountBalance = 50000 + 30000 = 80000; equity = 82000; usedMargin = 30000 => freeMargin = 52000
        assertEquals(30_000.0, p.totalUsedMargin, 0.001)
        assertEquals(80_000.0, p.accountBalance, 0.001)
        assertEquals(52_000.0, p.freeMargin, 0.001)
        assertEquals(82_000.0 / 30_000.0 * 100, p.marginLevel, 0.001)
    }

    @Test
    fun `buyingPower equals cash balance`() {
        val p = portfolio(cash = 25_000.0)
        assertEquals(25_000.0, p.buyingPower, 0.001)
    }

    @Test
    fun `empty portfolio has zero market value`() {
        val p = portfolio(positions = emptyList())
        assertEquals(0.0, p.totalMarketValue, 0.001)
        assertEquals(50_000.0, p.totalValue, 0.001)
    }
}
