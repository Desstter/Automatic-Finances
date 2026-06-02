package com.example.automaticfinances.data.preferences

import android.content.Context

/**
 * Tiny synchronous flag store for the first-run onboarding. Uses SharedPreferences (not DataStore)
 * on purpose: the onboarding gate in MainActivity needs the value at composition time with no async
 * flicker between "show the app" and "show onboarding".
 */
class OnboardingPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isCompleted: Boolean
        get() = prefs.getBoolean(KEY_COMPLETED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_COMPLETED, value).apply()
        }

    companion object {
        private const val PREFS_NAME = "onboarding_prefs"
        private const val KEY_COMPLETED = "onboarding_completed"
    }
}
