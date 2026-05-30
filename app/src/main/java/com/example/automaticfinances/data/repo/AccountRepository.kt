package com.example.automaticfinances.data.repo

import com.example.automaticfinances.data.db.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AccountRepository @Inject constructor(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao
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
    
    // ========== ANALYTICS & INSIGHTS ==========
    
    suspend fun getAccountSummary(): AccountSummary? {
        return accountDao.getAccountSummary()?.toAccountSummary()
    }
    
    fun getAccountSummaryFlow(): Flow<AccountSummary?> {
        return accountDao.getAccountSummaryFlow().map { it?.toAccountSummary() }
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
    
    // ========== MAINTENANCE OPERATIONS ==========

    suspend fun deactivateAccount(accountId: Long) = accountDao.deactivateAccount(accountId)

    suspend fun activateAccount(accountId: Long) = accountDao.activateAccount(accountId)

    suspend fun getInactiveAccounts(): List<Account> = accountDao.getInactiveAccounts()
}