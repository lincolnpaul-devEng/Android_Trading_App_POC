package com.trading.app.engine.interaction

import androidx.compose.ui.geometry.Offset
import java.math.BigDecimal
import java.math.RoundingMode

enum class DragTarget {
    NONE,
    ENTRY_PRICE,
    STOP_LOSS,
    TAKE_PROFIT
}

/**
 * Robust Coordinate Mapper converting screen pixels to financial prices and back:
 * P(y) = P_max - ((y - y_top) / H) * (P_max - P_min)
 */
class CoordinateMapper(
    var minPrice: BigDecimal = BigDecimal("140.00"),
    var maxPrice: BigDecimal = BigDecimal("160.00"),
    var canvasWidth: Float = 0f,
    var canvasHeight: Float = 0f,
    var topPadding: Float = 24f,
    var bottomPadding: Float = 36f,
    var rightPadding: Float = 60f
) {
    val drawableHeight: Float
        get() = (canvasHeight - topPadding - bottomPadding).coerceAtLeast(1f)

    val drawableWidth: Float
        get() = (canvasWidth - rightPadding).coerceAtLeast(1f)

    val priceRange: BigDecimal
        get() = maxPrice.subtract(minPrice).let { if (it <= BigDecimal.ZERO) BigDecimal.ONE else it }

    /**
     * Converts a financial price to screen pixel Y coordinate.
     */
    fun priceToY(price: BigDecimal): Float {
        val fraction = maxPrice.subtract(price).divide(priceRange, 6, RoundingMode.HALF_UP).toFloat()
        return topPadding + (fraction * drawableHeight).coerceIn(0f, drawableHeight)
    }

    /**
     * Converts screen pixel Y coordinate to financial price (clamped between min and max price).
     */
    fun yToPrice(y: Float): BigDecimal {
        val clampedY = (y - topPadding).coerceIn(0f, drawableHeight)
        val fraction = BigDecimal.valueOf((clampedY / drawableHeight).toDouble())
        val delta = priceRange.multiply(fraction)
        return maxPrice.subtract(delta).setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * Converts candle index to screen pixel X coordinate.
     */
    fun indexToX(index: Int, totalCount: Int): Float {
        if (totalCount <= 1) return drawableWidth / 2f
        val step = drawableWidth / (totalCount - 1).toFloat()
        return index * step
    }
}

/**
 * Hit Test Engine testing pointer proximity to interactive chart handles.
 */
class HitTestEngine(
    private val touchThresholdPx: Float = 48f
) {
    fun hitTest(
        touchY: Float,
        entryY: Float,
        stopLossY: Float?,
        takeProfitY: Float?
    ): DragTarget {
        takeProfitY?.let {
            if (Math.abs(touchY - it) <= touchThresholdPx) return DragTarget.TAKE_PROFIT
        }
        stopLossY?.let {
            if (Math.abs(touchY - it) <= touchThresholdPx) return DragTarget.STOP_LOSS
        }
        if (Math.abs(touchY - entryY) <= touchThresholdPx) {
            return DragTarget.ENTRY_PRICE
        }
        return DragTarget.NONE
    }
}

/**
 * Central State & Controller for drag-and-drop on financial charts.
 */
class ChartInteractionState(
    var entryPrice: BigDecimal = BigDecimal("150.00"),
    var stopLossPrice: BigDecimal? = BigDecimal("147.00"),
    var takeProfitPrice: BigDecimal? = BigDecimal("156.00")
) {
    var activeDragTarget: DragTarget = DragTarget.NONE
        private set

    val mapper = CoordinateMapper()
    val hitTester = HitTestEngine()

    fun onDragStart(offset: Offset) {
        val entryY = mapper.priceToY(entryPrice)
        val stopY = stopLossPrice?.let { mapper.priceToY(it) }
        val tpY = takeProfitPrice?.let { mapper.priceToY(it) }

        activeDragTarget = hitTester.hitTest(offset.y, entryY, stopY, tpY)
    }

    fun onDrag(dragAmount: Offset) {
        if (activeDragTarget == DragTarget.NONE) return

        when (activeDragTarget) {
            DragTarget.STOP_LOSS -> {
                val currentY = stopLossPrice?.let { mapper.priceToY(it) } ?: return
                val newPrice = mapper.yToPrice(currentY + dragAmount.y)
                // Stop-loss cannot exceed entry price
                if (newPrice < entryPrice) {
                    stopLossPrice = newPrice
                }
            }
            DragTarget.TAKE_PROFIT -> {
                val currentY = takeProfitPrice?.let { mapper.priceToY(it) } ?: return
                val newPrice = mapper.yToPrice(currentY + dragAmount.y)
                // Take-profit cannot be below entry price
                if (newPrice > entryPrice) {
                    takeProfitPrice = newPrice
                }
            }
            DragTarget.ENTRY_PRICE -> {
                val currentY = mapper.priceToY(entryPrice)
                val newPrice = mapper.yToPrice(currentY + dragAmount.y)
                entryPrice = newPrice
            }
            DragTarget.NONE -> Unit
        }
    }

    fun onDragEnd() {
        activeDragTarget = DragTarget.NONE
    }
}
