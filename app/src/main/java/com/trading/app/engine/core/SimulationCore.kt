package com.trading.app.engine.core

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Standard deterministic simulation clock for reproducing market scenarios.
 */
class SimulationClock(
    var currentTimeMs: Long = 0L,
    var timeSpeedMultiplier: Float = 1.0f,
    var isRunning: Boolean = false
) {
    fun tick(deltaSeconds: Double) {
        if (!isRunning) return
        currentTimeMs += (deltaSeconds * 1000.0 * timeSpeedMultiplier).toLong()
    }

    fun reset(startTimeMs: Long = 0L) {
        currentTimeMs = startTimeMs
        isRunning = false
    }
}

/**
 * Declarative configuration for any financial simulation,
 * suitable for authoring dashboards and remote quiz publishing.
 */
data class SimulationConfig(
    val id: String = "sim_default",
    val title: String = "Market Simulation",
    val initialBalance: BigDecimal = BigDecimal("10000.00"),
    val assetSymbol: String = "AAPL",
    val startingPrice: BigDecimal = BigDecimal("150.00"),
    val spreadBps: Double = 5.0, // 0.05%
    val commissionPerTrade: BigDecimal = BigDecimal("1.00"),
    val annualVolatility: Double = 0.25, // 25% annualized volatility
    val parameters: Map<String, String> = emptyMap()
)

enum class TradeDirection {
    BUY,
    SELL
}

enum class OrderType {
    MARKET,
    LIMIT,
    STOP
}

data class Position(
    val id: String,
    val asset: String,
    val direction: TradeDirection,
    val entryPrice: BigDecimal,
    val currentPrice: BigDecimal,
    val quantity: BigDecimal,
    val stopLoss: BigDecimal?,
    val takeProfit: BigDecimal?,
    val unrealizedPnl: BigDecimal,
    val realizedPnl: BigDecimal = BigDecimal.ZERO
) {
    val totalCost: BigDecimal
        get() = entryPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP)
}

/**
 * High precision monetary helper to prevent float drift in settlement math.
 */
object MoneyMath {
    fun bd(value: Double): BigDecimal = BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP)
    fun bd(value: String): BigDecimal = BigDecimal(value).setScale(4, RoundingMode.HALF_UP)
    fun formatCurrency(amount: BigDecimal): String = "$" + amount.setScale(2, RoundingMode.HALF_UP).toPlainString()
    fun formatPct(pct: Double): String = String.format("%.2f%%", pct)
}

/**
 * Base sealed interface for simulation states.
 */
interface SimulationState {
    val timestampMs: Long
    val accountBalance: BigDecimal
    val availableBalance: BigDecimal
    val equity: BigDecimal
    val totalPnl: BigDecimal
}

/**
 * User actions that can be dispatched to any financial simulation.
 */
sealed class SimulationAction {
    data class PlaceOrder(
        val asset: String,
        val direction: TradeDirection,
        val orderType: OrderType,
        val quantity: BigDecimal,
        val targetPrice: BigDecimal? = null,
        val stopLoss: BigDecimal? = null,
        val takeProfit: BigDecimal? = null
    ) : SimulationAction()

    data class AdjustRiskMarkers(
        val positionId: String,
        val stopLoss: BigDecimal?,
        val takeProfit: BigDecimal?
    ) : SimulationAction()

    data class UpdateAssetAllocation(
        val equitiesPct: Double,
        val bondsPct: Double,
        val cashPct: Double
    ) : SimulationAction()

    data class ApplyMarketShock(
        val shockMagnitudePct: Double, // e.g. -15.0 for -15% crash
        val shockDescription: String
    ) : SimulationAction()

    data class AdvanceTime(val deltaTimeSeconds: Double) : SimulationAction()
    data object Reset : SimulationAction()
    data object TogglePlayPause : SimulationAction()
}

/**
 * Result returned after processing an action.
 */
sealed class SimulationResult {
    data class Success(val message: String, val earnedXp: Int = 0) : SimulationResult()
    data class OrderFilled(val position: Position, val fillPrice: BigDecimal) : SimulationResult()
    data class Error(val reason: String) : SimulationResult()
    data class ObjectiveCompleted(val title: String, val xpReward: Int, val explanation: String) : SimulationResult()
}

/**
 * Standard Financial Simulation Interface recommended by architecture specification.
 */
interface FinancialSimulation {
    fun initialize(config: SimulationConfig)
    fun start()
    fun pause()
    fun reset()
    fun advance(deltaTime: Double)
    fun getState(): SimulationState
    fun processAction(action: SimulationAction): SimulationResult
}
