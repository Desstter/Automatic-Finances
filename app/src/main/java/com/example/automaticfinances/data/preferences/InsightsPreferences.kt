package com.example.automaticfinances.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Separate DataStore file from theme/onboarding so the insights toggle survives independently.
private val Context.insightsDataStore: DataStore<Preferences> by preferencesDataStore(name = "insights_preferences")

/**
 * User control over the insights layer. Currently a single switch: whether the periodic digest
 * notification ([com.example.automaticfinances.system.DigestWorker]) is allowed to post. Defaults to
 * on — the whole point of the feature is for the app to speak up without being asked.
 */
class InsightsPreferences(private val context: Context) {

    val digestEnabled: Flow<Boolean> = context.insightsDataStore.data.map { prefs ->
        prefs[DIGEST_ENABLED_KEY] ?: true
    }

    /** Suspend one-shot read for non-UI callers (the worker checks this before posting). */
    suspend fun isDigestEnabled(): Boolean = digestEnabled.first()

    suspend fun setDigestEnabled(enabled: Boolean) {
        context.insightsDataStore.edit { it[DIGEST_ENABLED_KEY] = enabled }
    }

    companion object {
        private val DIGEST_ENABLED_KEY = booleanPreferencesKey("digest_enabled")
    }
}
