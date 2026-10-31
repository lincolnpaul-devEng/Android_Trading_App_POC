package com.trading.app.presentation.learn.simulation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.trading.app.domain.quiz.model.QuizQuestionType
import com.trading.app.presentation.learn.simulation.renderers.AiExplanationCard
import com.trading.app.presentation.learn.simulation.renderers.MultipleChoiceRenderer
import com.trading.app.presentation.learn.simulation.renderers.NumericalRenderer
import com.trading.app.presentation.learn.simulation.renderers.PredictionExperimentRenderer

@Composable
fun DynamicQuizScreen(
    onBack: () -> Unit,
    viewModel: DynamicQuizViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentQuestion = uiState.currentQuestion

    // If active question is a full simulation, render it using SimulationRegistry
    if (currentQuestion != null && currentQuestion.type == QuizQuestionType.SIMULATION && currentQuestion.simulationType != null) {
        val simComposable = SimulationRegistry.get(currentQuestion.simulationType)
        if (simComposable != null) {
            simComposable(
                currentQuestion.simulationConfigJson,
                { xp -> viewModel.submitAnswer("completed", true, xp) },
                onBack
            )
            return
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top Header
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
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (uiState.questions.isNotEmpty()) {
                            "Question ${uiState.currentIndex + 1} of ${uiState.questions.size}"
                        } else "Loading Quiz...",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (uiState.isSyncing) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Syncing",
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(13.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Synced",
                            tint = Color(0xFF4ADE80),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            // Earned XP Chip
            Box(
                modifier = Modifier
                    .background(Color(0x335FE2D6), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF5FE2D6), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${uiState.totalEarnedXp} XP",
                    color = Color(0xFF5FE2D6),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Progress Bar
        LinearProgressIndicator(
            progress = { uiState.progressFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = Color(0xFF5FE2D6),
            trackColor = Color(0xFF21262D)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Content Area
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF5FE2D6))
            }
        } else if (currentQuestion == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No questions available. Pull to refresh.",
                    color = Color(0xFF8B949E),
                    fontSize = 14.sp
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = currentQuestion.title.replace("\\n", "\n").replace("`", "").trim(),
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Render dynamic question type
                when (currentQuestion.type) {
                    QuizQuestionType.MULTIPLE_CHOICE -> {
                        MultipleChoiceRenderer(
                            question = currentQuestion,
                            onSubmit = { answer, isCorrect, xp ->
                                viewModel.submitAnswer(answer, isCorrect, xp)
                            }
                        )
                    }
                    QuizQuestionType.NUMERICAL -> {
                        NumericalRenderer(
                            question = currentQuestion,
                            onSubmit = { answer, isCorrect, xp ->
                                viewModel.submitAnswer(answer, isCorrect, xp)
                            }
                        )
                    }
                    QuizQuestionType.PREDICTION_EXPERIMENT -> {
                        PredictionExperimentRenderer(
                            question = currentQuestion,
                            onSubmit = { answer, isCorrect, xp ->
                                viewModel.submitAnswer(answer, isCorrect, xp)
                            }
                        )
                    }
                    QuizQuestionType.SIMULATION -> {
                        // Handled by top branch or fallback info
                        Text(
                            text = "Simulation: ${currentQuestion.simulationType}",
                            color = Color(0xFF5FE2D6),
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // AI Deep Dive Card (expandable)
                AiExplanationCard(
                    aiHint = currentQuestion.aiExplanationHint,
                    generalExplanation = currentQuestion.explanation
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Bottom Navigation Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D1117))
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { viewModel.previousQuestion() },
                    enabled = uiState.currentIndex > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF161B22),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0x1A161B22),
                        disabledContentColor = Color(0x33FFFFFF)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Previous", fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        if (uiState.currentIndex < uiState.questions.size - 1) {
                            viewModel.nextQuestion()
                        } else {
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF21262D),
                        contentColor = Color(0xFF5FE2D6)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (uiState.currentIndex < uiState.questions.size - 1) "Next" else "Done",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
