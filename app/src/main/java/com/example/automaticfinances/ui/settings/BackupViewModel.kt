package com.example.automaticfinances.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automaticfinances.data.backup.BackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Estado de la pantalla de copia de seguridad. */
sealed interface BackupUiState {
    data object Idle : BackupUiState
    data object Working : BackupUiState
    data object ExportDone : BackupUiState
    /** La restauración terminó: la UI debe avisar y reiniciar la app. */
    data object RestoreDone : BackupUiState
    data class Error(val message: String) : BackupUiState
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager,
) : ViewModel() {

    private val _state = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    fun suggestedFileName(): String = backupManager.suggestedFileName()

    fun export(destination: Uri) {
        if (_state.value == BackupUiState.Working) return
        viewModelScope.launch {
            _state.value = BackupUiState.Working
            val result = withContext(Dispatchers.IO) {
                runCatching { backupManager.export(destination) }
            }
            _state.value = result.fold(
                onSuccess = { BackupUiState.ExportDone },
                onFailure = { BackupUiState.Error(it.message ?: "No se pudo crear la copia") },
            )
        }
    }

    fun import(source: Uri) {
        if (_state.value == BackupUiState.Working) return
        viewModelScope.launch {
            _state.value = BackupUiState.Working
            val result = withContext(Dispatchers.IO) {
                runCatching { backupManager.import(source) }
            }
            _state.value = result.fold(
                onSuccess = { BackupUiState.RestoreDone },
                onFailure = { BackupUiState.Error(it.message ?: "No se pudo restaurar la copia") },
            )
        }
    }

    fun restartApp() = backupManager.restartApp()

    fun dismiss() {
        if (_state.value != BackupUiState.Working) _state.value = BackupUiState.Idle
    }
}
