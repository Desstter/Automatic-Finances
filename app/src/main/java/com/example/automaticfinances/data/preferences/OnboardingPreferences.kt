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

    /**
     * User-confirmed they enabled the OEM auto-start toggle. There is no API to read that toggle's
     * real state, so this manual acknowledgement is the only way to mark the step done and stop
     * nagging. Best-effort UX, not a security boundary.
     */
    var autostartAcknowledged: Boolean
        get() = prefs.getBoolean(KEY_AUTOSTART_ACK, false)
        set(value) {
            prefs.edit().putBoolean(KEY_AUTOSTART_ACK, value).apply()
        }

    companion object {
        private const val PREFS_NAME = "onboarding_prefs"
        private const val KEY_COMPLETED = "onboarding_completed"
        private const val KEY_AUTOSTART_ACK = "autostart_acknowledged"
    }
}
