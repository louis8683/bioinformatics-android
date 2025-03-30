package com.louislu.pennbioinformatics.di

import com.louislu.pennbioinformatics.auth.UserService
import com.louislu.pennbioinformatics.data.entry.remote.DataEntryApiService
import com.louislu.pennbioinformatics.data.session.remote.SessionApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val BASE_URL = "https://p3q86838i1.execute-api.us-east-2.amazonaws.com/dev/" // <-- Change this!

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideUserService(retrofit: Retrofit): UserService {
        return retrofit.create(UserService::class.java)
    }

    @Provides
    @Singleton
    fun provideSessionApiService(retrofit: Retrofit): SessionApiService {
        return retrofit.create(SessionApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideDataEntryApiService(retrofit: Retrofit): DataEntryApiService {
        return retrofit.create(DataEntryApiService::class.java)
    }
}