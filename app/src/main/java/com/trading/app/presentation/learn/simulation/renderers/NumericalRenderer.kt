package com.trading.app.presentation.learn.simulation.renderers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trading.app.domain.quiz.model.QuizQuestion
import kotlin.math.abs

@Composable
fun NumericalRenderer(
    question: QuizQuestion,
    onSubmit: (userAnswer: String, isCorrect: Boolean, earnedXp: Int) -> Unit
) {
    var inputText by remember(question.id) {
        mutableStateOf(question.userAnswer ?: "")
    }
    var isSubmitted by remember(question.id) {
        mutableStateOf(question.isCompleted)
    }
    val keyboardController = LocalSoftwareKeyboardController.current

    val expectedVal = question.correctAnswer?.toDoubleOrNull() ?: 0.0
    val tolerance = question.tolerance ?: 0.5

    fun checkCorrectness(input: String): Boolean {
        val num = input.toDoubleOrNull() ?: return false
        return abs(num - expectedVal) <= tolerance
    }

    val userWasCorrect = checkCorrectness(inputText)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Question Card
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
                            .background(Color(0x338B5CF6), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "NUMERICAL CALCULATION",
                            color = Color(0xFFA78BFA),
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

        Spacer(modifier = Modifier.height(20.dp))

        // Input card with steppers and unit
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161B22), RoundedCornerShape(14.dp))
                .border(1.dp, Color(0xFF21262D), RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "YOUR COMPUTED ANSWER",
                    color = Color(0xFF8B949E),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val cur = inputText.toDoubleOrNull() ?: 0.0
                            val updated = (cur - 5.0).coerceAtLeast(0.0)
                            inputText = if (updated % 1.0 == 0.0) updated.toInt().toString() else String.format("%.1f", updated)
                        },
                        enabled = !isSubmitted,
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFF21262D), RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrement", tint = Color.White)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { if (!isSubmitted) inputText = it },
                        modifier = Modifier.weight(1f),
                        enabled = !isSubmitted,
                        singleLine = true,
                        placeholder = { Text("0.0", color = Color(0xFF484F58)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF5FE2D6),
                            unfocusedBorderColor = Color(0xFF30363D),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { keyboardController?.hide() }
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        trailingIcon = {
                            question.unit?.let {
                                Text(
                                    text = it,
                                    color = Color(0xFF8B949E),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    IconButton(
                        onClick = {
                            val cur = inputText.toDoubleOrNull() ?: 0.0
                            val updated = cur + 5.0
                            inputText = if (updated % 1.0 == 0.0) updated.toInt().toString() else String.format("%.1f", updated)
                        },
                        enabled = !isSubmitted,
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFF21262D), RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increment", tint = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!isSubmitted) {
            Button(
                onClick = {
                    keyboardController?.hide()
                    isSubmitted = true
                    val correct = checkCorrectness(inputText)
                    val xp = if (correct) question.maxXp else question.minXp
                    onSubmit(inputText, correct, xp)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = inputText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF5FE2D6),
                    contentColor = Color(0xFF0D1117),
                    disabledContainerColor = Color(0xFF21262D),
                    disabledContentColor = Color(0xFF6E7681)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Verify Calculation",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        AnimatedVisibility(visible = isSubmitted) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (userWasCorrect) Color(0x1F22C55E) else Color(0x1FEF4444),
                            RoundedCornerShape(14.dp)
                        )
                        .border(
                            1.dp,
                            if (userWasCorrect) Color(0xFF22C55E) else Color(0xFFEF4444),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (userWasCorrect) Icons.Default.Check else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (userWasCorrect) Color(0xFF4ADE80) else Color(0xFFF87171),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (userWasCorrect) {
                                    "SPOT ON! Target: ${question.correctAnswer} ${question.unit.orEmpty()} (+${question.maxXp} XP)"
                                } else {
                                    "EXPECTED: ${question.correctAnswer} ${question.unit.orEmpty()} (+${question.minXp} XP)"
                                },
                                color = if (userWasCorrect) Color(0xFF4ADE80) else Color(0xFFF87171),
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
                    }
                }
            }
        }
    }
}
