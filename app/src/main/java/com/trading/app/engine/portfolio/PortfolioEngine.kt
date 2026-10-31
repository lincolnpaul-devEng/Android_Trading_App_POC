package com.trading.app.engine.portfolio

import com.trading.app.engine.core.MoneyMath
import java.math.BigDecimal
import java.math.RoundingMode

data class AssetAllocationItem(
    val assetClass: String, // "Equities", "Bonds", "Cash"
    val allocationPct: Double,
    val value: BigDecimal,
    val colorHex: Long,
    val expectedAnnualReturn: Double,
    val annualVolatility: Double
)

data class PortfolioSnapshot(
    val totalValue: BigDecimal,
    val items: List<AssetAllocationItem>,
    val concentrationHHI: Double, // Herfindahl index (0 - 10000)
    val diversificationRating: String,
    val expectedReturnPct: Double,
    val portfolioVolatilityPct: Double
)

class PortfolioEngine(
    initialCapital: BigDecimal = BigDecimal("10000.00")
) {
    var totalPortfolioValue: BigDecimal = initialCapital
        private set

    // Default target allocation: 50% Equities, 30% Bonds, 20% Cash
    var equitiesPct: Double = 50.0
        private set
    var bondsPct: Double = 30.0
        private set
    var cashPct: Double = 20.0
        private set

    fun setAllocation(equities: Double, bonds: Double, cash: Double) {
        val sum = equities + bonds + cash
        if (sum > 0.0) {
            equitiesPct = (equities / sum) * 100.0
            bondsPct = (bonds / sum) * 100.0
            cashPct = (cash / sum) * 100.0
        }
    }

    fun getSnapshot(): PortfolioSnapshot {
        val eqVal = totalPortfolioValue.multiply(BigDecimal.valueOf(equitiesPct / 100.0)).setScale(2, RoundingMode.HALF_UP)
        val bdVal = totalPortfolioValue.multiply(BigDecimal.valueOf(bondsPct / 100.0)).setScale(2, RoundingMode.HALF_UP)
        val csVal = totalPortfolioValue.multiply(BigDecimal.valueOf(cashPct / 100.0)).setScale(2, RoundingMode.HALF_UP)

        val items = listOf(
            AssetAllocationItem(
                assetClass = "Equities",
                allocationPct = equitiesPct,
                value = eqVal,
                colorHex = 0xFF5FE2D6, // Cyan
                expectedAnnualReturn = 0.09, // 9%
                annualVolatility = 0.18 // 18%
            ),
            AssetAllocationItem(
                assetClass = "Bonds",
                allocationPct = bondsPct,
                value = bdVal,
                colorHex = 0xFF8B5CF6, // Purple
                expectedAnnualReturn = 0.04, // 4%
                annualVolatility = 0.06 // 6%
            ),
            AssetAllocationItem(
                assetClass = "Cash",
                allocationPct = cashPct,
                value = csVal,
                colorHex = 0xFF4ADE80, // Green
                expectedAnnualReturn = 0.02, // 2%
                annualVolatility = 0.00
            )
        )

        // HHI = sum of squared market share percentages (0 to 10,000)
        val hhi = (equitiesPct * equitiesPct) + (bondsPct * bondsPct) + (cashPct * cashPct)
        val rating = when {
            hhi > 6500 -> "High Concentration Risk (Vulnerable to single-asset crash)"
            hhi in 4000.0..6500.0 -> "Moderate Concentration"
            else -> "Well-Diversified Balanced Portfolio"
        }

        // Weighted Expected Return & Volatility approximation
        val expReturn = (equitiesPct * 0.09 + bondsPct * 0.04 + cashPct * 0.02)
        val portVol = Math.sqrt((equitiesPct / 100.0) * (equitiesPct / 100.0) * 0.18 * 0.18 +
                (bondsPct / 100.0) * (bondsPct / 100.0) * 0.06 * 0.06) * 100.0

        return PortfolioSnapshot(
            totalValue = totalPortfolioValue,
            items = items,
            concentrationHHI = hhi,
            diversificationRating = rating,
            expectedReturnPct = expReturn,
            portfolioVolatilityPct = portVol
        )
    }

    /**
     * Simulates an economic shock across asset classes:
     * e.g. Equity market crash (-20%), Flight-to-Safety bond rally (+5%), Cash unaffected (0%).
     */
    fun simulateMarketShock(
        equityShockPct: Double = -20.0,
        bondShockPct: Double = 5.0
    ): Pair<BigDecimal, BigDecimal> {
        val initialTotal = totalPortfolioValue
        val eqVal = initialTotal.multiply(BigDecimal.valueOf(equitiesPct / 100.0))
        val bdVal = initialTotal.multiply(BigDecimal.valueOf(bondsPct / 100.0))
        val csVal = initialTotal.multiply(BigDecimal.valueOf(cashPct / 100.0))

        val newEq = eqVal.multiply(BigDecimal.valueOf(1.0 + (equityShockPct / 100.0)))
        val newBd = bdVal.multiply(BigDecimal.valueOf(1.0 + (bondShockPct / 100.0)))
        val newCs = csVal // Cash remains constant

        val postShockTotal = newEq.add(newBd).add(newCs).setScale(2, RoundingMode.HALF_UP)
        val delta = postShockTotal.subtract(initialTotal)
        totalPortfolioValue = postShockTotal

        // Recalculate weights
        if (postShockTotal > BigDecimal.ZERO) {
            equitiesPct = newEq.divide(postShockTotal, 4, RoundingMode.HALF_UP).toDouble() * 100.0
            bondsPct = newBd.divide(postShockTotal, 4, RoundingMode.HALF_UP).toDouble() * 100.0
            cashPct = newCs.divide(postShockTotal, 4, RoundingMode.HALF_UP).toDouble() * 100.0
        }

        return Pair(postShockTotal, delta)
    }

    fun reset(initialCapital: BigDecimal = BigDecimal("10000.00")) {
        totalPortfolioValue = initialCapital
        equitiesPct = 50.0
        bondsPct = 30.0
        cashPct = 20.0
    }
}
