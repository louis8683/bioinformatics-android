package com.louislu.pennbioinformatics.di

import android.content.Context
import androidx.room.Room
import com.louislu.pennbioinformatics.auth.AuthRepository
import com.louislu.pennbioinformatics.auth.AuthRepositoryImpl
import com.louislu.pennbioinformatics.auth.UserService
import com.louislu.pennbioinformatics.ble.BleRepository
import com.louislu.pennbioinformatics.ble.BleRepositoryImpl
import com.louislu.pennbioinformatics.data.AppDatabase
import com.louislu.pennbioinformatics.data.entry.DataEntryRepositoryImpl
import com.louislu.pennbioinformatics.data.entry.local.DataEntryDao
import com.louislu.pennbioinformatics.data.entry.remote.DataEntryApiService
import com.louislu.pennbioinformatics.data.location.LocationRepositoryImpl
import com.louislu.pennbioinformatics.data.session.SessionRepositoryImpl
import com.louislu.pennbioinformatics.data.session.local.SessionDao
import com.louislu.pennbioinformatics.data.session.remote.SessionApiService
import com.louislu.pennbioinformatics.domain.repository.DataEntryRepository
import com.louislu.pennbioinformatics.domain.repository.LocationRepository
import com.louislu.pennbioinformatics.domain.repository.SessionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /*** Dependencies for repositories ***/

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        Timber.i("Database Provided")
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "bioinformatics_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideSessionDao(database: AppDatabase): SessionDao {
        Timber.i("Session Dao Provided")
        return database.sessionDao()
    }

    @Provides
    fun provideDataEntryDao(database: AppDatabase): DataEntryDao {
        return database.dataEntryDao()
    }

    /*** Repositories ***/

    @Provides
    @Singleton
    fun provideAuthRepository(
        userService: UserService,
        @ApplicationContext context: Context
    ): AuthRepository {
        return AuthRepositoryImpl(userService, context)
    }

    @Provides
    @Singleton
    fun provideBleRepository(
        @ApplicationContext context: Context
    ): BleRepository {
        return BleRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideLocationRepository(
        @ApplicationContext context: Context
    ): LocationRepository {
        return LocationRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideSessionRepository(
        sessionDao: SessionDao,
        sessionApiService: SessionApiService,
        authRepository: AuthRepository
    ): SessionRepository {
        Timber.i("Session Repository Provided")
        return SessionRepositoryImpl(sessionDao, sessionApiService, authRepository)
    }

    @Provides
    @Singleton
    fun provideDataEntryRepository(
        dataEntryDao: DataEntryDao,
        dataEntryApiService: DataEntryApiService,
        authRepository: AuthRepository
    ): DataEntryRepository {
        return DataEntryRepositoryImpl(dataEntryDao, dataEntryApiService, authRepository)
    }
}