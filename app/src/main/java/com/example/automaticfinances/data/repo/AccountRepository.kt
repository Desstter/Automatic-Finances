package com.example.automaticfinances.data.repo

import com.example.automaticfinances.data.db.*
import com.example.automaticfinances.data.models.MonthlySpending
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOf

class AccountRepository(
    private val accountDao: AccountDao = AppDatabase.get().accountDao(),
    private val transactionDao: TransactionDao = AppDatabase.get().transactionDao()
) {
    
    // ========== BASIC CRUD OPERATIONS ==========
    
    suspend fun getAllActiveAccounts(): List<Account> = accountDao.getAllActiveAccounts()
    
    fun getAllActiveAccountsFlow(): Flow<List<Account>> = accountDao.getAllActiveAccountsFlow()
    
    suspend fun getAccountById(accountId: Long): Account? = accountDao.getAccountById(accountId)
    
    fun getAccountByIdFlow(accountId: Long): Flow<Account?> = accountDao.getAccountByIdFlow(accountId)
    
    suspend fun getAccountsByType(type: AccountType): List<Account> = accountDao.getAccountsByType(type)
    
    suspend fun getBankAccount(): Account? = accountDao.getAccountByName("Banco")
    
    suspend fun getCashAccount(): Account? = accountDao.getAccountByName("Efectivo")
    
    suspend fun createAccount(account: Account): Long = accountDao.insertAccount(account)
    
    suspend fun updateAccount(account: Account) = accountDao.updateAccount(account)
    
    suspend fun deleteAccount(account: Account) = accountDao.deleteAccount(account)
    
    // ========== BALANCE MANAGEMENT ==========
    
    suspend fun getAccountBalance(accountId: Long): Long? = accountDao.getAccountBalance(accountId)
    
    suspend fun updateAccountBalance(accountId: Long, newBalanceCents: Long) {
        accountDao.updateAccountBalance(accountId, newBalanceCents)
    }
    
    suspend fun adjustAccountBalance(accountId: Long, amountCents: Long) {
        accountDao.adjustAccountBalance(accountId, amountCents)
    }
    
    /**
     * Updates account balance when a transaction is added
     * For expenses: balance decreases (negative amount)
     * For income: balance increases (positive amount)
     */
    suspend fun applyTransactionToBalance(transaction: Transaction) {
        transaction.accountId?.let { accountId ->
            val adjustmentAmount = if (transaction.isIncome) {
                transaction.amountCents // Income increases balance
            } else {
                -transaction.amountCents // Expense decreases balance
            }
            adjustAccountBalance(accountId, adjustmentAmount)
        }
    }
    
    /**
     * Reverts account balance when a transaction is deleted
     */
    suspend fun revertTransactionFromBalance(transaction: Transaction) {
        transaction.accountId?.let { accountId ->
            val adjustmentAmount = if (transaction.isIncome) {
                -transaction.amountCents // Revert income
            } else {
                transaction.amountCents // Revert expense
            }
            adjustAccountBalance(accountId, adjustmentAmount)
        }
    }
    
    /**
     * Updates account balance when a transaction is modified
     */
    suspend fun updateTransactionBalance(oldTransaction: Transaction, newTransaction: Transaction) {
        // Revert old transaction
        revertTransactionFromBalance(oldTransaction)
        
        // Apply new transaction
        applyTransactionToBalance(newTransaction)
    }
    
    // ========== ANALYTICS & INSIGHTS ==========
    
    suspend fun getAccountSummary(): AccountSummary? {
        return accountDao.getAccountSummary()?.toAccountSummary()
    }
    
    fun getAccountSummaryFlow(): Flow<AccountSummary?> {
        return accountDao.getAccountSummaryFlow().map { it?.toAccountSummary() }
    }
    
    suspend fun getAccountsWithStats(): List<AccountWithStats> {
        return accountDao.getAccountsWithMonthlyStats()
    }
    
    fun getAccountBalances(): Flow<List<AccountBalance>> {
        return getAllActiveAccountsFlow().map { accounts ->
            accounts.map { account ->
                AccountBalance(
                    account = account,
                    currentBalanceCents = account.balanceCents,
                    previousBalanceCents = 0L, // TODO: Implement historical balance tracking
                    changeAmountCents = 0L,
                    changePercentage = 0f,
                    lastTransactionDate = null // TODO: Get from transaction history
                )
            }
        }
    }
    
    suspend fun getTotalBalance(): Long {
        return getAccountSummary()?.totalBalanceCents ?: 0L
    }
    
    suspend fun getBankBalance(): Long {
        return getBankAccount()?.balanceCents ?: 0L
    }
    
    suspend fun getCashBalance(): Long {
        return getCashAccount()?.balanceCents ?: 0L
    }
    
    fun getTotalBalanceFlow(): Flow<Long> {
        return getAccountSummaryFlow().map { summary ->
            summary?.totalBalanceCents ?: 0L
        }
    }
    
    fun getBankBalanceFlow(): Flow<Long> {
        return getAccountSummaryFlow().map { summary ->
            summary?.bankBalanceCents ?: 0L
        }
    }
    
    fun getCashBalanceFlow(): Flow<Long> {
        return getAccountSummaryFlow().map { summary ->
            summary?.cashBalanceCents ?: 0L
        }
    }
    
    // ========== ACCOUNT ASSIGNMENT LOGIC ==========
    
    /**
     * Determines which account a transaction should be assigned to
     * SMS transactions → Bank account
     * Manual transactions → Cash account
     */
    suspend fun getAccountForTransaction(source: String): Account? {
        return if (source == "notif:sms") {
            getBankAccount()
        } else {
            getCashAccount()
        }
    }
    
    /**
     * Auto-assigns account to transaction based on source
     */
    suspend fun assignAccountToTransaction(transaction: Transaction): Transaction {
        val account = getAccountForTransaction(transaction.source)
        return transaction.copy(accountId = account?.id)
    }
    
    // ========== INITIALIZATION & SETUP ==========
    
    suspend fun initializeDefaultAccounts(): Boolean {
        return try {
            val accountCount = accountDao.getAccountCount()
            if (accountCount == 0) {
                val accountIds = accountDao.setupDefaultAccounts()
                accountDao.initializeAccountBalances()
                accountIds.isNotEmpty()
            } else {
                true // Already initialized
            }
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun recalculateAllBalances() {
        try {
            accountDao.initializeAccountBalances()
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    // ========== MAINTENANCE OPERATIONS ==========
    
    suspend fun deactivateAccount(accountId: Long) = accountDao.deactivateAccount(accountId)
    
    suspend fun activateAccount(accountId: Long) = accountDao.activateAccount(accountId)
    
    suspend fun getInactiveAccounts(): List<Account> = accountDao.getInactiveAccounts()
    
    /**
     * Validates that account balances match transaction history
     */
    suspend fun validateAccountBalances(): Map<Long, Boolean> {
        val accounts = getAllActiveAccounts()
        val validationResults = mutableMapOf<Long, Boolean>()
        
        for (account in accounts) {
            val calculatedBalance = if (account.type == AccountType.BANK) {
                accountDao.calculateBankBalanceFromTransactions()
            } else {
                accountDao.calculateCashBalanceFromTransactions()
            }
            
            validationResults[account.id] = account.balanceCents == calculatedBalance
        }
        
        return validationResults
    }
    
    // ========== BALANCE HISTORY TRACKING ==========
    
    suspend fun recordBalanceChange(
        accountId: Long,
        transactionId: String?,
        changeType: BalanceChangeType
    ) {
        // TODO: Implement balance history tracking
        // This would create entries in a balance_history table
        // to track all balance changes over time
    }
    
    // ========== ACCOUNT INSIGHTS ==========
    
    fun getAccountSpendingTrend(accountId: Long): Flow<List<MonthlySpending>> {
        // TODO: Implement monthly spending trend for specific account
        // This would analyze spending patterns per account over time
        return flowOf(emptyList()) // Placeholder implementation
    }
    
    suspend fun getAccountTransactionStats(accountId: Long): AccountTransactionStats? {
        // TODO: Implement comprehensive transaction statistics per account
        return null
    }
}

// Additional data classes for account analytics
data class AccountTransactionStats(
    val accountId: Long,
    val totalTransactions: Int,
    val totalIncomeCents: Long,
    val totalExpensesCents: Long,
    val averageTransactionCents: Long,
    val lastTransactionDate: Long?,
    val mostFrequentCategory: String?
)