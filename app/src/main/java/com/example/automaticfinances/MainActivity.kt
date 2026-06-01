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
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.example.automaticfinances.navigation.AppNavigation
import com.example.automaticfinances.ui.theme.FinanceTheme
import com.example.automaticfinances.ui.theme.ThemeViewModel
import com.example.automaticfinances.ui.voice.VoiceEntryActivity
import com.example.automaticfinances.system.ServiceManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val themeViewModel: ThemeViewModel by viewModels()

    // POST_NOTIFICATIONS is runtime-gated on Android 13+. Without it the foreground service's
    // persistent notification (which hosts the "voz" quick action) and the voice/SMS feedback
    // notifications are silently suppressed, so we must actually ask for it.
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Outcome handled by the OS; the app degrades gracefully if denied. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()

        // Enable edge-to-edge display
        enableEdgeToEdge()

        // Configure system bars for immersive experience
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            FinanceTheme(
                darkTheme = themeViewModel.isDarkTheme(),
                useDynamicColor = themeViewModel.getUseDynamicColor()
            ) {
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

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}