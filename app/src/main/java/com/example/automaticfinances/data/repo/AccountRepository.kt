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
        return combine(
            getAllActiveAccountsFlow(),
            transactionDao.getAllTransactionsFlow()
        ) { accounts, transactions ->
            accounts.map { account ->
                // Get last transaction date for this account
                val lastTransaction = transactions
                    .filter { it.accountId == account.id }
                    .maxByOrNull { it.ts }
                
                // Calculate previous balance (current - last transaction impact)
                val lastTransactionAmount = lastTransaction?.let { tx ->
                    if (tx.isIncome) tx.amountCents else -tx.amountCents
                } ?: 0L
                val previousBalance = account.balanceCents - lastTransactionAmount
                
                val changeAmount = account.balanceCents - previousBalance
                val changePercentage = if (previousBalance != 0L) {
                    (changeAmount.toFloat() / previousBalance.toFloat()) * 100f
                } else 0f
                
                AccountBalance(
                    account = account,
                    currentBalanceCents = account.balanceCents,
                    previousBalanceCents = previousBalance,
                    changeAmountCents = changeAmount,
                    changePercentage = changePercentage,
                    lastTransactionDate = lastTransaction?.ts
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
                // Only initialize balances for brand new accounts
                accountDao.initializeAccountBalances()
                accountIds.isNotEmpty()
            } else {
                true // Already initialized - don't recalculate balances
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
        // Balance history tracking - implemented as basic logging for now
        // In a full implementation, this would:
        // 1. Create a BalanceHistory entity
        // 2. Record timestamp, old balance, new balance, change reason
        // 3. Implement retention policies for historical data
        
        // For now, we're tracking balance changes implicitly through transactions
        // The getAccountBalances() function calculates historical context
        
        // Placeholder: Could log balance changes or trigger balance audit
        // Log.d("AccountRepository", "Balance change recorded: accountId=$accountId, type=$changeType")
    }
    
    // ========== ACCOUNT INSIGHTS ==========
    
    fun getAccountSpendingTrend(accountId: Long): Flow<List<MonthlySpending>> {
        return transactionDao.getAllTransactionsFlow().map { transactions ->
            transactions
                .filter { it.accountId == accountId && !it.isIncome }
                .groupBy { it.date.substring(0, 7) } // Group by YYYY-MM
                .map { (yearMonth, transactionsInMonth) ->
                    val totalCents = transactionsInMonth.sumOf { it.amountCents }
                    val transactionCount = transactionsInMonth.size
                    val averageDaily = if (transactionCount > 0) totalCents / 30 else 0L
                    
                    MonthlySpending(
                        yearMonth = java.time.YearMonth.parse(yearMonth),
                        totalCents = totalCents,
                        transactionCount = transactionCount,
                        averageDailySpending = averageDaily
                    )
                }
                .sortedBy { it.yearMonth }
        }
    }
    
    suspend fun getAccountTransactionStats(accountId: Long): AccountTransactionStats? {
        val transactions = transactionDao.getTransactionsByAccountId(accountId)
        if (transactions.isEmpty()) return null
        
        val incomeTransactions = transactions.filter { it.isIncome }
        val expenseTransactions = transactions.filter { !it.isIncome }
        
        val totalIncome = incomeTransactions.sumOf { it.amountCents }
        val totalExpenses = expenseTransactions.sumOf { it.amountCents }
        val totalAmount = totalIncome + totalExpenses
        val averageTransaction = if (transactions.isNotEmpty()) totalAmount / transactions.size else 0L
        
        // Find most frequent category
        val categoryFrequency = transactions
            .groupingBy { it.categoryId }
            .eachCount()
        val mostFrequentCategoryId = categoryFrequency.maxByOrNull { it.value }?.key
        val mostFrequentCategory = mostFrequentCategoryId?.let { 
            // This would need category lookup - using placeholder
            "Category $it"
        }
        
        val lastTransaction = transactions.maxByOrNull { it.ts }
        
        return AccountTransactionStats(
            accountId = accountId,
            totalTransactions = transactions.size,
            totalIncomeCents = totalIncome,
            totalExpensesCents = totalExpenses,
            averageTransactionCents = averageTransaction,
            lastTransactionDate = lastTransaction?.ts,
            mostFrequentCategory = mostFrequentCategory
        )
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