package com.trading.app.presentation.learn.simulation.renderers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trading.app.domain.quiz.model.QuizQuestion

@Composable
fun PredictionExperimentRenderer(
    question: QuizQuestion,
    onSubmit: (userAnswer: String, isCorrect: Boolean, earnedXp: Int) -> Unit
) {
    val config = question.predictionConfig

    // Step 1: Prediction options
    val predictionOptions = listOf("5%", "10%", "20%", "50%")
    var predictedOption by remember(question.id) {
        mutableStateOf(question.userAnswer)
    }
    var isPredictionLocked by remember(question.id) {
        mutableStateOf(question.isCompleted)
    }

    // Step 2: Interactive Experimentation Sliders
    var leverageSlider by remember { mutableFloatStateOf(config?.defaultVal ?: 10f) }
    var priceDropSlider by remember { mutableFloatStateOf(10f) }

    // Equity calculation
    // Initial capital: $1,000. Position size: $1,000 * leverage.
    // Loss = $1,000 * leverage * (priceDrop / 100)
    // Remaining Equity = $1,000 - Loss. Margin loss % = (leverage * priceDrop).
    val marginLossPct = (leverageSlider * priceDropSlider).coerceIn(0f, 150f)
    val isLiquidated = marginLossPct >= 100f

    val isCorrect = predictedOption == "10%"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Step Header Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161B22), RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFF30363D), RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(Color(0x33EC4899), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (!isPredictionLocked) "PHASE 1: PREDICTION" else "PHASE 2: LIVE EXPERIMENT",
                            color = Color(0xFFF472B6),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "+${question.maxXp} XP",
                        color = Color(0xFF5FE2D6),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = question.prompt.replace("\\n", "\n").replace("`", ""),
                    color = Color.White,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Phase 1 Prediction Chooser
        Text(
            text = "Step 1: Commit your prediction before running the simulation",
            color = Color(0xFF8B949E),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            predictionOptions.forEach { opt ->
                val isSelected = predictedOption == opt
                val bg = if (isSelected) Color(0x335FE2D6) else Color(0xFF161B22)
                val border = if (isSelected) Color(0xFF5FE2D6) else Color(0xFF21262D)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(bg, RoundedCornerShape(12.dp))
                        .border(1.dp, border, RoundedCornerShape(12.dp))
                        .clickable(enabled = !isPredictionLocked) {
                            predictedOption = opt
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = opt,
                        color = if (isSelected) Color(0xFF5FE2D6) else Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (!isPredictionLocked) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    isPredictionLocked = true
                    onSubmit(predictedOption ?: "10%", isCorrect, if (isCorrect) question.maxXp else question.minXp)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = predictedOption != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF5FE2D6),
                    contentColor = Color(0xFF0D1117),
                    disabledContainerColor = Color(0xFF21262D),
                    disabledContentColor = Color(0xFF6E7681)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Lock Prediction & Run Experiment", fontWeight = FontWeight.Bold)
            }
        }

        // Phase 2: Live Interactive Simulation Playground
        AnimatedVisibility(visible = isPredictionLocked) {
            Column(modifier = Modifier.padding(top = 20.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A), RoundedCornerShape(16.dp))
                        .border(1.dp, if (isLiquidated) Color(0xFFEF4444) else Color(0xFF334155), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "MARGIN RISK SIMULATOR",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (isLiquidated) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFDC2626), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("LIQUIDATED", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Loss Gauge
                        Text(
                            text = "Initial Margin Consumed: ${marginLossPct.toInt()}%",
                            color = if (isLiquidated) Color(0xFFF87171) else Color(0xFF5FE2D6),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (marginLossPct / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp),
                            color = if (isLiquidated) Color(0xFFEF4444) else Color(0xFF5FE2D6),
                            trackColor = Color(0xFF1E293B)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Leverage Scrubber
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Leverage Factor", color = Color.White, fontSize = 12.sp)
                            Text("${leverageSlider.toInt()}x", color = Color(0xFF5FE2D6), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Slider(
                            value = leverageSlider,
                            onValueChange = { leverageSlider = it.toInt().toFloat() },
                            valueRange = 1f..20f,
                            steps = 19,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF5FE2D6),
                                activeTrackColor = Color(0xFF5FE2D6),
                                inactiveTrackColor = Color(0xFF334155)
                            )
                        )

                        // Underlying Price Drop Scrubber
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Underlying Asset Price Drop", color = Color.White, fontSize = 12.sp)
                            Text("-${priceDropSlider.toInt()}%", color = Color(0xFFF87171), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Slider(
                            value = priceDropSlider,
                            onValueChange = { priceDropSlider = it.toInt().toFloat() },
                            valueRange = 0f..20f,
                            steps = 20,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFF87171),
                                activeTrackColor = Color(0xFFF87171),
                                inactiveTrackColor = Color(0xFF334155)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Verification & AI Insight
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isCorrect) Color(0x1F22C55E) else Color(0x1FEF4444),
                            RoundedCornerShape(14.dp)
                        )
                        .border(
                            1.dp,
                            if (isCorrect) Color(0xFF22C55E) else Color(0xFFEF4444),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isCorrect) Icons.Default.Check else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isCorrect) Color(0xFF4ADE80) else Color(0xFFF87171),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isCorrect) {
                                    "PREDICTION CONFIRMED! (+${question.maxXp} XP)"
                                } else {
                                    "EXPERIMENT REVEALS: 10% Drop (+${question.minXp} XP)"
                                },
                                color = if (isCorrect) Color(0xFF4ADE80) else Color(0xFFF87171),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = question.explanation.replace("\\n", "\n").replace("`", ""),
                            color = Color(0xFFE2E8F0),
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )

                        config?.revealInsight?.let { insight ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = insight.replace("\\n", "\n").replace("`", ""),
                                color = Color(0xFF5FE2D6),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
