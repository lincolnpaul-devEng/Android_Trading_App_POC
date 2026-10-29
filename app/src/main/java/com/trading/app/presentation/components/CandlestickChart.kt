package com.trading.app.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trading.app.domain.model.Candlestick
import com.trading.app.domain.model.Position
import com.trading.app.presentation.theme.*
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun CandlestickChart(
    candles: List<Candlestick>,
    modifier: Modifier = Modifier,
    openPosition: Position? = null,
    showEma20: Boolean = false,
    showEma50: Boolean = false,
    showBollingerBands: Boolean = false
) {
    if (candles.isEmpty()) return

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var crosshairIndex by remember { mutableIntStateOf(-1) }
    val textMeasurer = rememberTextMeasurer()

    val ema20 = remember(candles) { calculateEma(candles, 20) }
    val ema50 = remember(candles) { calculateEma(candles, 50) }
    val bollingerBands = remember(candles) { calculateBollingerBands(candles, 20, 2.0) }

    Column(modifier = modifier) {
        // Crosshair tooltip
        if (crosshairIndex in candles.indices) {
            val c = candles[crosshairIndex]
            CrosshairTooltip(c)
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(DarkCard)
                .pointerInput(candles.size) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 3f)
                        offsetX = (offsetX + pan.x).coerceIn(
                            -(candles.size * 12f * scale - size.width),
                            0f
                        )
                    }
                }
                .pointerInput(candles.size) {
                    detectTapGestures { offset ->
                        val candleWidth = 12f * scale
                        val index = ((offset.x - offsetX) / candleWidth).roundToInt()
                        crosshairIndex = index.coerceIn(candles.indices)
                    }
                }
        ) {
            drawCandlesticks(
                candles = candles,
                scale = scale,
                offsetX = offsetX,
                textMeasurer = textMeasurer,
                openPosition = openPosition,
                showEma20 = showEma20,
                showEma50 = showEma50,
                showBollingerBands = showBollingerBands,
                ema20 = ema20,
                ema50 = ema50,
                bollingerBands = bollingerBands
            )
        }

        // Volume bars
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                .background(DarkCard)
        ) {
            drawVolumeBars(candles, scale, offsetX)
        }
    }
}

private fun DrawScope.drawCandlesticks(
    candles: List<Candlestick>,
    scale: Float,
    offsetX: Float,
    textMeasurer: TextMeasurer,
    openPosition: Position?,
    showEma20: Boolean,
    showEma50: Boolean,
    showBollingerBands: Boolean,
    ema20: List<Double?>,
    ema50: List<Double?>,
    bollingerBands: List<BollingerBandPoint?>
) {
    val padding = 40f
    val chartHeight = size.height - padding * 2
    val candleWidth = 12f * scale
    val bodyWidth = 8f * scale

    val visibleStart = ((-offsetX) / candleWidth).toInt().coerceAtLeast(0)
    val visibleEnd = ((size.width - offsetX) / candleWidth).toInt().coerceAtMost(candles.size - 1)
    if (visibleStart > visibleEnd) return

    val visibleCandles = candles.subList(visibleStart, (visibleEnd + 1).coerceAtMost(candles.size))
    var allHigh = visibleCandles.maxOf { it.high }
    var allLow = visibleCandles.minOf { it.low }

    // Factor in position entry/tp/sl to keep them in chart view when close
    openPosition?.let { pos ->
        allHigh = maxOf(allHigh, pos.averageCost)
        allLow = minOf(allLow, pos.averageCost)
        pos.takeProfitPrice?.let { allHigh = maxOf(allHigh, it) }
        pos.stopLossPrice?.let { allLow = minOf(allLow, it) }
    }

    val priceRange = if (allHigh - allLow == 0.0) 1.0 else allHigh - allLow

    fun priceToY(price: Double): Float {
        return (padding + chartHeight * (1 - (price - allLow) / priceRange)).toFloat()
    }

    // Draw grid lines
    val gridLines = 5
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
    for (i in 0..gridLines) {
        val y = padding + (chartHeight / gridLines) * i
        drawLine(DarkBorder, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.5f, pathEffect = dashEffect)

        val price = allHigh - (priceRange / gridLines) * i
        val text = Formatters.formatPrice(price)
        val result = textMeasurer.measure(text, TextStyle(fontSize = 9.sp, color = TextTertiary))
        drawText(result, topLeft = Offset(size.width - result.size.width - 4, y - result.size.height / 2))
    }

    // 1. Draw Bollinger Bands Overlay if enabled
    if (showBollingerBands) {
        var lastUpper: Offset? = null
        var lastLower: Offset? = null
        var lastMid: Offset? = null

        for (i in visibleStart..visibleEnd) {
            val band = bollingerBands.getOrNull(i) ?: continue
            val x = i * candleWidth + offsetX + candleWidth / 2
            val currUpper = Offset(x, priceToY(band.upper))
            val currLower = Offset(x, priceToY(band.lower))
            val currMid = Offset(x, priceToY(band.middle))

            if (lastUpper != null && lastLower != null && lastMid != null) {
                val bandArea = Path().apply {
                    moveTo(lastUpper.x, lastUpper.y)
                    lineTo(currUpper.x, currUpper.y)
                    lineTo(currLower.x, currLower.y)
                    lineTo(lastLower.x, lastLower.y)
                    close()
                }
                drawPath(bandArea, color = Color(0xFF26A69A).copy(alpha = 0.08f))
                drawLine(Color(0xFF26A69A).copy(alpha = 0.5f), lastUpper, currUpper, strokeWidth = 1.2f)
                drawLine(Color(0xFF26A69A).copy(alpha = 0.35f), lastMid, currMid, strokeWidth = 1f, pathEffect = dashEffect)
                drawLine(Color(0xFF26A69A).copy(alpha = 0.5f), lastLower, currLower, strokeWidth = 1.2f)
            }
            lastUpper = currUpper
            lastLower = currLower
            lastMid = currMid
        }
    }

    // 2. Draw Candlestick Bodies & Wicks
    for (i in visibleStart..visibleEnd) {
        val candle = candles[i]
        val x = i * candleWidth + offsetX + candleWidth / 2
        val color = if (candle.isBullish) ChartGreen else ChartRed

        // Wick
        drawLine(
            color = color,
            start = Offset(x, priceToY(candle.high)),
            end = Offset(x, priceToY(candle.low)),
            strokeWidth = 1.5f
        )

        // Body
        val openY = priceToY(candle.open)
        val closeY = priceToY(candle.close)
        val bodyTop = minOf(openY, closeY)
        val bodyHeight = maxOf(abs(openY - closeY), 1f)

        drawRect(
            color = color,
            topLeft = Offset(x - bodyWidth / 2, bodyTop),
            size = Size(bodyWidth, bodyHeight)
        )
    }

    // 3. Draw EMA 20 Overlay (Yellow/Gold)
    if (showEma20) {
        var lastPt: Offset? = null
        for (i in visibleStart..visibleEnd) {
            val price = ema20.getOrNull(i) ?: continue
            val x = i * candleWidth + offsetX + candleWidth / 2
            val currPt = Offset(x, priceToY(price))
            if (lastPt != null) {
                drawLine(Color(0xFFF1E05A), lastPt, currPt, strokeWidth = 2f)
            }
            lastPt = currPt
        }
    }

    // 4. Draw EMA 50 Overlay (Cyan/Blue)
    if (showEma50) {
        var lastPt: Offset? = null
        for (i in visibleStart..visibleEnd) {
            val price = ema50.getOrNull(i) ?: continue
            val x = i * candleWidth + offsetX + candleWidth / 2
            val currPt = Offset(x, priceToY(price))
            if (lastPt != null) {
                drawLine(Color(0xFF58A6FF), lastPt, currPt, strokeWidth = 2f)
            }
            lastPt = currPt
        }
    }

    // 5. Draw Position Lines (Entry, TP, SL, Liq)
    openPosition?.let { pos ->
        fun drawLineWithTag(price: Double, color: Color, label: String) {
            val y = priceToY(price)
            drawLine(
                color = color,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
            )
            val tagText = "$label ${Formatters.formatPrice(price)}"
            val measured = textMeasurer.measure(
                tagText,
                TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
            )
            val bgWidth = measured.size.width + 12f
            val bgHeight = measured.size.height + 6f
            val tagX = size.width - bgWidth - 2f
            val tagY = y - bgHeight / 2
            drawRoundRect(
                color = color,
                topLeft = Offset(tagX, tagY),
                size = Size(bgWidth, bgHeight),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawText(measured, topLeft = Offset(tagX + 6f, tagY + 3f))
        }

        drawLineWithTag(pos.averageCost, Color(0xFF58A6FF), "ENTRY")
        pos.takeProfitPrice?.let { drawLineWithTag(it, Color(0xFF238636), "TP") }
        pos.stopLossPrice?.let { drawLineWithTag(it, Color(0xFFDA3633), "SL") }
        if (pos.liquidationPrice > 0.0) {
            drawLineWithTag(pos.liquidationPrice, Color(0xFFD29922), "LIQ")
        }
    }
}

// Indicator Calculation Functions
private fun calculateEma(candles: List<Candlestick>, period: Int): List<Double?> {
    if (candles.isEmpty()) return emptyList()
    val k = 2.0 / (period + 1.0)
    val emaList = ArrayList<Double?>(candles.size)
    var prevEma: Double? = null

    for (i in candles.indices) {
        val close = candles[i].close
        if (i < period - 1) {
            emaList.add(null)
        } else if (i == period - 1) {
            val sma = candles.subList(0, period).map { it.close }.average()
            prevEma = sma
            emaList.add(sma)
        } else {
            val currEma = close * k + (prevEma ?: close) * (1.0 - k)
            prevEma = currEma
            emaList.add(currEma)
        }
    }
    return emaList
}

data class BollingerBandPoint(val upper: Double, val middle: Double, val lower: Double)

private fun calculateBollingerBands(candles: List<Candlestick>, period: Int = 20, multiplier: Double = 2.0): List<BollingerBandPoint?> {
    if (candles.size < period) return candles.map { null }
    val result = ArrayList<BollingerBandPoint?>(candles.size)

    for (i in candles.indices) {
        if (i < period - 1) {
            result.add(null)
        } else {
            val slice = candles.subList(i - period + 1, i + 1).map { it.close }
            val sma = slice.average()
            val variance = slice.map { (it - sma).let { d -> d * d } }.average()
            val stdDev = sqrt(variance)
            result.add(
                BollingerBandPoint(
                    upper = sma + multiplier * stdDev,
                    middle = sma,
                    lower = sma - multiplier * stdDev
                )
            )
        }
    }
    return result
}

private fun DrawScope.drawVolumeBars(
    candles: List<Candlestick>,
    scale: Float,
    offsetX: Float
) {
    val candleWidth = 12f * scale
    val bodyWidth = 8f * scale

    val visibleStart = ((-offsetX) / candleWidth).toInt().coerceAtLeast(0)
    val visibleEnd = ((size.width - offsetX) / candleWidth).toInt().coerceAtMost(candles.size - 1)
    if (visibleStart > visibleEnd) return

    val visibleCandles = candles.subList(visibleStart, (visibleEnd + 1).coerceAtMost(candles.size))
    val maxVol = visibleCandles.maxOf { it.volume }.toFloat()
    if (maxVol == 0f) return

    for (i in visibleStart..visibleEnd) {
        val candle = candles[i]
        val x = i * candleWidth + offsetX + candleWidth / 2
        val color = if (candle.isBullish) ChartGreen.copy(alpha = 0.4f) else ChartRed.copy(alpha = 0.4f)
        val barHeight = (candle.volume / maxVol) * size.height * 0.85f

        drawRect(
            color = color,
            topLeft = Offset(x - bodyWidth / 2, size.height - barHeight),
            size = Size(bodyWidth, barHeight)
        )
    }
}

@Composable
private fun CrosshairTooltip(candle: Candlestick) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurfaceVariant)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val color = if (candle.isBullish) ProfitGreen else LossRed
        TooltipItem("O", Formatters.formatPrice(candle.open), color)
        TooltipItem("H", Formatters.formatPrice(candle.high), color)
        TooltipItem("L", Formatters.formatPrice(candle.low), color)
        TooltipItem("C", Formatters.formatPrice(candle.close), color)
        TooltipItem("Vol", Formatters.formatVolume(candle.volume), TextSecondary)
    }
}

@Composable
private fun TooltipItem(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "$label ", fontSize = 10.sp, color = TextTertiary)
        Text(text = value, fontSize = 11.sp, color = color)
    }
}
