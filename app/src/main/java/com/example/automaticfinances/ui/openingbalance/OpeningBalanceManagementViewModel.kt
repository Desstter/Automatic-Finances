package com.example.automaticfinances.ui.openingbalance

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automaticfinances.data.db.AccountWithOpeningBalance
import com.example.automaticfinances.data.db.OpeningBalance
import com.example.automaticfinances.data.db.OpeningBalanceSummary
import com.example.automaticfinances.data.repo.OpeningBalanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OpeningBalanceManagementViewModel(
    private val openingBalanceRepository: OpeningBalanceRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(OpeningBalanceManagementState())
    val state: StateFlow<OpeningBalanceManagementState> = _state.asStateFlow()
    
    private val tag = "OpeningBalanceManagementVM"
    
    fun loadOpeningBalanceData() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true, error = null) }
                
                Log.d(tag, "Loading opening balance data")
                
                // Load accounts with opening balances
                val accountsWithOpeningBalance = openingBalanceRepository.calculateAllAccountBalancesWithOpening()
                
                // Load summary
                val summary = openingBalanceRepository.getOpeningBalanceSummary()
                
                // Load opening balance history
                val history = loadOpeningBalanceHistory()
                
                _state.update { 
                    it.copy(
                        accountsWithOpeningBalance = accountsWithOpeningBalance,
                        summary = summary,
                        openingBalanceHistory = history,
                        isLoading = false
                    )
                }
                
                Log.d(tag, "Loaded data: ${accountsWithOpeningBalance.size} accounts, ${history.size} history entries")
                
            } catch (e: Exception) {
                Log.e(tag, "Error loading opening balance data", e)
                _state.update { 
                    it.copy(
                        error = "Error cargando datos: ${e.message}",
                        isLoading = false
                    ) 
                }
            }
        }
    }
    
    private suspend fun loadOpeningBalanceHistory(): List<OpeningBalance> {
        return try {
            val allBalances = mutableListOf<OpeningBalance>()
            
            // Get all accounts and their opening balance history
            val accountsWithBalance = openingBalanceRepository.calculateAllAccountBalancesWithOpening()
            
            for (accountWithBalance in accountsWithBalance) {
                val history = openingBalanceRepository.getOpeningBalanceHistory(accountWithBalance.account.id)
                allBalances.addAll(history)
            }
            
            // Sort by creation date descending (most recent first)
            allBalances.sortedByDescending { it.createdAt }
            
        } catch (e: Exception) {
            Log.w(tag, "Error loading opening balance history", e)
            emptyList()
        }
    }
    
    fun recalculateBalances() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isRecalculating = true, error = null) }
                
                Log.d(tag, "Recalculating account balances")
                
                // Recalculate all account balances based on opening balances + transactions
                openingBalanceRepository.recalculateAccountBalances()
                
                // Reload the data to reflect changes
                loadOpeningBalanceData()
                
                _state.update { it.copy(isRecalculating = false) }
                
                Log.d(tag, "Account balances recalculated successfully")
                
            } catch (e: Exception) {
                Log.e(tag, "Error recalculating balances", e)
                _state.update { 
                    it.copy(
                        error = "Error recalculando balances: ${e.message}",
                        isRecalculating = false
                    ) 
                }
            }
        }
    }
    
    fun validateOpeningBalances() {
        viewModelScope.launch {
            try {
                Log.d(tag, "Validating opening balances")
                
                val issues = openingBalanceRepository.validateAndFixOpeningBalances()
                
                if (issues.isNotEmpty()) {
                    val issuesText = issues.joinToString("\n• ", "Problemas encontrados:\n• ")
                    _state.update { 
                        it.copy(
                            error = issuesText
                        ) 
                    }
                } else {
                    Log.d(tag, "No issues found with opening balances")
                }
                
            } catch (e: Exception) {
                Log.e(tag, "Error validating opening balances", e)
                _state.update { 
                    it.copy(
                        error = "Error validando balances: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    fun refreshData() {
        loadOpeningBalanceData()
    }
    
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
    
    // Analytics methods for getting insights
    fun getNetWorthProgress(): Pair<Long, Long>? {
        return try {
            val summary = _state.value.summary
            if (summary != null) {
                Pair(summary.totalCurrentBalanceCents, summary.totalOpeningBalanceCents)
            } else null
        } catch (e: Exception) {
            Log.w(tag, "Error calculating net worth progress", e)
            null
        }
    }
    
    fun getAccountPerformance(): List<AccountPerformance> {
        return try {
            _state.value.accountsWithOpeningBalance.map { accountWithBalance ->
                AccountPerformance(
                    accountName = accountWithBalance.account.name,
                    accountType = accountWithBalance.account.type,
                    openingBalanceCents = accountWithBalance.openingBalanceCents,
                    currentBalanceCents = accountWithBalance.currentBalanceCents,
                    netChangeCents = accountWithBalance.netChangeCents,
                    transactionCount = accountWithBalance.transactionsSinceOpening,
                    growthPercentage = if (accountWithBalance.openingBalanceCents != 0L) {
                        (accountWithBalance.netChangeCents.toFloat() / accountWithBalance.openingBalanceCents.toFloat()) * 100f
                    } else 0f
                )
            }.sortedByDescending { it.growthPercentage }
        } catch (e: Exception) {
            Log.w(tag, "Error calculating account performance", e)
            emptyList()
        }
    }
}

data class OpeningBalanceManagementState(
    val accountsWithOpeningBalance: List<AccountWithOpeningBalance> = emptyList(),
    val summary: OpeningBalanceSummary? = null,
    val openingBalanceHistory: List<OpeningBalance> = emptyList(),
    val isLoading: Boolean = false,
    val isRecalculating: Boolean = false,
    val error: String? = null
) {
    val hasOpeningBalances: Boolean
        get() = accountsWithOpeningBalance.any { it.hasOpeningBalance }
    
    val totalNetWorth: Long
        get() = summary?.totalCurrentBalanceCents ?: 0L
    
    val totalGrowth: Long
        get() = summary?.totalNetChangeCents ?: 0L
    
    val formattedNetWorth: String
        get() {
            val nf = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("es", "CO"))
            return nf.format(totalNetWorth / 100.0)
        }
    
    val formattedGrowth: String
        get() {
            val nf = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("es", "CO"))
            return nf.format(totalGrowth / 100.0)
        }
    
    val hasPositiveGrowth: Boolean
        get() = totalGrowth > 0L
    
    val recentOpeningBalanceChanges: List<OpeningBalance>
        get() = openingBalanceHistory.take(5)
}

data class AccountPerformance(
    val accountName: String,
    val accountType: com.example.automaticfinances.data.db.AccountType,
    val openingBalanceCents: Long,
    val currentBalanceCents: Long,
    val netChangeCents: Long,
    val transactionCount: Int,
    val growthPercentage: Float
) {
    val formattedOpeningBalance: String
        get() {
            val nf = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("es", "CO"))
            return nf.format(openingBalanceCents / 100.0)
        }
    
    val formattedCurrentBalance: String
        get() {
            val nf = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("es", "CO"))
            return nf.format(currentBalanceCents / 100.0)
        }
    
    val formattedNetChange: String
        get() {
            val nf = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("es", "CO"))
            return nf.format(netChangeCents / 100.0)
        }
    
    val hasPositiveGrowth: Boolean
        get() = netChangeCents > 0L
    
    val hasNegativeGrowth: Boolean
        get() = netChangeCents < 0L
    
    val isStable: Boolean
        get() = netChangeCents == 0L
}