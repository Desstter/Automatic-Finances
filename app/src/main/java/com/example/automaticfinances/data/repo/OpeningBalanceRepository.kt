package com.example.automaticfinances.data.repo

import android.util.Log
import com.example.automaticfinances.data.db.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class OpeningBalanceRepository(
    private val openingBalanceDao: OpeningBalanceDao,
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao
) {
    private val tag = "OpeningBalanceRepository"
    
    // ================ BASIC OPERATIONS ================
    
    suspend fun createOpeningBalance(
        accountId: Long,
        effectiveDate: LocalDate,
        balanceCents: Long,
        note: String = ""
    ): Long {
        Log.d(tag, "Creating opening balance for account $accountId: ${balanceCents/100.0} on $effectiveDate")
        
        val openingBalance = OpeningBalance.create(
            accountId = accountId,
            effectiveDate = effectiveDate,
            balanceCents = balanceCents,
            note = note
        )
        
        return openingBalanceDao.setNewOpeningBalance(openingBalance).let { 
            // setNewOpeningBalance doesn't return the ID, so we fetch it
            openingBalanceDao.getActiveByAccount(accountId)?.id ?: 0L
        }
    }
    
    suspend fun updateOpeningBalance(
        accountId: Long,
        newEffectiveDate: LocalDate,
        newBalanceCents: Long,
        newNote: String = ""
    ): Long {
        Log.d(tag, "Updating opening balance for account $accountId")
        
        // Create new opening balance (this will deactivate the old one)
        return createOpeningBalance(accountId, newEffectiveDate, newBalanceCents, newNote)
    }
    
    suspend fun deleteOpeningBalance(accountId: Long) {
        Log.d(tag, "Deleting opening balance for account $accountId")
        openingBalanceDao.deactivateByAccount(accountId)
    }
    
    // ================ QUERY OPERATIONS ================
    
    suspend fun getOpeningBalanceForAccount(accountId: Long): OpeningBalance? {
        return openingBalanceDao.getActiveByAccount(accountId)
    }
    
    fun getOpeningBalanceForAccountFlow(accountId: Long): Flow<OpeningBalance?> {
        return openingBalanceDao.getActiveByAccountFlow(accountId)
    }
    
    suspend fun getAllActiveOpeningBalances(): List<OpeningBalance> {
        return openingBalanceDao.getAllActiveSync()
    }
    
    fun getAllActiveOpeningBalancesFlow(): Flow<List<OpeningBalance>> {
        return openingBalanceDao.getAllActive()
    }
    
    suspend fun getOpeningBalanceHistory(accountId: Long): List<OpeningBalance> {
        return openingBalanceDao.getHistoryByAccount(accountId)
    }
    
    // ================ ACCOUNT-SPECIFIC OPERATIONS ================
    
    suspend fun getBankOpeningBalance(): OpeningBalance? {
        return openingBalanceDao.getActiveBankOpeningBalance()
    }
    
    suspend fun getCashOpeningBalance(): OpeningBalance? {
        return openingBalanceDao.getActiveCashOpeningBalance()
    }
    
    suspend fun hasOpeningBalance(accountId: Long): Boolean {
        return openingBalanceDao.hasActiveOpeningBalance(accountId)
    }
    
    suspend fun hasAnyOpeningBalances(): Boolean {
        return openingBalanceDao.getActiveOpeningBalanceCount() > 0
    }
    
    // ================ CALCULATION OPERATIONS ================
    
    suspend fun calculateCurrentBalanceWithOpening(accountId: Long): AccountWithOpeningBalance {
        val account = accountDao.getAccountById(accountId) 
            ?: throw IllegalArgumentException("Account not found: $accountId")
        
        val openingBalance = openingBalanceDao.getActiveByAccount(accountId)
        
        val currentBalanceCents = if (openingBalance != null) {
            // Calculate based on opening balance + transactions since effective date
            val transactionsSince = getTransactionsSinceDate(accountId, openingBalance.effectiveDate)
            val netTransactionAmount = transactionsSince.sumOf { 
                if (it.isIncome) it.amountCents else -it.amountCents 
            }
            openingBalance.balanceCents + netTransactionAmount
        } else {
            // Use account's current balance (legacy behavior)
            account.balanceCents
        }
        
        val transactionCount = if (openingBalance != null) {
            getTransactionCountSinceDate(accountId, openingBalance.effectiveDate)
        } else 0
        
        return AccountWithOpeningBalance(
            account = account,
            openingBalance = openingBalance,
            currentBalanceCents = currentBalanceCents,
            transactionsSinceOpening = transactionCount
        )
    }
    
    suspend fun calculateAllAccountBalancesWithOpening(): List<AccountWithOpeningBalance> {
        val accounts = accountDao.getAllActiveAccounts()
        return accounts.map { account ->
            calculateCurrentBalanceWithOpening(account.id)
        }
    }
    
    suspend fun getOpeningBalanceSummary(): OpeningBalanceSummary {
        val summaryRaw = openingBalanceDao.getOpeningBalanceSummaryRaw()
        
        if (summaryRaw == null) {
            // No opening balances found, return defaults
            val accounts = accountDao.getAllActiveAccounts()
            val totalCurrent = accounts.sumOf { it.balanceCents }
            val bankCurrent = accounts.filter { it.type == AccountType.BANK }.sumOf { it.balanceCents }
            val cashCurrent = accounts.filter { it.type == AccountType.CASH }.sumOf { it.balanceCents }
            
            return OpeningBalanceSummary(
                totalOpeningBalanceCents = 0L,
                totalCurrentBalanceCents = totalCurrent,
                totalNetChangeCents = totalCurrent,
                bankOpeningBalanceCents = 0L,
                cashOpeningBalanceCents = 0L,
                bankCurrentBalanceCents = bankCurrent,
                cashCurrentBalanceCents = cashCurrent,
                accountsWithOpeningBalance = 0
            )
        }
        
        // Recalculate current balances based on opening balance + transactions
        val accountsWithOpening = calculateAllAccountBalancesWithOpening()
        
        val totalCurrentRecalculated = accountsWithOpening.sumOf { it.currentBalanceCents }
        val bankCurrentRecalculated = accountsWithOpening
            .filter { it.account.type == AccountType.BANK }
            .sumOf { it.currentBalanceCents }
        val cashCurrentRecalculated = accountsWithOpening
            .filter { it.account.type == AccountType.CASH }
            .sumOf { it.currentBalanceCents }
        
        return OpeningBalanceSummary(
            totalOpeningBalanceCents = summaryRaw.totalOpening,
            totalCurrentBalanceCents = totalCurrentRecalculated,
            totalNetChangeCents = totalCurrentRecalculated - summaryRaw.totalOpening,
            bankOpeningBalanceCents = summaryRaw.bankOpening,
            cashOpeningBalanceCents = summaryRaw.cashOpening,
            bankCurrentBalanceCents = bankCurrentRecalculated,
            cashCurrentBalanceCents = cashCurrentRecalculated,
            accountsWithOpeningBalance = summaryRaw.accountsWithOpening,
            effectiveDate = summaryRaw.mostRecentEffectiveDate
        )
    }
    
    // ================ SETUP AND VALIDATION ================
    
    suspend fun initializeDefaultOpeningBalances() {
        Log.d(tag, "Initializing default opening balances")
        
        val accounts = accountDao.getAllActiveAccounts()
        val today = LocalDate.now()
        
        for (account in accounts) {
            if (!hasOpeningBalance(account.id)) {
                Log.d(tag, "Creating default opening balance for ${account.name}")
                createOpeningBalance(
                    accountId = account.id,
                    effectiveDate = today,
                    balanceCents = account.balanceCents, // Use current balance as opening
                    note = "Balance inicial automático"
                )
            }
        }
    }
    
    suspend fun validateAndFixOpeningBalances(): List<String> {
        val issues = mutableListOf<String>()
        
        // Check for accounts without opening balances
        val accounts = accountDao.getAllActiveAccounts()
        val accountsWithoutOpening = accounts.filter { account ->
            !hasOpeningBalance(account.id)
        }
        
        if (accountsWithoutOpening.isNotEmpty()) {
            issues.add("${accountsWithoutOpening.size} cuentas sin balance inicial")
        }
        
        // Check for inconsistent dates
        val openingBalances = getAllActiveOpeningBalances()
        val distinctDates = openingBalances.map { it.effectiveDate }.distinct()
        if (distinctDates.size > 1) {
            issues.add("Fechas efectivas inconsistentes: ${distinctDates.joinToString()}")
        }
        
        return issues
    }
    
    // ================ HELPER METHODS ================
    
    private suspend fun getTransactionsSinceDate(accountId: Long, effectiveDate: String): List<Transaction> {
        val endDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        return transactionDao.getByAccountAndDateRangeSync(accountId, effectiveDate, endDate)
    }
    
    private suspend fun getTransactionCountSinceDate(accountId: Long, effectiveDate: String): Int {
        val endDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        return transactionDao.getTransactionCountByAccountAndDateRange(accountId, effectiveDate, endDate)
    }
    
    // ================ BALANCE RECALCULATION ================
    
    suspend fun recalculateAccountBalances() {
        Log.d(tag, "Recalculating all account balances based on opening balances")
        
        val accountsWithOpening = calculateAllAccountBalancesWithOpening()
        
        for (accountBalance in accountsWithOpening) {
            // Update the account's balance in the database
            val updatedAccount = accountBalance.account.copy(
                balanceCents = accountBalance.currentBalanceCents
            )
            accountDao.updateAccount(updatedAccount)
            
            Log.d(tag, "Updated ${accountBalance.account.name}: ${accountBalance.formattedCurrentBalance}")
        }
    }
    
    suspend fun getNetWorthCalculation(): Pair<Long, Long> {
        val summary = getOpeningBalanceSummary()
        val currentNetWorth = summary.totalCurrentBalanceCents
        val initialNetWorth = summary.totalOpeningBalanceCents
        
        return Pair(currentNetWorth, initialNetWorth)
    }
}