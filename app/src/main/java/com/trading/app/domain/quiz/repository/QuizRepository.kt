package com.trading.app.domain.quiz.repository

import com.trading.app.domain.quiz.model.QuizProgressSummary
import com.trading.app.domain.quiz.model.QuizQuestion
import kotlinx.coroutines.flow.Flow

interface QuizRepository {
    fun getQuestionsFlow(): Flow<List<QuizQuestion>>
    fun getQuestion(id: String): Flow<QuizQuestion?>
    suspend fun syncQuestionsWithServer(force: Boolean = false): Result<Int>
    suspend fun submitAnswer(questionId: String, answer: String, isCorrect: Boolean, earnedXp: Int)
    suspend fun resetProgress()
    fun getProgressSummary(): Flow<QuizProgressSummary>
}
