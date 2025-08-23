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
    
    @Query("UPDATE accounts SET balanceCents = :balanceCents WHERE id = :accountId")
    suspend fun updateAccountBalance(accountId: Long, balanceCents: Long)
    
    @Query("""
        UPDATE accounts 
        SET balanceCents = balanceCents + :amountCents 
        WHERE id = :accountId
    """)
    suspend fun adjustAccountBalance(accountId: Long, amountCents: Long)
    
    // ========== ANALYTICS QUERIES ==========
    
    @Query("""
        SELECT 
            SUM(CASE WHEN type = 'BANK' THEN balanceCents ELSE 0 END) as bankTotal,
            SUM(CASE WHEN type = 'CASH' THEN balanceCents ELSE 0 END) as cashTotal,
            SUM(balanceCents) as totalBalance,
            COUNT(*) as accountCount,
            COUNT(CASE WHEN isActive = 1 THEN 1 END) as activeCount
        FROM accounts
    """)
    suspend fun getAccountSummary(): AccountSummaryRaw?
    
    @Query("""
        SELECT 
            SUM(CASE WHEN type = 'BANK' THEN balanceCents ELSE 0 END) as bankTotal,
            SUM(CASE WHEN type = 'CASH' THEN balanceCents ELSE 0 END) as cashTotal,
            SUM(balanceCents) as totalBalance,
            COUNT(*) as accountCount,
            COUNT(CASE WHEN isActive = 1 THEN 1 END) as activeCount
        FROM accounts
    """)
    fun getAccountSummaryFlow(): Flow<AccountSummaryRaw?>
    
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
    
    // ========== BALANCE HISTORY ==========
    
    @Query("""
        SELECT :accountId as accountId,
               a.balanceCents, 
               strftime('%s', 'now') * 1000 as timestamp,
               NULL as transactionId,
               'INITIAL' as changeType
        FROM accounts a
        WHERE a.id = :accountId
    """)
    suspend fun getCurrentBalanceHistory(accountId: Long): BalanceHistory?
    
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
data class AccountSummaryRaw(
    val bankTotal: Long,
    val cashTotal: Long,
    val totalBalance: Long,
    val accountCount: Int,
    val activeCount: Int
) {
    fun toAccountSummary(): AccountSummary {
        return AccountSummary(
            totalBalanceCents = totalBalance,
            bankBalanceCents = bankTotal,
            cashBalanceCents = cashTotal,
            accountCount = accountCount,
            activeAccountCount = activeCount
        )
    }
}

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