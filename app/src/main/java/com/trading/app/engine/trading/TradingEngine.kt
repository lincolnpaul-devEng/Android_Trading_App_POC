package com.trading.app.engine.trading

import com.trading.app.engine.core.MoneyMath
import com.trading.app.engine.core.OrderType
import com.trading.app.engine.core.Position
import com.trading.app.engine.core.TradeDirection
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

data class OrderExecutionResult(
    val success: Boolean,
    val fillPrice: BigDecimal,
    val executedQuantity: BigDecimal,
    val commissionPaid: BigDecimal,
    val slippagePaid: BigDecimal,
    val message: String
)

class TradingEngine(
    initialBalance: BigDecimal = BigDecimal("10000.00"),
    private val commissionPerTrade: BigDecimal = BigDecimal("1.00"),
    private val slippageRateBps: Double = 2.0 // 0.02% simulated slippage
) {
    var cashBalance: BigDecimal = initialBalance
        private set

    private val _openPositions = mutableListOf<Position>()
    val openPositions: List<Position> get() = _openPositions.toList()

    private val _closedPositions = mutableListOf<Position>()
    val closedPositions: List<Position> get() = _closedPositions.toList()

    var realizedPnl: BigDecimal = BigDecimal.ZERO
        private set

    fun getUnrealizedPnl(currentMarketPrice: BigDecimal): BigDecimal {
        return _openPositions.fold(BigDecimal.ZERO) { acc, pos ->
            val pnl = calculatePnl(pos, currentMarketPrice)
            acc.add(pnl)
        }.setScale(2, RoundingMode.HALF_UP)
    }

    fun getEquity(currentMarketPrice: BigDecimal): BigDecimal {
        return cashBalance.add(getUnrealizedPnl(currentMarketPrice))
            .add(getTotalPositionCost())
            .setScale(2, RoundingMode.HALF_UP)
    }

    fun getTotalPositionCost(): BigDecimal {
        return _openPositions.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.totalCost) }
    }

    fun executeOrder(
        asset: String,
        direction: TradeDirection,
        orderType: OrderType,
        quantity: BigDecimal,
        bestBid: BigDecimal,
        bestAsk: BigDecimal,
        stopLoss: BigDecimal? = null,
        takeProfit: BigDecimal? = null
    ): OrderExecutionResult {
        // Long orders cross the spread to buy at ask; Short orders cross the spread to sell at bid
        val rawPrice = if (direction == TradeDirection.BUY) bestAsk else bestBid
        val slippage = rawPrice.multiply(BigDecimal.valueOf(slippageRateBps / 10000.0)).setScale(4, RoundingMode.HALF_UP)
        val fillPrice = if (direction == TradeDirection.BUY) rawPrice.add(slippage) else rawPrice.subtract(slippage)
        val totalCost = fillPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP)
        val totalDebit = totalCost.add(commissionPerTrade)

        if (direction == TradeDirection.BUY) {
            if (cashBalance < totalDebit) {
                return OrderExecutionResult(
                    success = false,
                    fillPrice = fillPrice,
                    executedQuantity = BigDecimal.ZERO,
                    commissionPaid = BigDecimal.ZERO,
                    slippagePaid = BigDecimal.ZERO,
                    message = "Insufficient cash balance. Required: ${MoneyMath.formatCurrency(totalDebit)}, Available: ${MoneyMath.formatCurrency(cashBalance)}"
                )
            }
            cashBalance = cashBalance.subtract(totalDebit).setScale(2, RoundingMode.HALF_UP)
            val newPosition = Position(
                id = UUID.randomUUID().toString(),
                asset = asset,
                direction = direction,
                entryPrice = fillPrice,
                currentPrice = fillPrice,
                quantity = quantity,
                stopLoss = stopLoss,
                takeProfit = takeProfit,
                unrealizedPnl = BigDecimal.ZERO
            )
            _openPositions.add(newPosition)
            return OrderExecutionResult(
                success = true,
                fillPrice = fillPrice,
                executedQuantity = quantity,
                commissionPaid = commissionPerTrade,
                slippagePaid = slippage.multiply(quantity).setScale(2, RoundingMode.HALF_UP),
                message = "Bought ${quantity.toPlainString()} $asset @ ${MoneyMath.formatCurrency(fillPrice)}"
            )
        } else {
            // Sell / Short
            if (cashBalance < totalCost.multiply(BigDecimal.valueOf(0.2))) { // 20% margin requirement
                return OrderExecutionResult(
                    success = false,
                    fillPrice = fillPrice,
                    executedQuantity = BigDecimal.ZERO,
                    commissionPaid = BigDecimal.ZERO,
                    slippagePaid = BigDecimal.ZERO,
                    message = "Insufficient margin to open short position."
                )
            }
            cashBalance = cashBalance.subtract(commissionPerTrade)
            val newPosition = Position(
                id = UUID.randomUUID().toString(),
                asset = asset,
                direction = direction,
                entryPrice = fillPrice,
                currentPrice = fillPrice,
                quantity = quantity,
                stopLoss = stopLoss,
                takeProfit = takeProfit,
                unrealizedPnl = BigDecimal.ZERO
            )
            _openPositions.add(newPosition)
            return OrderExecutionResult(
                success = true,
                fillPrice = fillPrice,
                executedQuantity = quantity,
                commissionPaid = commissionPerTrade,
                slippagePaid = slippage.multiply(quantity).setScale(2, RoundingMode.HALF_UP),
                message = "Sold ${quantity.toPlainString()} $asset @ ${MoneyMath.formatCurrency(fillPrice)}"
            )
        }
    }

    fun updateStopLossAndTakeProfit(positionId: String, stopLoss: BigDecimal?, takeProfit: BigDecimal?) {
        val index = _openPositions.indexOfFirst { it.id == positionId }
        if (index != -1) {
            val p = _openPositions[index]
            _openPositions[index] = p.copy(stopLoss = stopLoss, takeProfit = takeProfit)
        }
    }

    /**
     * Checks if current market price has hit Stop-Loss or Take-Profit for any position.
     * Returns list of closed positions with trigger rationale.
     */
    fun evaluateRiskTriggers(currentPrice: BigDecimal): List<String> {
        val triggeredMessages = mutableListOf<String>()
        val iterator = _openPositions.iterator()

        while (iterator.hasNext()) {
            val pos = iterator.next()
            var shouldClose = false
            var reason = ""

            if (pos.direction == TradeDirection.BUY) {
                if (pos.stopLoss != null && currentPrice <= pos.stopLoss) {
                    shouldClose = true
                    reason = "Stop-Loss triggered at ${MoneyMath.formatCurrency(currentPrice)} (limit: ${MoneyMath.formatCurrency(pos.stopLoss)})"
                } else if (pos.takeProfit != null && currentPrice >= pos.takeProfit) {
                    shouldClose = true
                    reason = "Take-Profit target hit at ${MoneyMath.formatCurrency(currentPrice)} (target: ${MoneyMath.formatCurrency(pos.takeProfit)})"
                }
            } else {
                if (pos.stopLoss != null && currentPrice >= pos.stopLoss) {
                    shouldClose = true
                    reason = "Short Stop-Loss triggered at ${MoneyMath.formatCurrency(currentPrice)}"
                } else if (pos.takeProfit != null && currentPrice <= pos.takeProfit) {
                    shouldClose = true
                    reason = "Short Take-Profit target hit at ${MoneyMath.formatCurrency(currentPrice)}"
                }
            }

            if (shouldClose) {
                iterator.remove()
                val pnl = calculatePnl(pos, currentPrice)
                val returnedCapital = pos.totalCost.add(pnl).subtract(commissionPerTrade)
                cashBalance = cashBalance.add(returnedCapital).setScale(2, RoundingMode.HALF_UP)
                realizedPnl = realizedPnl.add(pnl).setScale(2, RoundingMode.HALF_UP)
                _closedPositions.add(pos.copy(currentPrice = currentPrice, unrealizedPnl = BigDecimal.ZERO, realizedPnl = pnl))
                triggeredMessages.add(reason)
            }
        }
        return triggeredMessages
    }

    private fun calculatePnl(pos: Position, currentPrice: BigDecimal): BigDecimal {
        val delta = if (pos.direction == TradeDirection.BUY) {
            currentPrice.subtract(pos.entryPrice)
        } else {
            pos.entryPrice.subtract(currentPrice)
        }
        return delta.multiply(pos.quantity).setScale(2, RoundingMode.HALF_UP)
    }

    fun reset(initialBalance: BigDecimal) {
        cashBalance = initialBalance
        realizedPnl = BigDecimal.ZERO
        _openPositions.clear()
        _closedPositions.clear()
    }
}
