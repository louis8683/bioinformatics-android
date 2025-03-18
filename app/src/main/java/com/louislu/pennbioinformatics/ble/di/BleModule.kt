package com.louislu.pennbioinformatics.ble.di

import android.content.Context
import com.louislu.pennbioinformatics.auth.AuthRepository
import com.louislu.pennbioinformatics.auth.AuthRepositoryImpl
import com.louislu.pennbioinformatics.ble.BleRepository
import com.louislu.pennbioinformatics.ble.BleRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BleModule {

    @Provides
    @Singleton
    fun provideBleRepository(
        @ApplicationContext context: Context
    ): BleRepository {
        return BleRepositoryImpl(context)
    }

}