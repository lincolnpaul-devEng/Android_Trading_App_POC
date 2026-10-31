package com.trading.app.data.remote.quiz

import com.google.gson.annotations.SerializedName

data class PredictionConfigDto(
    @SerializedName("variable_name") val variableName: String,
    @SerializedName("min_val") val minVal: Float,
    @SerializedName("max_val") val maxVal: Float,
    @SerializedName("step") val step: Float,
    @SerializedName("default_val") val defaultVal: Float,
    @SerializedName("unit") val unit: String,
    @SerializedName("target_min") val targetMin: Float,
    @SerializedName("target_max") val targetMax: Float,
    @SerializedName("prediction_prompt") val predictionPrompt: String,
    @SerializedName("experiment_prompt") val experimentPrompt: String,
    @SerializedName("reveal_insight") val revealInsight: String
)

data class QuizQuestionDto(
    @SerializedName("id") val id: String,
    @SerializedName("lesson_id") val lessonId: String,
    @SerializedName("title") val title: String,
    @SerializedName("prompt") val prompt: String,
    @SerializedName("type") val type: String,
    @SerializedName("version") val version: Int,
    @SerializedName("min_xp") val minXp: Int,
    @SerializedName("max_xp") val maxXp: Int,
    @SerializedName("published_at") val publishedAt: Long,
    @SerializedName("explanation") val explanation: String,
    @SerializedName("ai_explanation_hint") val aiExplanationHint: String?,
    @SerializedName("options") val options: List<String>?,
    @SerializedName("correct_answer") val correctAnswer: String?,
    @SerializedName("tolerance") val tolerance: Double?,
    @SerializedName("unit") val unit: String?,
    @SerializedName("prediction_config") val predictionConfig: PredictionConfigDto?,
    @SerializedName("simulation_type") val simulationType: String?,
    @SerializedName("simulation_config_json") val simulationConfigJson: String?
)

data class QuizServerManifestResponse(
    @SerializedName("version") val version: Int,
    @SerializedName("etag") val etag: String,
    @SerializedName("published_at") val publishedAt: Long,
    @SerializedName("items") val items: List<QuizQuestionDto>
)
