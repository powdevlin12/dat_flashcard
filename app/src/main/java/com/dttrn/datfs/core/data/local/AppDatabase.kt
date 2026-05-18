package com.dttrn.datfs.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dttrn.datfs.core.data.local.converter.StringListConverter
import com.dttrn.datfs.core.data.local.dao.DeckDao
import com.dttrn.datfs.core.data.local.dao.FlashcardDao
import com.dttrn.datfs.core.data.local.dao.ReviewSessionDao
import com.dttrn.datfs.core.data.local.dao.StudyStatisticsDao
import com.dttrn.datfs.core.data.local.entity.DeckEntity
import com.dttrn.datfs.core.data.local.entity.FlashcardEntity
import com.dttrn.datfs.core.data.local.entity.ReviewSessionEntity
import com.dttrn.datfs.core.data.local.entity.StudyStatisticsEntity

/**
 * Room Database chính của ứng dụng FlashMind.
 * Version 1 — thêm migration script khi nâng version.
 * exportSchema = true để track migrations trong file JSON.
 */
@Database(
    entities = [
        DeckEntity::class,
        FlashcardEntity::class,
        ReviewSessionEntity::class,
        StudyStatisticsEntity::class,
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(StringListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun reviewSessionDao(): ReviewSessionDao
    abstract fun studyStatisticsDao(): StudyStatisticsDao

    companion object {
        const val DATABASE_NAME = "flashmind.db"
    }
}
