package com.trading.app.engine.assessment

import com.trading.app.engine.core.MoneyMath
import java.math.BigDecimal

data class EvaluationResult(
    val isSuccessful: Boolean,
    val xpAwarded: Int,
    val title: String,
    val feedbackMessage: String,
    val aiDeepDive: String
)

class AssessmentEngine {

    /**
     * Evaluates a risk management trade entry.
     */
    fun evaluateRiskTrade(
        entryPrice: BigDecimal,
        stopLoss: BigDecimal,
        takeProfit: BigDecimal,
        quantity: BigDecimal,
        accountBalance: BigDecimal
    ): EvaluationResult {
        val perShareRisk = (entryPrice.subtract(stopLoss)).abs()
        val perShareGain = (takeProfit.subtract(entryPrice)).abs()
        val totalRisk = perShareRisk.multiply(quantity)
        val riskPct = totalRisk.divide(accountBalance, 4, java.math.RoundingMode.HALF_UP).toDouble() * 100.0
        val rr = if (perShareRisk > BigDecimal.ZERO) perShareGain.divide(perShareRisk, 2, java.math.RoundingMode.HALF_UP).toDouble() else 0.0

        return if (riskPct <= 2.0 && rr >= 1.8) {
            EvaluationResult(
                isSuccessful = true,
                xpAwarded = 50,
                title = "Optimal Trade Architecture! (+50 XP)",
                feedbackMessage = "You maintained a 1:${rr} Risk-to-Reward ratio while limiting risk to ${MoneyMath.formatPct(riskPct)} of capital. Professional prop traders live by this rule to remain profitable even with a 40% win rate.",
                aiDeepDive = "Statistical Expectancy = (Win% * AvgWin) - (Loss% * AvgLoss). With a 2:1 ratio, winning just 40 trades out of 100 yields positive portfolio growth."
            )
        } else if (riskPct > 2.0) {
            EvaluationResult(
                isSuccessful = false,
                xpAwarded = 10,
                title = "Excessive Position Risk (+10 XP)",
                feedbackMessage = "Your potential loss is ${MoneyMath.formatPct(riskPct)} of the account. A string of 3 bad trades could produce catastrophic drawdown. Reduce position size or tighten stop-loss.",
                aiDeepDive = "Drawdown math is non-linear: A 50% account loss requires a 100% gain just to breakeven. That is why capital preservation precedes capital growth."
            )
        } else {
            EvaluationResult(
                isSuccessful = false,
                xpAwarded = 15,
                title = "Sub-optimal Risk/Reward (+15 XP)",
                feedbackMessage = "Your Risk/Reward ratio is 1:${rr}. When risk exceeds or equals reward, you need an unrealistically high win rate (>65%) to survive market fees and slippage.",
                aiDeepDive = "Aim for targets where the profit potential is at least double your predefined loss boundary."
            )
        }
    }

    /**
     * Evaluates a portfolio diversification decision before/after market shock.
     */
    fun evaluatePortfolioDiversification(
        equitiesPct: Double,
        bondsPct: Double,
        cashPct: Double,
        portfolioDrawdownPct: Double
    ): EvaluationResult {
        val isBalanced = equitiesPct in 35.0..65.0 && bondsPct in 15.0..40.0 && cashPct in 10.0..30.0

        return if (isBalanced && portfolioDrawdownPct < 12.0) {
            EvaluationResult(
                isSuccessful = true,
                xpAwarded = 45,
                title = "Diversification Shield Active! (+45 XP)",
                feedbackMessage = "During a -20% equity crash, your portfolio only experienced a ${String.format("%.1f", portfolioDrawdownPct)}% drawdown. The bond flight-to-safety rally and cash cushion successfully absorbed the shock.",
                aiDeepDive = "Harry Markowitz (Nobel Laureate) termed diversification 'the only free lunch in finance' because combining uncorrelated assets lowers volatility without sacrificing long-term compounding proportionally."
            )
        } else if (equitiesPct > 80.0) {
            EvaluationResult(
                isSuccessful = false,
                xpAwarded = 15,
                title = "Severe Concentration Drawdown (+15 XP)",
                feedbackMessage = "With ${equitiesPct.toInt()}% in equities, the market shock inflicted a painful ${String.format("%.1f", portfolioDrawdownPct)}% loss. Concentration creates extreme vulnerability during liquidity freezes.",
                aiDeepDive = "Holding non-correlated assets (negative or near-zero covariance) dampens portfolio variance."
            )
        } else {
            EvaluationResult(
                isSuccessful = true,
                xpAwarded = 30,
                title = "Portfolio Survives (+30 XP)",
                feedbackMessage = "Portfolio absorbed the shock with ${String.format("%.1f", portfolioDrawdownPct)}% loss. Experiment with adjusting cash and bonds to find your personal risk tolerance sweet spot.",
                aiDeepDive = "Asset allocation dictates over 90% of a portfolio's return variation over a 10-year horizon."
            )
        }
    }
}
