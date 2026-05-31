package com.example.automaticfinances.di

import com.example.automaticfinances.BuildConfig
import com.example.automaticfinances.data.remote.GeminiService
import com.example.automaticfinances.data.remote.LlmJsonClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Networking + serialization graph for the Gemini NLP layer. Kept separate from [AppModule]
 * (which owns DB/repositories) so the data/remote concern is isolated and easy to swap or test.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        // Speech-to-text already cost the user a few seconds; keep the NLP round-trip snappy
        // but tolerant of a cold mobile connection.
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    @Provides
    @Named("geminiApiKey")
    fun provideGeminiApiKey(): String = BuildConfig.GEMINI_API_KEY

    @Provides
    @Singleton
    fun provideLlmJsonClient(impl: GeminiService): LlmJsonClient = impl
}
