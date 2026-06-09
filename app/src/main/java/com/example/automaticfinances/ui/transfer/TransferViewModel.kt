package com.example.automaticfinances.ui.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automaticfinances.data.db.Account
import com.example.automaticfinances.data.db.AccountType
import com.example.automaticfinances.data.repo.AccountRepository
import com.example.automaticfinances.domain.TransferUseCase
import com.example.automaticfinances.utils.parseColombiaCents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransferState(
    val amount: String = "",
    val accounts: List<Account> = emptyList(),
    val originAccountId: Long? = null,
    val destAccountId: Long? = null,
    val note: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val amountError: String? = null,
) {
    val originAccount: Account? get() = accounts.find { it.id == originAccountId }
    val destAccount: Account? get() = accounts.find { it.id == destAccountId }
    val amountCents: Long get() = amount.parseColombiaCents() ?: 0L

    val canSave: Boolean
        get() = amountCents > 0 &&
            amountError == null &&
            originAccountId != null &&
            destAccountId != null &&
            originAccountId != destAccountId &&
            !isLoading
}

@HiltViewModel
class TransferViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transferUseCase: TransferUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(TransferState())
    val state: StateFlow<TransferState> = _state.asStateFlow()

    init {
        loadAccounts()
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            val accounts = accountRepository.getAllActiveAccounts()
            // Default to the most common move: Banco -> Efectivo (e.g. an ATM withdrawal recorded
            // by hand), falling back to the first two distinct accounts.
            val defaultOrigin = accounts.firstOrNull { it.type == AccountType.BANK } ?: accounts.firstOrNull()
            val defaultDest = accounts.firstOrNull { it.type == AccountType.CASH && it.id != defaultOrigin?.id }
                ?: accounts.firstOrNull { it.id != defaultOrigin?.id }
            _state.value = _state.value.copy(
                accounts = accounts,
                originAccountId = _state.value.originAccountId ?: defaultOrigin?.id,
                destAccountId = _state.value.destAccountId ?: defaultDest?.id,
            )
        }
    }

    fun setAmount(raw: String) {
        val clean = raw.filter { it.isDigit() || it == '.' || it == ',' }.take(15)
        _state.value = _state.value.copy(
            amount = clean,
            amountError = if (clean.isNotEmpty() && (clean.parseColombiaCents() ?: 0L) <= 0L) {
                "Monto inválido"
            } else null,
        )
    }

    fun setOrigin(accountId: Long) {
        val s = _state.value
        // Keep origin != destination: if the user picks the current destination as origin, swap.
        val newDest = if (s.destAccountId == accountId) s.originAccountId else s.destAccountId
        _state.value = s.copy(originAccountId = accountId, destAccountId = newDest)
    }

    fun setDest(accountId: Long) {
        val s = _state.value
        val newOrigin = if (s.originAccountId == accountId) s.destAccountId else s.originAccountId
        _state.value = s.copy(destAccountId = accountId, originAccountId = newOrigin)
    }

    fun swap() {
        val s = _state.value
        _state.value = s.copy(originAccountId = s.destAccountId, destAccountId = s.originAccountId)
    }

    fun setNote(note: String) {
        _state.value = _state.value.copy(note = note.take(100))
    }

    fun save() {
        val s = _state.value
        if (!s.canSave) return
        val origin = s.originAccountId ?: return
        val dest = s.destAccountId ?: return

        _state.value = s.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val result = transferUseCase(
                originAccountId = origin,
                destAccountId = dest,
                amountCents = s.amountCents,
                ts = System.currentTimeMillis(),
                note = s.note.trim(),
            )
            _state.value = when (result) {
                is TransferUseCase.Result.Success ->
                    _state.value.copy(isLoading = false, isSuccess = true)
                is TransferUseCase.Result.Failure ->
                    _state.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    fun resetSuccess() {
        _state.value = _state.value.copy(isSuccess = false)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
