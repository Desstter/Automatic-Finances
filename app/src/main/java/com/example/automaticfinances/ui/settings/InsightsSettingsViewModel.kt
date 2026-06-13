package com.example.automaticfinances.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automaticfinances.data.preferences.AiPreferences
import com.example.automaticfinances.data.preferences.InsightsPreferences
import com.example.automaticfinances.system.DigestWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the "Resumen e insights" and "Inteligencia artificial" settings sections: the digest toggle,
 * the "run now" action, and the DeepSeek credentials/model + AI-advisor switch.
 */
@HiltViewModel
class InsightsSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val insightsPreferences: InsightsPreferences,
    private val aiPreferences: AiPreferences,
) : ViewModel() {

    val digestEnabled: StateFlow<Boolean> = insightsPreferences.digestEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    // ---- AI advisor (DeepSeek) ----

    val aiAdvisorEnabled: StateFlow<Boolean> = aiPreferences.advisorEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val aiApiKey: StateFlow<String> = aiPreferences.apiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val aiModel: StateFlow<String> = aiPreferences.model
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AiPreferences.DEFAULT_MODEL)

    fun setDigestEnabled(enabled: Boolean) {
        viewModelScope.launch { insightsPreferences.setDigestEnabled(enabled) }
    }

    fun setAiAdvisorEnabled(enabled: Boolean) {
        viewModelScope.launch { aiPreferences.setAdvisorEnabled(enabled) }
    }

    fun setAiApiKey(value: String) {
        viewModelScope.launch { aiPreferences.setApiKey(value) }
    }

    fun setAiModel(value: String) {
        viewModelScope.launch { aiPreferences.setModel(value) }
    }

    /** Enqueues a one-off digest so the user can preview it immediately. */
    fun runDigestNow() = DigestWorker.runNow(context)
}
