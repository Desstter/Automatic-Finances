package com.example.automaticfinances.data.repo

import com.example.automaticfinances.data.db.*
import kotlinx.coroutines.flow.Flow
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

    // NOTE: balances are no longer mutated incrementally per transaction. The single source of
    // truth is the derived value (opening snapshot + movements); `account.balanceCents` is a
    // materialized cache recomputed wholesale via OpeningBalanceRepository.recalculateAccountBalance
    // after each insert/delete/restore. This removes the two-bookkeeping divergence risk (ARQ-1).

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
}