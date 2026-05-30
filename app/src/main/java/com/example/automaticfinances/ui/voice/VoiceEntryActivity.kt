package com.example.automaticfinances.ui.voice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.example.automaticfinances.ui.theme.FinanceTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

/**
 * Translucent overlay launched from the persistent notification's "voz" action. It hosts the
 * voice-entry Compose sheet, drives the RECORD_AUDIO runtime permission, and finishes itself once
 * the flow is dismissed or the transaction(s) are saved. All persistence goes through the shared
 * [com.example.automaticfinances.domain.AddTransactionUseCase] via the ViewModel.
 */
@AndroidEntryPoint
class VoiceEntryActivity : ComponentActivity() {

    private val viewModel: VoiceEntryViewModel by viewModels()

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.onMicPermissionGranted() else viewModel.onMicPermissionDenied()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FinanceTheme {
                val state by viewModel.state.collectAsState()
                val categories by viewModel.categories.collectAsState()

                // Auto-dismiss shortly after a successful save so the user gets confirmation.
                LaunchedEffect(state) {
                    if (state is VoiceUiState.Saved) {
                        delay(900)
                        finish()
                    }
                }

                VoiceEntryScreen(
                    state = state,
                    categories = categories,
                    onDismiss = { finish() },
                    onRetry = viewModel::retry,
                    onOpenAppSettings = ::openAppSettings,
                    onConfirmSave = viewModel::confirmSave,
                    onAmountChange = viewModel::updateDraftAmount,
                    onDescriptionChange = viewModel::updateDraftDescription,
                    onCategorySelect = viewModel::updateDraftCategory,
                    onIncomeToggle = viewModel::setDraftIsIncome,
                    onRemoveDraft = viewModel::removeDraft,
                )
            }
        }

        // Only kick off the mic flow on first creation; rotations keep ViewModel state.
        if (savedInstanceState == null) {
            ensureMicPermission()
        }
    }

    /**
     * `singleTop` means a second notification tap re-delivers here instead of stacking. If the
     * previous flow already finished its work this is a fresh start, so re-request the mic.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (viewModel.state.value.let { it is VoiceUiState.Saved || it is VoiceUiState.Failed }) {
            viewModel.retry()
        }
    }

    private fun ensureMicPermission() {
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            viewModel.onMicPermissionGranted()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }
}
