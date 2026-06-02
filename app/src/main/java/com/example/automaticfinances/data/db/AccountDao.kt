package com.example.automaticfinances.data.db

import androidx.room.*
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    
    // ========== BASIC CRUD OPERATIONS ==========
    
    @Query("SELECT * FROM accounts WHERE isActive = 1 ORDER BY type ASC, name ASC")
    suspend fun getAllActiveAccounts(): List<Account>
    
    @Query("SELECT * FROM accounts WHERE isActive = 1 ORDER BY type ASC, name ASC")
    fun getAllActiveAccountsFlow(): Flow<List<Account>>
    
    @Query("SELECT * FROM accounts WHERE id = :accountId")
    suspend fun getAccountById(accountId: Long): Account?
    
    @Query("SELECT * FROM accounts WHERE id = :accountId")
    fun getAccountByIdFlow(accountId: Long): Flow<Account?>
    
    @Query("SELECT * FROM accounts WHERE type = :type AND isActive = 1")
    suspend fun getAccountsByType(type: AccountType): List<Account>
    
    @Query("SELECT * FROM accounts WHERE name = :name LIMIT 1")
    suspend fun getAccountByName(name: String): Account?
    
    @Insert
    suspend fun insertAccount(account: Account): Long
    
    @Update
    suspend fun updateAccount(account: Account)
    
    @Delete
    suspend fun deleteAccount(account: Account)
    
    // ========== BALANCE OPERATIONS ==========
    
    @Query("SELECT balanceCents FROM accounts WHERE id = :accountId")
    suspend fun getAccountBalance(accountId: Long): Long?
    
    // The stored balanceCents is a materialized cache of the derived balance
    // (opening snapshot + movements). It is overwritten wholesale by a recompute, never
    // mutated incrementally — see OpeningBalanceRepository.recalculateAccountBalance.
    @Query("UPDATE accounts SET balanceCents = :balanceCents WHERE id = :accountId")
    suspend fun updateAccountBalance(accountId: Long, balanceCents: Long)

    // ========== ANALYTICS QUERIES ==========

    // Get account with transaction count for the current month
    @Query("""
        SELECT a.*, 
               COUNT(t.id) as transactionCount,
               COALESCE(SUM(CASE WHEN t.isIncome = 0 THEN t.amountCents ELSE 0 END), 0) as totalExpensesCents,
               COALESCE(SUM(CASE WHEN t.isIncome = 1 THEN t.amountCents ELSE 0 END), 0) as totalIncomeCents
        FROM accounts a
        LEFT JOIN transactions t ON a.id = t.accountId 
            AND t.date >= date('now', 'start of month') 
            AND t.date < date('now', 'start of month', '+1 month')
        WHERE a.isActive = 1
        GROUP BY a.id
        ORDER BY a.type ASC, a.name ASC
    """)
    suspend fun getAccountsWithMonthlyStats(): List<AccountWithStats>
    
    // ========== ACCOUNT SETUP OPERATIONS ==========
    
    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun getAccountCount(): Int
    
    @Transaction
    suspend fun setupDefaultAccounts(): List<Long> {
        val bankAccount = Account.createBankAccount("Banco")
        val cashAccount = Account.createCashAccount("Efectivo")
        
        return listOf(
            insertAccount(bankAccount),
            insertAccount(cashAccount)
        )
    }
    
    @Transaction
    suspend fun initializeAccountBalances() {
        // Only initialize balances if accounts have zero balance (first time setup)
        getAccountByName("Banco")?.let { account ->
            if (account.balanceCents == 0L) {
                val bankBalance = calculateBankBalanceFromTransactions()
                updateAccountBalance(account.id, bankBalance)
            }
        }
        
        getAccountByName("Efectivo")?.let { account ->
            if (account.balanceCents == 0L) {
                val cashBalance = calculateCashBalanceFromTransactions()
                updateAccountBalance(account.id, cashBalance)
            }
        }
    }
    
    @Query("""
        SELECT COALESCE(
            SUM(CASE 
                WHEN isIncome = 1 THEN amountCents 
                ELSE -amountCents 
            END), 0
        ) 
        FROM transactions 
        WHERE source = 'notif:sms'
    """)
    suspend fun calculateBankBalanceFromTransactions(): Long
    
    @Query("""
        SELECT COALESCE(
            SUM(CASE 
                WHEN isIncome = 1 THEN amountCents 
                ELSE -amountCents 
            END), 0
        ) 
        FROM transactions 
        WHERE source != 'notif:sms'
    """)
    suspend fun calculateCashBalanceFromTransactions(): Long
    
    // ========== MAINTENANCE OPERATIONS ==========
    
    @Query("UPDATE accounts SET isActive = 0 WHERE id = :accountId")
    suspend fun deactivateAccount(accountId: Long)
    
    @Query("UPDATE accounts SET isActive = 1 WHERE id = :accountId")
    suspend fun activateAccount(accountId: Long)
    
    @Query("SELECT * FROM accounts WHERE isActive = 0")
    suspend fun getInactiveAccounts(): List<Account>
}

// Data classes for complex queries
data class AccountWithStats(
    val id: Long,
    val name: String,
    val type: AccountType,
    val balanceCents: Long,
    val isActive: Boolean,
    val createdAt: Long,
    val transactionCount: Int,
    val totalExpensesCents: Long,
    val totalIncomeCents: Long
) {
    val netActivityCents: Long get() = totalIncomeCents - totalExpensesCents
    val hasActivity: Boolean get() = transactionCount > 0
}