package com.trading.app.data.local.quiz

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        QuizQuestionEntity::class,
        QuizProgressEntity::class,
        QuizSyncMetadataEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class QuizDatabase : RoomDatabase() {
    abstract fun quizDao(): QuizDao
}
