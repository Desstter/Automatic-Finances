package com.example.automaticfinances.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Its own DataStore file so the AI credentials survive independently of theme/insights toggles.
private val Context.aiDataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_preferences")

/**
 * User-controlled configuration for the in-app AI backend (DeepSeek). The API key and model are
 * editable from Settings so the user owns their own credentials and can point at any compatible
 * model id without an app update. Read on every LLM call so changes take effect immediately.
 */
class AiPreferences(private val context: Context) {

    /** DeepSeek API key. Empty until the user pastes one in Settings. */
    val apiKey: Flow<String> = context.aiDataStore.data.map { it[KEY_API] ?: "" }

    /** Model id sent to DeepSeek. Defaults to the fast chat model; user-overridable. */
    val model: Flow<String> = context.aiDataStore.data.map { it[KEY_MODEL]?.takeIf { m -> m.isNotBlank() } ?: DEFAULT_MODEL }

    /** Whether the AI financial advisor is allowed to run. On by default. */
    val advisorEnabled: Flow<Boolean> = context.aiDataStore.data.map { it[KEY_ADVISOR] ?: true }

    suspend fun getApiKey(): String = apiKey.first()
    suspend fun getModel(): String = model.first()
    suspend fun isAdvisorEnabled(): Boolean = advisorEnabled.first()

    suspend fun setApiKey(value: String) {
        context.aiDataStore.edit { it[KEY_API] = value.trim() }
    }

    suspend fun setModel(value: String) {
        context.aiDataStore.edit { it[KEY_MODEL] = value.trim() }
    }

    suspend fun setAdvisorEnabled(enabled: Boolean) {
        context.aiDataStore.edit { it[KEY_ADVISOR] = enabled }
    }

    companion object {
        const val DEFAULT_MODEL = "deepseek-chat"

        private val KEY_API = stringPreferencesKey("deepseek_api_key")
        private val KEY_MODEL = stringPreferencesKey("deepseek_model")
        private val KEY_ADVISOR = booleanPreferencesKey("ai_advisor_enabled")
    }
}
