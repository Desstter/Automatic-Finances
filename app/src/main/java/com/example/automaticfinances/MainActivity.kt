package com.example.automaticfinances

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.example.automaticfinances.navigation.AppNavigation
import com.example.automaticfinances.ui.theme.AutomaticFinancesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AutomaticFinancesTheme {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    onOpenNotifAccess = {
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                )
            }
        }
    }
}