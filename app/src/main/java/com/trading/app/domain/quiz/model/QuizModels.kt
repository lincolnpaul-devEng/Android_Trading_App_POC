package com.trading.app.domain.quiz.model

enum class QuizQuestionType {
    MULTIPLE_CHOICE,
    NUMERICAL,
    PREDICTION_EXPERIMENT,
    SIMULATION
}

data class PredictionConfig(
    val variableName: String,
    val minVal: Float,
    val maxVal: Float,
    val step: Float,
    val defaultVal: Float,
    val unit: String,
    val targetMin: Float,
    val targetMax: Float,
    val predictionPrompt: String,
    val experimentPrompt: String,
    val revealInsight: String
)

data class QuizQuestion(
    val id: String,
    val lessonId: String = "weekly_spotlight",
    val title: String,
    val prompt: String,
    val type: QuizQuestionType,
    val version: Int = 1,
    val minXp: Int = 5,
    val maxXp: Int = 25,
    val publishedAt: Long = System.currentTimeMillis(),
    val explanation: String,
    val aiExplanationHint: String? = null,
    val options: List<String> = emptyList(),
    val correctAnswer: String? = null,
    val tolerance: Double? = null,
    val unit: String? = null,
    val predictionConfig: PredictionConfig? = null,
    val simulationType: String? = null,
    val simulationConfigJson: String? = null,
    val isCompleted: Boolean = false,
    val userAnswer: String? = null,
    val earnedXp: Int = 0
)

data class QuizLesson(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val version: Int,
    val questions: List<QuizQuestion> = emptyList()
)

data class QuizProgressSummary(
    val completedCount: Int,
    val totalCount: Int,
    val totalEarnedXp: Int
)
