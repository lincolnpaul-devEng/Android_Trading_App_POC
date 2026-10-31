package com.trading.app.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.trading.app.data.local.quiz.QuizDao
import com.trading.app.data.local.quiz.QuizProgressEntity
import com.trading.app.data.local.quiz.QuizQuestionEntity
import com.trading.app.data.local.quiz.QuizSyncMetadataEntity
import com.trading.app.data.remote.quiz.PredictionConfigDto
import com.trading.app.data.remote.quiz.QuizQuestionDto
import com.trading.app.data.remote.quiz.QuizRemoteDataSource
import com.trading.app.domain.quiz.model.PredictionConfig
import com.trading.app.domain.quiz.model.QuizProgressSummary
import com.trading.app.domain.quiz.model.QuizQuestion
import com.trading.app.domain.quiz.model.QuizQuestionType
import com.trading.app.domain.quiz.repository.QuizRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuizRepositoryImpl @Inject constructor(
    private val quizDao: QuizDao,
    private val remoteDataSource: QuizRemoteDataSource,
    private val gson: Gson
) : QuizRepository {

    private val stringListType = object : TypeToken<List<String>>() {}.type

    override fun getQuestionsFlow(): Flow<List<QuizQuestion>> {
        return combine(quizDao.getAllQuestions(), quizDao.getAllProgress()) { entities, progressList ->
            val progressMap = progressList.associateBy { it.questionId }
            entities.map { it.toDomain(progressMap[it.id]) }
        }
    }

    override fun getQuestion(id: String): Flow<QuizQuestion?> {
        return combine(quizDao.getQuestionById(id), quizDao.getAllProgress()) { entity, progressList ->
            entity?.let {
                val progress = progressList.find { p -> p.questionId == id }
                it.toDomain(progress)
            }
        }
    }

    override suspend fun syncQuestionsWithServer(force: Boolean): Result<Int> {
        return runCatching {
            val syncMetadata = quizDao.getSyncMetadata("quiz_sync")
            val etagToSend = if (force) null else syncMetadata?.etag

            val response = remoteDataSource.fetchQuizManifest(etagToSend).getOrThrow()
            if (response == null) {
                // Already up to date (304 Not Modified)
                return@runCatching 0
            }

            val questionEntities = response.items.map { it.toEntity() }
            quizDao.upsertQuestions(questionEntities)
            quizDao.pruneOldQuestions(response.items.map { it.id })

            quizDao.upsertSyncMetadata(
                QuizSyncMetadataEntity(
                    syncKey = "quiz_sync",
                    serverVersion = response.version,
                    etag = response.etag,
                    lastSyncTimestamp = System.currentTimeMillis()
                )
            )

            questionEntities.size
        }
    }

    override suspend fun submitAnswer(
        questionId: String,
        answer: String,
        isCorrect: Boolean,
        earnedXp: Int
    ) {
        val existing = quizDao.getProgressFor(questionId)
        val attemptCount = (existing?.attemptCount ?: 0) + 1
        val finalEarnedXp = if (existing?.isCompleted == true) existing.earnedXp else earnedXp

        val progress = QuizProgressEntity(
            questionId = questionId,
            isCompleted = true,
            userAnswer = answer,
            isCorrect = isCorrect,
            earnedXp = finalEarnedXp,
            attemptCount = attemptCount,
            lastAttemptAt = System.currentTimeMillis()
        )
        quizDao.upsertProgress(progress)
    }

    override suspend fun resetProgress() {
        quizDao.clearProgress()
    }

    override fun getProgressSummary(): Flow<QuizProgressSummary> {
        return combine(quizDao.getAllQuestions(), quizDao.getAllProgress()) { questions, progressList ->
            val totalCount = questions.size
            val completedList = progressList.filter { it.isCompleted }
            val completedCount = completedList.size
            val totalEarnedXp = completedList.sumOf { it.earnedXp }
            QuizProgressSummary(
                completedCount = completedCount,
                totalCount = totalCount,
                totalEarnedXp = totalEarnedXp
            )
        }
    }

    private fun QuizQuestionDto.toEntity(): QuizQuestionEntity {
        return QuizQuestionEntity(
            id = id,
            lessonId = lessonId,
            title = title,
            prompt = prompt,
            type = type,
            version = version,
            minXp = minXp,
            maxXp = maxXp,
            publishedAt = publishedAt,
            explanation = explanation,
            aiExplanationHint = aiExplanationHint,
            optionsJson = options?.let { gson.toJson(it) },
            correctAnswer = correctAnswer,
            tolerance = tolerance,
            unit = unit,
            predictionConfigJson = predictionConfig?.let { gson.toJson(it) },
            simulationType = simulationType,
            simulationConfigJson = simulationConfigJson,
            cachedAt = System.currentTimeMillis()
        )
    }

    private fun QuizQuestionEntity.toDomain(progress: QuizProgressEntity?): QuizQuestion {
        val parsedType = try {
            QuizQuestionType.valueOf(type)
        } catch (_: Exception) {
            QuizQuestionType.MULTIPLE_CHOICE
        }

        val parsedOptions: List<String> = optionsJson?.let {
            runCatching { gson.fromJson<List<String>>(it, stringListType) }.getOrNull()
        } ?: emptyList()

        val parsedPrediction: PredictionConfig? = predictionConfigJson?.let {
            runCatching {
                val dto = gson.fromJson(it, PredictionConfigDto::class.java)
                PredictionConfig(
                    variableName = dto.variableName,
                    minVal = dto.minVal,
                    maxVal = dto.maxVal,
                    step = dto.step,
                    defaultVal = dto.defaultVal,
                    unit = dto.unit,
                    targetMin = dto.targetMin,
                    targetMax = dto.targetMax,
                    predictionPrompt = dto.predictionPrompt,
                    experimentPrompt = dto.experimentPrompt,
                    revealInsight = dto.revealInsight
                )
            }.getOrNull()
        }

        return QuizQuestion(
            id = id,
            lessonId = lessonId,
            title = title,
            prompt = prompt,
            type = parsedType,
            version = version,
            minXp = minXp,
            maxXp = maxXp,
            publishedAt = publishedAt,
            explanation = explanation,
            aiExplanationHint = aiExplanationHint,
            options = parsedOptions,
            correctAnswer = correctAnswer,
            tolerance = tolerance,
            unit = unit,
            predictionConfig = parsedPrediction,
            simulationType = simulationType,
            simulationConfigJson = simulationConfigJson,
            isCompleted = progress?.isCompleted ?: false,
            userAnswer = progress?.userAnswer,
            earnedXp = progress?.earnedXp ?: 0
        )
    }
}
