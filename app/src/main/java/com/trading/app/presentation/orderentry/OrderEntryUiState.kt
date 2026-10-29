package com.trading.app.presentation.orderentry

import com.trading.app.domain.model.*
import com.trading.app.presentation.components.AutoCloseMode
import kotlin.math.abs

data class OrderEntryUiState(
    val symbol: String = "",
    val companyName: String = "",
    val currentPrice: Double = 0.0,
    val bidPrice: Double = 0.0,
    val askPrice: Double = 0.0,
    val side: OrderSide = OrderSide.BUY,
    val type: OrderType = OrderType.MARKET,
    val timeInForce: TimeInForce = TimeInForce.DAY,
    val quantity: String = "",
    val limitPrice: String = "",
    val stopPrice: String = "",
    val leverage: Int = 1,
    val takeProfit: String = "",
    val stopLoss: String = "",
    val autoCloseMode: AutoCloseMode = AutoCloseMode.PERCENT_PNL,
    val showAutoCloseSheet: Boolean = false,
    val buyingPower: Double = 0.0,
    val currentPosition: Int = 0,
    val showConfirmation: Boolean = false,
    val orderResult: String? = null,
    val specs: InstrumentSpecs = InstrumentSpecs(),
    val error: String? = null
) {
    val feeRate: Double = 0.00095 // 0.095% fee (matching reference design)

    val assetClass: AssetClass get() = specs.assetClass
    val multiplier: Double get() = specs.contractMultiplier
    val spread: Double get() = (askPrice - bidPrice).coerceAtLeast(0.0)

    val effectivePrice: Double get() = when (type) {
        OrderType.MARKET -> currentPrice
        OrderType.LIMIT, OrderType.STOP_LIMIT -> limitPrice.toDoubleOrNull() ?: currentPrice
        else -> currentPrice
    }

    val estimatedCost: Double get() {
        val qty = quantity.toIntOrNull() ?: 0
        return qty * effectivePrice * multiplier
    }

    val requiredMargin: Double get() {
        val lev = maxOf(1, leverage)
        return estimatedCost / lev
    }

    val estimatedFee: Double get() = estimatedCost * feeRate

    val isShort: Boolean get() = side == OrderSide.SELL

    val estimatedLiquidationPrice: Double? get() {
        if (leverage <= 1 || effectivePrice <= 0.0) return null
        val mmr = (1.0 / (specs.maxLeverage * 2.0)).coerceAtLeast(0.02)
        return if (isShort) {
            effectivePrice * (1.0 + (1.0 / leverage) - mmr)
        } else {
            (effectivePrice * (1.0 - (1.0 / leverage) + mmr)).coerceAtLeast(0.0)
        }
    }

    val calculatedTpPrice: Double? get() {
        val tpVal = takeProfit.toDoubleOrNull() ?: return null
        val lev = maxOf(1, leverage)
        return if (autoCloseMode == AutoCloseMode.PERCENT_PNL) {
            val deltaPct = tpVal / (100.0 * lev)
            if (isShort) effectivePrice * (1.0 - deltaPct) else effectivePrice * (1.0 + deltaPct)
        } else {
            tpVal
        }
    }

    val calculatedSlPrice: Double? get() {
        val slVal = stopLoss.toDoubleOrNull() ?: return null
        val lev = maxOf(1, leverage)
        return if (autoCloseMode == AutoCloseMode.PERCENT_PNL) {
            val deltaPct = abs(slVal) / (100.0 * lev)
            if (isShort) effectivePrice * (1.0 + deltaPct) else effectivePrice * (1.0 - deltaPct)
        } else {
            slVal
        }
    }

    val riskPerUnit: Double? get() {
        val sl = calculatedSlPrice ?: return null
        return abs(effectivePrice - sl) * multiplier
    }

    val rewardPerUnit: Double? get() {
        val tp = calculatedTpPrice ?: return null
        return abs(effectivePrice - tp) * multiplier
    }

    val totalRiskDollars: Double? get() {
        val qty = quantity.toIntOrNull() ?: return null
        return riskPerUnit?.let { it * qty }
    }

    val totalRewardDollars: Double? get() {
        val qty = quantity.toIntOrNull() ?: return null
        return rewardPerUnit?.let { it * qty }
    }

    val riskRewardRatio: Double? get() {
        val r = riskPerUnit ?: return null
        val w = rewardPerUnit ?: return null
        return if (r > 0.0) w / r else null
    }

    val riskRewardRatioDisplay: String? get() = riskRewardRatio?.let {
        String.format(java.util.Locale.US, "1 : %.1f", it)
    }

    val hasAutoClose: Boolean get() = calculatedTpPrice != null || calculatedSlPrice != null
}
