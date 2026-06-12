package com.example.automaticfinances

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.example.automaticfinances.data.preferences.OnboardingPreferences
import com.example.automaticfinances.navigation.AppNavigation
import com.example.automaticfinances.ui.onboarding.OnboardingScreen
import com.example.automaticfinances.ui.onboarding.rememberNotificationPermissionsState
import com.example.automaticfinances.ui.theme.FinanceTheme
import com.example.automaticfinances.ui.theme.ThemeViewModel
import com.example.automaticfinances.ui.voice.VoiceEntryActivity
import com.example.automaticfinances.system.OemAutostart
import com.example.automaticfinances.system.ServiceManager
import com.example.automaticfinances.system.VoiceQuickActionNotifier
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val themeViewModel: ThemeViewModel by viewModels()

    private val onboardingPreferences by lazy { OnboardingPreferences(this) }

    // POST_NOTIFICATIONS is runtime-gated on Android 13+. Without it the voice/SMS feedback
    // notifications and budget alerts are silently suppressed, so we must actually ask for it.
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Outcome handled by the OS; the app degrades gracefully if denied. */ }

    // RECEIVE_SMS powers the OEM-proof direct SMS capture path (see SmsReceiver). Runtime-gated
    // because it's a dangerous permission; the app falls back to the notification listener if denied.
    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Outcome handled by the OS; the app degrades gracefully if denied. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // First-run users meet the runtime permissions inside the onboarding flow (with context).
        // Returning users who already finished onboarding still get a quiet top-up request here.
        if (onboardingPreferences.isCompleted) {
            requestNotificationPermissionIfNeeded()
            requestSmsPermissionIfNeeded()
        }

        // Enable edge-to-edge display
        enableEdgeToEdge()

        // Configure system bars for immersive experience
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            FinanceTheme(
                darkTheme = themeViewModel.isDarkTheme(),
                useDynamicColor = themeViewModel.getUseDynamicColor(),
                accentColor = themeViewModel.getAccentColor()
            ) {
                var onboardingDone by remember { mutableStateOf(onboardingPreferences.isCompleted) }

                if (!onboardingDone) {
                    val permissions = rememberNotificationPermissionsState()
                    var autostartAck by remember {
                        mutableStateOf(onboardingPreferences.autostartAcknowledged)
                    }
                    OnboardingScreen(
                        state = permissions,
                        onGrantNotificationAccess = {
                            ServiceManager.openNotificationListenerSettings(this)
                        },
                        onRequestPostNotifications = ::requestNotificationPermissionIfNeeded,
                        onRequestSmsAccess = ::requestSmsPermissionIfNeeded,
                        onRequestBatteryExemption = {
                            ServiceManager.requestIgnoreBatteryOptimizations(this)
                        },
                        onFinish = {
                            onboardingPreferences.isCompleted = true
                            onboardingDone = true
                        },
                        oemAutostartRelevant = OemAutostart.isRelevant(),
                        oemAutostartAcknowledged = autostartAck,
                        onOpenOemAutostart = { OemAutostart.open(this) },
                        onAcknowledgeOemAutostart = {
                            onboardingPreferences.autostartAcknowledged = true
                            autostartAck = true
                        },
                    )
                } else {
                    val navController = rememberNavController()
                    AppNavigation(
                        navController = navController,
                        themeViewModel = themeViewModel,
                        onOpenNotifAccess = {
                            ServiceManager.openNotificationListenerSettings(this)
                        },
                        onVoiceEntry = {
                            startActivity(Intent(this, VoiceEntryActivity::class.java))
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-post the persistent voice notification: this is the point where POST_NOTIFICATIONS may
        // have just been granted (onboarding / system dialog), so the first successful post often
        // happens here. Idempotent, so cheap to repeat. Also nudge the listener to bind so the Home
        // detection banner clears the instant the user returns from granting notification access.
        VoiceQuickActionNotifier.show(this)
        ServiceManager.requestListenerRebind(this)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestSmsPermissionIfNeeded() {
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECEIVE_SMS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            smsPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
        }
    }
}