package com.dttrn.datfs.di

import android.content.Context
import com.dttrn.datfs.core.tts.TtsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TtsModule {

    @Provides
    @Singleton
    fun provideTtsManager(
        @ApplicationContext context: Context,
    ): TtsManager = TtsManager(context)
}
