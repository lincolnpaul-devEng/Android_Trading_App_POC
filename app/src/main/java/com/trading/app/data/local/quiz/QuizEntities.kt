package com.trading.app.data.local.quiz

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quiz_questions")
data class QuizQuestionEntity(
    @PrimaryKey val id: String,
    val lessonId: String,
    val title: String,
    val prompt: String,
    val type: String,
    val version: Int,
    val minXp: Int,
    val maxXp: Int,
    val publishedAt: Long,
    val explanation: String,
    val aiExplanationHint: String?,
    val optionsJson: String?,
    val correctAnswer: String?,
    val tolerance: Double?,
    val unit: String?,
    val predictionConfigJson: String?,
    val simulationType: String?,
    val simulationConfigJson: String?,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "quiz_progress")
data class QuizProgressEntity(
    @PrimaryKey val questionId: String,
    val isCompleted: Boolean,
    val userAnswer: String?,
    val isCorrect: Boolean,
    val earnedXp: Int,
    val attemptCount: Int,
    val lastAttemptAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "quiz_sync_metadata")
data class QuizSyncMetadataEntity(
    @PrimaryKey val syncKey: String,
    val serverVersion: Int,
    val etag: String?,
    val lastSyncTimestamp: Long
)
