package com.trading.app.engine.risk

import com.trading.app.engine.core.MoneyMath
import java.math.BigDecimal
import java.math.RoundingMode

data class RiskMetrics(
    val potentialLoss: BigDecimal,
    val potentialGain: BigDecimal,
    val riskRewardRatio: Double,
    val lossPercentageOfAccount: Double,
    val maxDrawdownPct: Double,
    val exposurePct: Double,
    val isRiskAcceptable: Boolean,
    val recommendation: String
)

class RiskEngine(
    private val maxAllowedRiskPerTradePct: Double = 2.0 // Max 2% loss per trade rule
) {
    private var peakEquity: BigDecimal = BigDecimal.ZERO

    fun updatePeakEquity(equity: BigDecimal) {
        if (equity > peakEquity) {
            peakEquity = equity
        }
    }

    fun calculateRiskReward(
        accountBalance: BigDecimal,
        entryPrice: BigDecimal,
        stopLossPrice: BigDecimal,
        takeProfitPrice: BigDecimal,
        quantity: BigDecimal
    ): RiskMetrics {
        val perShareRisk = (entryPrice.subtract(stopLossPrice)).abs()
        val perShareGain = (takeProfitPrice.subtract(entryPrice)).abs()

        val potentialLoss = perShareRisk.multiply(quantity).setScale(2, RoundingMode.HALF_UP)
        val potentialGain = perShareGain.multiply(quantity).setScale(2, RoundingMode.HALF_UP)

        val riskRewardRatio = if (potentialLoss > BigDecimal.ZERO) {
            potentialGain.divide(potentialLoss, 2, RoundingMode.HALF_UP).toDouble()
        } else 0.0

        val lossPct = if (accountBalance > BigDecimal.ZERO) {
            potentialLoss.divide(accountBalance, 4, RoundingMode.HALF_UP).toDouble() * 100.0
        } else 0.0

        val totalCost = entryPrice.multiply(quantity)
        val exposurePct = if (accountBalance > BigDecimal.ZERO) {
            totalCost.divide(accountBalance, 4, RoundingMode.HALF_UP).toDouble() * 100.0
        } else 0.0

        val isRiskAcceptable = lossPct <= maxAllowedRiskPerTradePct && riskRewardRatio >= 1.5

        val recommendation = when {
            lossPct > maxAllowedRiskPerTradePct -> "DANGER: You are risking ${MoneyMath.formatPct(lossPct)} of total account! Strict risk rules mandate capping single-trade risk under ${maxAllowedRiskPerTradePct}%."
            riskRewardRatio < 1.0 -> "UNFAVORABLE: Potential gain (${MoneyMath.formatCurrency(potentialGain)}) is smaller than potential loss (${MoneyMath.formatCurrency(potentialLoss)}). Risk/Reward is 1:${riskRewardRatio}."
            riskRewardRatio in 1.0..1.5 -> "MARGINAL: Risk/Reward is 1:${riskRewardRatio}. Aim for at least 1:2.0 to maintain positive statistical expectancy."
            else -> "OPTIMAL: Well-sized position! 1:${riskRewardRatio} Risk/Reward risking only ${MoneyMath.formatPct(lossPct)} of account capital."
        }

        return RiskMetrics(
            potentialLoss = potentialLoss,
            potentialGain = potentialGain,
            riskRewardRatio = riskRewardRatio,
            lossPercentageOfAccount = lossPct,
            maxDrawdownPct = 0.0,
            exposurePct = exposurePct,
            isRiskAcceptable = isRiskAcceptable,
            recommendation = recommendation
        )
    }

    fun calculateDrawdown(currentEquity: BigDecimal): Double {
        updatePeakEquity(currentEquity)
        if (peakEquity <= BigDecimal.ZERO) return 0.0
        val drop = peakEquity.subtract(currentEquity)
        if (drop <= BigDecimal.ZERO) return 0.0
        return drop.divide(peakEquity, 4, RoundingMode.HALF_UP).toDouble() * 100.0
    }

    /**
     * Recommends exact quantity based on fixed fractional risk formula:
     * Quantity = (Account * RiskPct) / (Entry - Stop)
     */
    fun recommendPositionSize(
        accountBalance: BigDecimal,
        entryPrice: BigDecimal,
        stopLossPrice: BigDecimal,
        riskFraction: Double = 0.01 // 1%
    ): BigDecimal {
        val maxDollarRisk = accountBalance.multiply(BigDecimal.valueOf(riskFraction))
        val perShareRisk = (entryPrice.subtract(stopLossPrice)).abs()
        if (perShareRisk <= BigDecimal.ZERO) return BigDecimal.ONE
        return maxDollarRisk.divide(perShareRisk, 0, RoundingMode.DOWN)
    }

    fun reset() {
        peakEquity = BigDecimal.ZERO
    }
}
