package com.trading.app.engine.simulations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trading.app.engine.assessment.AssessmentEngine
import com.trading.app.engine.assessment.EvaluationResult
import com.trading.app.engine.core.MoneyMath
import com.trading.app.engine.interaction.ChartInteractionState
import com.trading.app.engine.interaction.DragTarget
import com.trading.app.engine.market.MarketDataEngine
import com.trading.app.engine.risk.RiskEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun RiskManagementSimulation(
    configJson: String?,
    onComplete: (earnedXp: Int) -> Unit,
    onBack: () -> Unit
) {
    val accountBalance = remember { BigDecimal("10000.00") }
    val marketEngine = remember { MarketDataEngine(startingPrice = BigDecimal("150.00")) }
    val riskEngine = remember { RiskEngine() }
    val assessmentEngine = remember { AssessmentEngine() }

    // Chart interaction state with entry at 150, stop-loss at 147, take-profit at 156
    val chartState = remember {
        ChartInteractionState(
            entryPrice = BigDecimal("150.00"),
            stopLossPrice = BigDecimal("147.00"),
            takeProfitPrice = BigDecimal("156.00")
        )
    }

    var quantity by remember { mutableFloatStateOf(30f) } // shares
    var isSimulatingTrade by remember { mutableStateOf(false) }
    var evaluationResult by remember { mutableStateOf<EvaluationResult?>(null) }
    val scope = rememberCoroutineScope()

    val candles = remember { marketEngine.candles }
    val minCandlePrice = remember(candles) { candles.minOf { it.low }.multiply(BigDecimal.valueOf(0.98)) }
    val maxCandlePrice = remember(candles) { candles.maxOf { it.high }.multiply(BigDecimal.valueOf(1.02)) }

    // Synchronize coordinate mapper bounds with candle price extent
    chartState.mapper.minPrice = minOf(minCandlePrice, BigDecimal("144.00"))
    chartState.mapper.maxPrice = maxOf(maxCandlePrice, BigDecimal("158.00"))

    // Calculate live risk metrics
    val stopPrice = chartState.stopLossPrice ?: BigDecimal("147.00")
    val tpPrice = chartState.takeProfitPrice ?: BigDecimal("156.00")
    val riskMetrics = remember(chartState.entryPrice, stopPrice, tpPrice, quantity) {
        riskEngine.calculateRiskReward(
            accountBalance = accountBalance,
            entryPrice = chartState.entryPrice,
            stopLossPrice = stopPrice,
            takeProfitPrice = tpPrice,
            quantity = BigDecimal.valueOf(quantity.toDouble())
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Top Navigation & Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF161B22), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "FINANCIAL RISK ENGINE",
                    color = Color(0xFF5FE2D6),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Risk/Reward & Stop Placement",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .background(Color(0x335FE2D6), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF5FE2D6), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("+50 XP", color = Color(0xFF5FE2D6), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Pedagogical Hint
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161B22), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF21262D), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TouchApp, contentDescription = null, tint = Color(0xFF5FE2D6), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Drag the GREEN line (Take-Profit) and RED line (Stop-Loss) directly on the chart below to design your risk profile.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Interactive Candlestick Chart with Drag-and-Drop Risk Lines
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(Color(0xFF0F172A), RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            chartState.onDragStart(offset)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            chartState.onDrag(dragAmount)
                        },
                        onDragEnd = {
                            chartState.onDragEnd()
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasW = size.width
                val canvasH = size.height

                chartState.mapper.canvasWidth = canvasW
                chartState.mapper.canvasHeight = canvasH

                // Draw price grid lines
                val priceStep = BigDecimal("2.00")
                var gridP = chartState.mapper.minPrice
                while (gridP <= chartState.mapper.maxPrice) {
                    val y = chartState.mapper.priceToY(gridP)
                    drawLine(
                        color = Color(0x1F334155),
                        start = Offset(0f, y),
                        end = Offset(canvasW - 55.dp.toPx(), y),
                        strokeWidth = 1.dp.toPx()
                    )
                    gridP = gridP.add(priceStep)
                }

                // Draw Candlesticks
                val candleCount = candles.size
                val candleWidth = 6.dp.toPx()
                for (i in candles.indices) {
                    val c = candles[i]
                    val x = chartState.mapper.indexToX(i, candleCount)
                    val highY = chartState.mapper.priceToY(c.high)
                    val lowY = chartState.mapper.priceToY(c.low)
                    val openY = chartState.mapper.priceToY(c.open)
                    val closeY = chartState.mapper.priceToY(c.close)

                    val isBull = c.close >= c.open
                    val candleColor = if (isBull) Color(0xFF22C55E) else Color(0xFFEF4444)

                    // Wick
                    drawLine(
                        color = candleColor,
                        start = Offset(x, highY),
                        end = Offset(x, lowY),
                        strokeWidth = 1.5.dp.toPx()
                    )

                    // Body
                    val bodyTop = minOf(openY, closeY)
                    val bodyHeight = (Math.abs(closeY - openY)).coerceAtLeast(2.dp.toPx())
                    drawRect(
                        color = candleColor,
                        topLeft = Offset(x - candleWidth / 2f, bodyTop),
                        size = Size(candleWidth, bodyHeight)
                    )
                }

                // Draw Interactive Take-Profit Line (Green)
                val tpY = chartState.mapper.priceToY(tpPrice)
                drawLine(
                    color = Color(0xFF22C55E),
                    start = Offset(0f, tpY),
                    end = Offset(canvasW, tpY),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f)
                )
                // TP Handle Pill
                drawCircle(
                    color = if (chartState.activeDragTarget == DragTarget.TAKE_PROFIT) Color.White else Color(0xFF22C55E),
                    radius = 8.dp.toPx(),
                    center = Offset(canvasW - 40.dp.toPx(), tpY)
                )

                // Draw Entry Price Line (Cyan)
                val entryY = chartState.mapper.priceToY(chartState.entryPrice)
                drawLine(
                    color = Color(0xFF5FE2D6),
                    start = Offset(0f, entryY),
                    end = Offset(canvasW, entryY),
                    strokeWidth = 1.5.dp.toPx()
                )

                // Draw Interactive Stop-Loss Line (Red)
                val stopY = chartState.mapper.priceToY(stopPrice)
                drawLine(
                    color = Color(0xFFEF4444),
                    start = Offset(0f, stopY),
                    end = Offset(canvasW, stopY),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f)
                )
                // Stop Handle Pill
                drawCircle(
                    color = if (chartState.activeDragTarget == DragTarget.STOP_LOSS) Color.White else Color(0xFFEF4444),
                    radius = 8.dp.toPx(),
                    center = Offset(canvasW - 40.dp.toPx(), stopY)
                )
            }

            // Price Labels Overlay
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(modifier = Modifier.background(Color(0xCC052E16), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("TP: \$${tpPrice.toPlainString()}", color = Color(0xFF4ADE80), fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Box(modifier = Modifier.background(Color(0xCC082F49), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("ENTRY: \$${chartState.entryPrice.toPlainString()}", color = Color(0xFF38BDF8), fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Box(modifier = Modifier.background(Color(0xCC450A0A), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("SL: \$${stopPrice.toPlainString()}", color = Color(0xFFF87171), fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Real-Time Risk/Reward Scoreboard
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161B22), RoundedCornerShape(14.dp))
                .border(1.dp, Color(0xFF21262D), RoundedCornerShape(14.dp))
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("RISK / REWARD", color = Color(0xFF8B949E), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "1 : ${riskMetrics.riskRewardRatio}",
                    color = if (riskMetrics.riskRewardRatio >= 1.8) Color(0xFF4ADE80) else Color(0xFFF87171),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Column {
                Text("POTENTIAL LOSS", color = Color(0xFF8B949E), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = MoneyMath.formatCurrency(riskMetrics.potentialLoss),
                    color = Color(0xFFF87171),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Column {
                Text("POTENTIAL GAIN", color = Color(0xFF8B949E), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = MoneyMath.formatCurrency(riskMetrics.potentialGain),
                    color = Color(0xFF4ADE80),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Column {
                Text("ACCOUNT RISK", color = Color(0xFF8B949E), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = MoneyMath.formatPct(riskMetrics.lossPercentageOfAccount),
                    color = if (riskMetrics.lossPercentageOfAccount <= 2.0) Color(0xFF4ADE80) else Color(0xFFF87171),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Position Sizing Controller
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161B22), RoundedCornerShape(14.dp))
                .border(1.dp, Color(0xFF30363D), RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Position Sizing (Quantity)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("${quantity.toInt()} shares (${MoneyMath.formatCurrency(chartState.entryPrice.multiply(BigDecimal.valueOf(quantity.toDouble())))})", color = Color(0xFF5FE2D6), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = quantity,
                    onValueChange = { quantity = it.toInt().toFloat() },
                    valueRange = 5f..100f,
                    steps = 19,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF5FE2D6),
                        activeTrackColor = Color(0xFF5FE2D6),
                        inactiveTrackColor = Color(0xFF30363D)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Real-Time Pedagogical Recommendation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (riskMetrics.isRiskAcceptable) Color(0x1F22C55E) else Color(0x1FEF4444),
                    RoundedCornerShape(12.dp)
                )
                .border(
                    1.dp,
                    if (riskMetrics.isRiskAcceptable) Color(0xFF22C55E) else Color(0xFFEF4444),
                    RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = if (riskMetrics.isRiskAcceptable) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (riskMetrics.isRiskAcceptable) Color(0xFF4ADE80) else Color(0xFFF87171),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = riskMetrics.recommendation,
                    color = Color(0xFFE2E8F0),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Submit & Simulate Trade Action
        Button(
            onClick = {
                scope.launch {
                    isSimulatingTrade = true
                    evaluationResult = null
                    delay(500)
                    val result = assessmentEngine.evaluateRiskTrade(
                        entryPrice = chartState.entryPrice,
                        stopLoss = stopPrice,
                        takeProfit = tpPrice,
                        quantity = BigDecimal.valueOf(quantity.toDouble()),
                        accountBalance = accountBalance
                    )
                    evaluationResult = result
                    isSimulatingTrade = false
                    if (result.isSuccessful) {
                        onComplete(result.xpAwarded)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = !isSimulatingTrade,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF5FE2D6),
                contentColor = Color(0xFF0D1117),
                disabledContainerColor = Color(0xFF21262D)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Simulate Trade Execution & Evaluate", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        // Post-Simulation Assessment Result Card
        AnimatedVisibility(visible = evaluationResult != null) {
            evaluationResult?.let { eval ->
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (eval.isSuccessful) Color(0x1F22C55E) else Color(0x1FEF4444),
                                RoundedCornerShape(14.dp)
                            )
                            .border(
                                1.dp,
                                if (eval.isSuccessful) Color(0xFF22C55E) else Color(0xFFEF4444),
                                RoundedCornerShape(14.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (eval.isSuccessful) Icons.Default.CheckCircle else Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (eval.isSuccessful) Color(0xFF4ADE80) else Color(0xFFF87171),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = eval.title,
                                    color = if (eval.isSuccessful) Color(0xFF4ADE80) else Color(0xFFF87171),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = eval.feedbackMessage,
                                color = Color(0xFFE2E8F0),
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0x33000000), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "💡 Mathematical Truth: ${eval.aiDeepDive}",
                                    color = Color(0xFF5FE2D6),
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
