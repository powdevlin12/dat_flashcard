package com.dttrn.datfs.di

import android.content.Context
import androidx.room.Room
import com.dttrn.datfs.core.data.local.AppDatabase
import com.dttrn.datfs.core.data.local.dao.DeckDao
import com.dttrn.datfs.core.data.local.dao.FlashcardDao
import com.dttrn.datfs.core.data.local.dao.ReviewSessionDao
import com.dttrn.datfs.core.data.local.dao.StudyStatisticsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // TODO: thay bằng proper migrations khi release
            .build()
    }

    @Provides
    fun provideDeckDao(db: AppDatabase): DeckDao = db.deckDao()

    @Provides
    fun provideFlashcardDao(db: AppDatabase): FlashcardDao = db.flashcardDao()

    @Provides
    fun provideReviewSessionDao(db: AppDatabase): ReviewSessionDao = db.reviewSessionDao()

    @Provides
    fun provideStudyStatisticsDao(db: AppDatabase): StudyStatisticsDao = db.studyStatisticsDao()
}
