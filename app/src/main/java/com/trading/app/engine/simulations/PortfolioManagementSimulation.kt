package com.trading.app.engine.simulations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trading.app.engine.assessment.AssessmentEngine
import com.trading.app.engine.assessment.EvaluationResult
import com.trading.app.engine.core.MoneyMath
import com.trading.app.engine.portfolio.PortfolioEngine
import java.math.BigDecimal

@Composable
fun PortfolioManagementSimulation(
    configJson: String?,
    onComplete: (earnedXp: Int) -> Unit,
    onBack: () -> Unit
) {
    val initialCapital = remember { BigDecimal("10000.00") }
    val portfolioEngine = remember { PortfolioEngine(initialCapital) }
    val assessmentEngine = remember { AssessmentEngine() }

    var equitiesSlider by remember { mutableFloatStateOf(50f) }
    var bondsSlider by remember { mutableFloatStateOf(30f) }
    var cashSlider by remember { mutableFloatStateOf(20f) }

    portfolioEngine.setAllocation(equitiesSlider.toDouble(), bondsSlider.toDouble(), cashSlider.toDouble())
    val snapshot = portfolioEngine.getSnapshot()

    var hasSimulatedShock by remember { mutableStateOf(false) }
    var postShockPortfolioValue by remember { mutableStateOf(initialCapital) }
    var postShockDelta by remember { mutableStateOf(BigDecimal.ZERO) }
    var evaluationResult by remember { mutableStateOf<EvaluationResult?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Navigation Header
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
                    text = "PORTFOLIO ENGINE",
                    color = Color(0xFF5FE2D6),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Asset Allocation & Diversification",
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
                Text("+45 XP", color = Color(0xFF5FE2D6), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Portfolio Total Value Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161B22), RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFF21262D), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Text("HYPOTHETICAL PORTFOLIO VALUE", color = Color(0xFF8B949E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (!hasSimulatedShock) MoneyMath.formatCurrency(snapshot.totalValue) else MoneyMath.formatCurrency(postShockPortfolioValue),
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    if (hasSimulatedShock) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "${if (postShockDelta >= BigDecimal.ZERO) "+" else ""}${MoneyMath.formatCurrency(postShockDelta)}",
                            color = if (postShockDelta >= BigDecimal.ZERO) Color(0xFF4ADE80) else Color(0xFFF87171),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Canvas Stacked Allocation Bar
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                ) {
                    val w = size.width
                    val h = size.height

                    val eqW = (snapshot.items[0].allocationPct / 100.0).toFloat() * w
                    val bdW = (snapshot.items[1].allocationPct / 100.0).toFloat() * w
                    val csW = (snapshot.items[2].allocationPct / 100.0).toFloat() * w

                    drawRect(color = Color(0xFF5FE2D6), topLeft = Offset(0f, 0f), size = Size(eqW, h))
                    drawRect(color = Color(0xFF8B5CF6), topLeft = Offset(eqW, 0f), size = Size(bdW, h))
                    drawRect(color = Color(0xFF4ADE80), topLeft = Offset(eqW + bdW, 0f), size = Size(csW, h))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Class breakdown row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    snapshot.items.forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(Color(item.colorHex), CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${item.assetClass} ${item.allocationPct.toInt()}%", color = Color(0xFFCBD5E1), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Allocation Sliders
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161B22), RoundedCornerShape(14.dp))
                .border(1.dp, Color(0xFF30363D), RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Text("Adjust Asset Allocation", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(8.dp))

            // Equities
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Equities (High Growth, High Risk)", color = Color(0xFF5FE2D6), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("${equitiesSlider.toInt()}%", color = Color(0xFF5FE2D6), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = equitiesSlider,
                onValueChange = {
                    equitiesSlider = it
                    hasSimulatedShock = false
                },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(thumbColor = Color(0xFF5FE2D6), activeTrackColor = Color(0xFF5FE2D6), inactiveTrackColor = Color(0xFF30363D))
            )

            // Bonds
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Bonds (Fixed Income, Low Risk)", color = Color(0xFF8B5CF6), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("${bondsSlider.toInt()}%", color = Color(0xFF8B5CF6), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = bondsSlider,
                onValueChange = {
                    bondsSlider = it
                    hasSimulatedShock = false
                },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(thumbColor = Color(0xFF8B5CF6), activeTrackColor = Color(0xFF8B5CF6), inactiveTrackColor = Color(0xFF30363D))
            )

            // Cash
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Cash (Zero Risk, Inflation Vulnerable)", color = Color(0xFF4ADE80), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("${cashSlider.toInt()}%", color = Color(0xFF4ADE80), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = cashSlider,
                onValueChange = {
                    cashSlider = it
                    hasSimulatedShock = false
                },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(thumbColor = Color(0xFF4ADE80), activeTrackColor = Color(0xFF4ADE80), inactiveTrackColor = Color(0xFF30363D))
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Diversification Assessment Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161B22), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF21262D), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PieChart, contentDescription = null, tint = Color(0xFF5FE2D6), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(snapshot.diversificationRating, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Expected Annual Return: ${String.format("%.1f", snapshot.expectedReturnPct)}% | Volatility: ±${String.format("%.1f", snapshot.portfolioVolatilityPct)}%",
                    color = Color(0xFF8B949E),
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Market Shock Action Button
        Button(
            onClick = {
                val (postVal, delta) = portfolioEngine.simulateMarketShock(equityShockPct = -20.0, bondShockPct = 5.0)
                postShockPortfolioValue = postVal
                postShockDelta = delta
                hasSimulatedShock = true

                val drawdownPct = (Math.abs(delta.toDouble()) / initialCapital.toDouble()) * 100.0
                val result = assessmentEngine.evaluatePortfolioDiversification(
                    equitiesPct = snapshot.items[0].allocationPct,
                    bondsPct = snapshot.items[1].allocationPct,
                    cashPct = snapshot.items[2].allocationPct,
                    portfolioDrawdownPct = drawdownPct
                )
                evaluationResult = result
                if (result.isSuccessful) {
                    onComplete(result.xpAwarded)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Stress-Test: Trigger -20% Market Crash Shock", fontWeight = FontWeight.Bold)
        }

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
                                    imageVector = if (eval.isSuccessful) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (eval.isSuccessful) Color(0xFF4ADE80) else Color(0xFFF87171),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(eval.title, color = if (eval.isSuccessful) Color(0xFF4ADE80) else Color(0xFFF87171), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(eval.feedbackMessage, color = Color(0xFFE2E8F0), fontSize = 13.sp, lineHeight = 19.sp)

                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.fillMaxWidth().background(Color(0x33000000), RoundedCornerShape(8.dp)).padding(10.dp)) {
                                Text("💡 " + eval.aiDeepDive, color = Color(0xFF5FE2D6), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
