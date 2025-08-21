package com.example.automaticfinances.ui.openingbalance

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automaticfinances.data.db.Account
import com.example.automaticfinances.data.db.AppDatabase
import com.example.automaticfinances.data.repo.OpeningBalanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class OpeningBalanceSetupViewModel(
    private val openingBalanceRepository: OpeningBalanceRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(OpeningBalanceSetupState())
    val state: StateFlow<OpeningBalanceSetupState> = _state.asStateFlow()
    
    private val tag = "OpeningBalanceSetupVM"
    
    fun loadAccounts() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true, error = null) }
                
                // Get all active accounts
                val accountDao = AppDatabase.get().accountDao()
                val accounts = accountDao.getAllActiveAccounts()
                
                // Load existing opening balances
                val existingBalances = openingBalanceRepository.getAllActiveOpeningBalances()
                val accountBalanceMap = mutableMapOf<Long, Double>()
                
                // Map existing opening balances
                for (openingBalance in existingBalances) {
                    accountBalanceMap[openingBalance.accountId] = openingBalance.balanceInPesos
                }
                
                // For accounts without opening balances, use current account balance
                for (account in accounts) {
                    if (!accountBalanceMap.containsKey(account.id)) {
                        accountBalanceMap[account.id] = account.balanceCents / 100.0
                    }
                }
                
                // Initialize default opening balances if none exist
                if (existingBalances.isEmpty()) {
                    createDefaultAccountsIfNeeded()
                }
                
                _state.update { 
                    it.copy(
                        accounts = accounts,
                        accountBalances = accountBalanceMap,
                        isLoading = false
                    )
                }
                
                Log.d(tag, "Loaded ${accounts.size} accounts")
                
            } catch (e: Exception) {
                Log.e(tag, "Error loading accounts", e)
                _state.update { 
                    it.copy(
                        error = "Error cargando cuentas: ${e.message}",
                        isLoading = false
                    ) 
                }
            }
        }
    }
    
    private suspend fun createDefaultAccountsIfNeeded() {
        // This would normally be handled by the AccountRepository
        // but for simplicity, we'll ensure default accounts exist
        try {
            if (!openingBalanceRepository.hasAnyOpeningBalances()) {
                openingBalanceRepository.initializeDefaultOpeningBalances()
            }
        } catch (e: Exception) {
            Log.w(tag, "Could not initialize default accounts", e)
        }
    }
    
    fun updateEffectiveDate(newDate: LocalDate) {
        _state.update { it.copy(effectiveDate = newDate) }
        Log.d(tag, "Updated effective date to: $newDate")
    }
    
    fun updateAccountBalance(accountId: Long, newBalance: Double) {
        _state.update { currentState ->
            val updatedBalances = currentState.accountBalances.toMutableMap()
            updatedBalances[accountId] = newBalance
            currentState.copy(accountBalances = updatedBalances)
        }
        Log.d(tag, "Updated balance for account $accountId: $newBalance")
    }
    
    fun saveOpeningBalances() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true, error = null) }
                
                val currentState = _state.value
                Log.d(tag, "Saving opening balances for ${currentState.accounts.size} accounts")
                
                // Save opening balance for each account
                for (account in currentState.accounts) {
                    val balance = currentState.accountBalances[account.id] ?: 0.0
                    val balanceCents = (balance * 100).toLong()
                    
                    openingBalanceRepository.updateOpeningBalance(
                        accountId = account.id,
                        newEffectiveDate = currentState.effectiveDate,
                        newBalanceCents = balanceCents,
                        newNote = "Balance inicial configurado por usuario"
                    )
                    
                    Log.d(tag, "Saved opening balance for ${account.name}: $balance")
                }
                
                // Recalculate account balances based on new opening balances
                openingBalanceRepository.recalculateAccountBalances()
                
                _state.update { 
                    it.copy(
                        isLoading = false,
                        isSetupComplete = true
                    ) 
                }
                
                Log.d(tag, "Opening balances saved successfully")
                
            } catch (e: Exception) {
                Log.e(tag, "Error saving opening balances", e)
                _state.update { 
                    it.copy(
                        error = "Error guardando balances: ${e.message}",
                        isLoading = false
                    ) 
                }
            }
        }
    }
    
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
    
    fun resetSetupComplete() {
        _state.update { it.copy(isSetupComplete = false) }
    }
}

data class OpeningBalanceSetupState(
    val accounts: List<Account> = emptyList(),
    val accountBalances: MutableMap<Long, Double> = mutableMapOf(),
    val effectiveDate: LocalDate = LocalDate.now(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSetupComplete: Boolean = false
) {
    val hasValidBalances: Boolean
        get() = accounts.isNotEmpty() && 
                accounts.all { account -> 
                    accountBalances[account.id]?.let { it >= 0.0 } == true
                }
    
    val totalBalance: Double
        get() = accountBalances.values.sum()
    
    val formattedTotalBalance: String
        get() {
            val nf = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("es", "CO"))
            return nf.format(totalBalance)
        }
}