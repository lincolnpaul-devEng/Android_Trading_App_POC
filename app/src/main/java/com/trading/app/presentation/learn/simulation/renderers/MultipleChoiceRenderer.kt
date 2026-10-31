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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trading.app.domain.quiz.model.QuizQuestion

@Composable
fun MultipleChoiceRenderer(
    question: QuizQuestion,
    onSubmit: (userAnswer: String, isCorrect: Boolean, earnedXp: Int) -> Unit
) {
    val correctIndex = question.correctAnswer?.toIntOrNull() ?: 0
    var selectedIndex by remember(question.id) {
        mutableStateOf(question.userAnswer?.toIntOrNull())
    }
    var isSubmitted by remember(question.id) {
        mutableStateOf(question.isCompleted)
    }

    val optionLetters = listOf("A", "B", "C", "D", "E")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Question Prompt Card
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
                            .background(Color(0x335FE2D6), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "CONCEPT CHALLENGE",
                            color = Color(0xFF5FE2D6),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "+${question.maxXp} XP",
                        color = Color(0xFF8B5CF6),
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

        // Options
        question.options.forEachIndexed { index, optionText ->
            val letter = optionLetters.getOrElse(index) { "${index + 1}" }
            val isSelected = selectedIndex == index
            val isCorrect = index == correctIndex

            val cardBg = when {
                !isSubmitted && isSelected -> Color(0x335FE2D6)
                isSubmitted && isCorrect -> Color(0x2E22C55E)
                isSubmitted && isSelected && !isCorrect -> Color(0x2EEF4444)
                else -> Color(0xFF161B22)
            }

            val cardBorder = when {
                !isSubmitted && isSelected -> Color(0xFF5FE2D6)
                isSubmitted && isCorrect -> Color(0xFF22C55E)
                isSubmitted && isSelected && !isCorrect -> Color(0xFFEF4444)
                else -> Color(0xFF21262D)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .background(cardBg, RoundedCornerShape(12.dp))
                    .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                    .clickable(enabled = !isSubmitted) {
                        selectedIndex = index
                    }
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                when {
                                    isSubmitted && isCorrect -> Color(0xFF22C55E)
                                    isSubmitted && isSelected && !isCorrect -> Color(0xFFEF4444)
                                    isSelected -> Color(0xFF5FE2D6)
                                    else -> Color(0xFF21262D)
                                },
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSubmitted && isCorrect) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        } else if (isSubmitted && isSelected && !isCorrect) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        } else {
                            Text(
                                text = letter,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = optionText.replace("\\n", "\n").replace("`", ""),
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Button
        if (!isSubmitted) {
            Button(
                onClick = {
                    val sel = selectedIndex ?: return@Button
                    isSubmitted = true
                    val correct = sel == correctIndex
                    val xp = if (correct) question.maxXp else question.minXp
                    onSubmit(sel.toString(), correct, xp)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = selectedIndex != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF5FE2D6),
                    contentColor = Color(0xFF0D1117),
                    disabledContainerColor = Color(0xFF21262D),
                    disabledContentColor = Color(0xFF6E7681)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Submit Answer",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Post-submission Explanation Card
        AnimatedVisibility(visible = isSubmitted) {
            val userWasCorrect = selectedIndex == correctIndex
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
                                text = if (userWasCorrect) "CORRECT! (+${question.maxXp} XP)" else "NOT QUITE (+${question.minXp} XP)",
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
