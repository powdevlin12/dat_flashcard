package com.dttrn.datfs.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * AppModule — giữ lại cho backward compat.
 * Các providers đã được tách ra DatabaseModule và DataStoreModule.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // Providers đã được tách: DatabaseModule, DataStoreModule, RepositoryModule
}
