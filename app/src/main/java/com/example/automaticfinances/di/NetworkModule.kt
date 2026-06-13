package com.example.automaticfinances.di

import com.example.automaticfinances.BuildConfig
import com.example.automaticfinances.data.remote.CompositeLlmClient
import com.example.automaticfinances.data.remote.DeepSeekService
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
 * Networking + serialization graph for the LLM layer (DeepSeek). Kept separate from [AppModule]
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
        // but tolerant of a cold mobile connection. The advisor prompt is larger and DeepSeek can be
        // slower (plus a 2x backoff retry), so allow a longer read window than voice strictly needs.
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    @Provides
    @Named("geminiApiKey")
    fun provideGeminiApiKey(): String = BuildConfig.GEMINI_API_KEY

    // Build-time fallback DeepSeek key (CI/local.properties); the user-set key in AiPreferences
    // takes precedence at runtime. Empty is fine — the app degrades gracefully without a key.
    @Provides
    @Named("deepseekApiKey")
    fun provideDeepSeekApiKey(): String = BuildConfig.DEEPSEEK_API_KEY

    // The two backends, exposed behind the neutral interface + a qualifier each, so CompositeLlmClient
    // depends on the abstraction (and is unit-testable) rather than on the concrete services.
    @Provides
    @Named("primaryLlm")
    fun providePrimaryLlm(impl: DeepSeekService): LlmJsonClient = impl

    @Provides
    @Named("fallbackLlm")
    fun provideFallbackLlm(impl: GeminiService): LlmJsonClient = impl

    // DeepSeek primary with automatic Gemini fallback. Both voice parsing and the financial advisor
    // resolve their LlmJsonClient here, so both get the resilience for free.
    @Provides
    @Singleton
    fun provideLlmJsonClient(impl: CompositeLlmClient): LlmJsonClient = impl
}
