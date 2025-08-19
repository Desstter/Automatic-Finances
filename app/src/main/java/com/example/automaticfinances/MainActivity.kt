package com.example.automaticfinances

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.automaticfinances.ui.HomeScreen
import com.example.automaticfinances.ui.HomeViewModel
import com.example.automaticfinances.ui.theme.AutomaticFinancesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AutomaticFinancesTheme {
                val vm: HomeViewModel = viewModel()
                HomeScreen(
                    stateFlow = vm.state,
                    onOpenNotifAccess = {
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                )
            }
        }
    }
}