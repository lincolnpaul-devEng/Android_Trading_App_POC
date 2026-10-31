package com.trading.app.data.local.quiz

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizDao {

    @Query("SELECT * FROM quiz_questions ORDER BY publishedAt ASC")
    fun getAllQuestions(): Flow<List<QuizQuestionEntity>>

    @Query("SELECT * FROM quiz_questions WHERE id = :id LIMIT 1")
    fun getQuestionById(id: String): Flow<QuizQuestionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuestions(questions: List<QuizQuestionEntity>)

    @Query("DELETE FROM quiz_questions WHERE id NOT IN (:validIds)")
    suspend fun pruneOldQuestions(validIds: List<String>)

    @Query("SELECT * FROM quiz_progress")
    fun getAllProgress(): Flow<List<QuizProgressEntity>>

    @Query("SELECT * FROM quiz_progress WHERE questionId = :questionId LIMIT 1")
    suspend fun getProgressFor(questionId: String): QuizProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: QuizProgressEntity)

    @Query("DELETE FROM quiz_progress")
    suspend fun clearProgress()

    @Query("SELECT * FROM quiz_sync_metadata WHERE syncKey = :key LIMIT 1")
    suspend fun getSyncMetadata(key: String): QuizSyncMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSyncMetadata(metadata: QuizSyncMetadataEntity)
}
