package com.example.automaticfinances.ui.unparsed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automaticfinances.data.db.UnparsedSms
import com.example.automaticfinances.data.repo.UnparsedSmsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UnparsedSmsViewModel @Inject constructor(
    private val repo: UnparsedSmsRepository
) : ViewModel() {

    val items: StateFlow<List<UnparsedSms>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(id: String) {
        viewModelScope.launch { repo.delete(id) }
    }

    fun clearAll() {
        viewModelScope.launch { repo.clearAll() }
    }
}
