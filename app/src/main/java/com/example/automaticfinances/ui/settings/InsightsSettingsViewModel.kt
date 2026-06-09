package com.example.automaticfinances.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automaticfinances.data.preferences.InsightsPreferences
import com.example.automaticfinances.system.DigestWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Backs the "Resumen e insights" settings section: the digest toggle and the "run now" action. */
@HiltViewModel
class InsightsSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val insightsPreferences: InsightsPreferences,
) : ViewModel() {

    val digestEnabled: StateFlow<Boolean> = insightsPreferences.digestEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setDigestEnabled(enabled: Boolean) {
        viewModelScope.launch { insightsPreferences.setDigestEnabled(enabled) }
    }

    /** Enqueues a one-off digest so the user can preview it immediately. */
    fun runDigestNow() = DigestWorker.runNow(context)
}
