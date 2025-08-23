package com.example.automaticfinances

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.example.automaticfinances.navigation.AppNavigation
import com.example.automaticfinances.ui.theme.FinanceTheme
import com.example.automaticfinances.ui.theme.ThemeViewModel
import com.example.automaticfinances.ui.theme.ThemeViewModelFactory

class MainActivity : ComponentActivity() {
    
    // Theme ViewModel para gestionar el estado del tema
    private val themeViewModel: ThemeViewModel by lazy {
        ThemeViewModelFactory(this).create()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                )
            }
        }
    }
}