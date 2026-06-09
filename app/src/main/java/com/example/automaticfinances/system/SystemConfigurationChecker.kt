package com.example.automaticfinances.system

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay

/**
 * Determines whether automatic detection is configured and surfaces it to the UI.
 *
 * Single source of truth: **whether the notification-listener permission is granted**. Once it is,
 * the system binds [SmsNotifListener] and delivers bank notifications to it on its own — detection
 * is live. The previous design also required an in-process "service running" flag that was set in
 * `onListenerConnected`; that flag reset to `false` on every process restart and frequently left
 * the UI stuck on "Iniciando servicio… / Conectando con el sistema", and Ajustes showing the
 * permission as inactive even though it was granted. The permission flag alone is reliable.
 */
object SystemConfigurationChecker {

    fun getSystemHealth(context: Context): SystemHealthStatus {
        val isListenerEnabled = ServiceManager.isNotificationListenerEnabled(context)
        val isBatteryOptimized = isBatteryOptimizationEnabled(context)
        val isSmsCaptureEnabled = isSmsCaptureEnabled(context)

        return SystemHealthStatus(
            isListenerEnabled = isListenerEnabled,
            isBatteryOptimized = isBatteryOptimized,
            isSmsCaptureEnabled = isSmsCaptureEnabled,
            needsUserAttention = !isListenerEnabled,
        )
    }

    /**
     * Whether the app can read bank SMS directly (RECEIVE_SMS granted). This is the ONLY capture path
     * for SMS-only banks (e.g. Bancolombia) — when it's denied there is no notification-listener
     * fallback for them, so a denied state must be surfaced, not silently tolerated.
     */
    fun isSmsCaptureEnabled(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Check if battery optimization is enabled for this app.
     */
    private fun isBatteryOptimizationEnabled(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                val isIgnoring = powerManager.isIgnoringBatteryOptimizations(context.packageName)
                return !isIgnoring
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Reactive system-health state for Compose. Re-reads on every `ON_RESUME` so the instant the
     * user returns from granting notification access in system Settings the UI reflects it — no
     * polling lag. A light 5s tick additionally covers the rare case of the permission changing
     * while the screen is already foregrounded (e.g. a quick-settings toggle).
     */
    @Composable
    fun rememberSystemHealth(context: Context): State<SystemHealthStatus> {
        val lifecycleOwner = LocalLifecycleOwner.current
        val state = remember { mutableStateOf(getSystemHealth(context)) }

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    state.value = getSystemHealth(context)
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        LaunchedEffect(context) {
            while (true) {
                delay(5_000)
                state.value = getSystemHealth(context)
            }
        }

        return state
    }
}

/**
 * Data class representing system health status.
 */
data class SystemHealthStatus(
    val isListenerEnabled: Boolean,
    val isBatteryOptimized: Boolean,
    val isSmsCaptureEnabled: Boolean,
    val needsUserAttention: Boolean,
)
