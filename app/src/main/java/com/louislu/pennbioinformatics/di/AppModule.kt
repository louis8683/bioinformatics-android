package com.louislu.pennbioinformatics.di

import android.content.Context
import com.louislu.pennbioinformatics.data.location.LocationRepositoryImpl
import com.louislu.pennbioinformatics.domain.repository.LocationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideLocationRepository(@ApplicationContext context: Context): LocationRepository {
        return LocationRepositoryImpl(context) // ✅ Binds interface to implementation
    }
}