package com.example.automaticfinances.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OpeningBalanceDao {
    
    // ================ BASIC CRUD OPERATIONS ================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(openingBalance: OpeningBalance): Long
    
    @Update
    suspend fun update(openingBalance: OpeningBalance)
    
    @Delete
    suspend fun delete(openingBalance: OpeningBalance)
    
    @Query("DELETE FROM opening_balances WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    // ================ BASIC QUERIES ================
    
    @Query("SELECT * FROM opening_balances WHERE id = :id")
    suspend fun getById(id: Long): OpeningBalance?
    
    @Query("SELECT * FROM opening_balances ORDER BY createdAt DESC")
    fun getAll(): Flow<List<OpeningBalance>>
    
    @Query("SELECT * FROM opening_balances WHERE isActive = 1 ORDER BY effectiveDate DESC")
    fun getAllActive(): Flow<List<OpeningBalance>>
    
    @Query("SELECT * FROM opening_balances WHERE isActive = 1 ORDER BY effectiveDate DESC")
    suspend fun getAllActiveSync(): List<OpeningBalance>
    
    // ================ ACCOUNT-SPECIFIC QUERIES ================
    
    @Query("SELECT * FROM opening_balances WHERE accountId = :accountId ORDER BY effectiveDate DESC")
    fun getByAccount(accountId: Long): Flow<List<OpeningBalance>>
    
    @Query("SELECT * FROM opening_balances WHERE accountId = :accountId AND isActive = 1 ORDER BY effectiveDate DESC LIMIT 1")
    suspend fun getActiveByAccount(accountId: Long): OpeningBalance?
    
    @Query("SELECT * FROM opening_balances WHERE accountId = :accountId AND isActive = 1 ORDER BY effectiveDate DESC LIMIT 1")
    fun getActiveByAccountFlow(accountId: Long): Flow<OpeningBalance?>
    
    @Query("SELECT * FROM opening_balances WHERE accountId = :accountId ORDER BY effectiveDate DESC")
    suspend fun getHistoryByAccount(accountId: Long): List<OpeningBalance>
    
    // ================ DATE-BASED QUERIES ================
    
    @Query("SELECT * FROM opening_balances WHERE effectiveDate = :date AND isActive = 1")
    suspend fun getByEffectiveDate(date: String): List<OpeningBalance>
    
    @Query("SELECT * FROM opening_balances WHERE effectiveDate <= :date AND isActive = 1 ORDER BY effectiveDate DESC")
    suspend fun getActiveOnOrBefore(date: String): List<OpeningBalance>
    
    @Query("SELECT * FROM opening_balances WHERE accountId = :accountId AND effectiveDate <= :date AND isActive = 1 ORDER BY effectiveDate DESC LIMIT 1")
    suspend fun getActiveByAccountOnOrBefore(accountId: Long, date: String): OpeningBalance?
    
    // ================ ACCOUNT TYPE QUERIES ================
    
    @Query("""
        SELECT ob.* FROM opening_balances ob
        INNER JOIN accounts a ON ob.accountId = a.id
        WHERE a.type = :accountType AND ob.isActive = 1
        ORDER BY ob.effectiveDate DESC
    """)
    suspend fun getActiveByAccountType(accountType: AccountType): List<OpeningBalance>
    
    @Query("""
        SELECT ob.* FROM opening_balances ob
        INNER JOIN accounts a ON ob.accountId = a.id
        WHERE a.type = 'BANK' AND ob.isActive = 1
        ORDER BY ob.effectiveDate DESC
        LIMIT 1
    """)
    suspend fun getActiveBankOpeningBalance(): OpeningBalance?
    
    @Query("""
        SELECT ob.* FROM opening_balances ob
        INNER JOIN accounts a ON ob.accountId = a.id
        WHERE a.type = 'CASH' AND ob.isActive = 1
        ORDER BY ob.effectiveDate DESC
        LIMIT 1
    """)
    suspend fun getActiveCashOpeningBalance(): OpeningBalance?
    
    // ================ MANAGEMENT OPERATIONS ================
    
    @androidx.room.Transaction
    suspend fun setNewOpeningBalance(newOpeningBalance: OpeningBalance) {
        // Deactivate current active opening balance for this account
        deactivateByAccount(newOpeningBalance.accountId)
        // Insert new active opening balance
        insert(newOpeningBalance.copy(isActive = true))
    }
    
    @Query("UPDATE opening_balances SET isActive = 0 WHERE accountId = :accountId AND isActive = 1")
    suspend fun deactivateByAccount(accountId: Long)
    
    @Query("UPDATE opening_balances SET isActive = 0 WHERE isActive = 1")
    suspend fun deactivateAll()
    
    // ================ ANALYTICS QUERIES ================
    
    @Query("""
        SELECT 
            COALESCE(SUM(ob.balanceCents), 0) as totalBalance
        FROM opening_balances ob
        WHERE ob.isActive = 1
    """)
    suspend fun getTotalActiveOpeningBalance(): Long
    
    @Query("""
        SELECT 
            COALESCE(SUM(ob.balanceCents), 0) as totalBalance
        FROM opening_balances ob
        INNER JOIN accounts a ON ob.accountId = a.id
        WHERE ob.isActive = 1 AND a.type = :accountType
    """)
    suspend fun getTotalActiveOpeningBalanceByType(accountType: AccountType): Long
    
    @Query("SELECT COUNT(*) FROM opening_balances WHERE isActive = 1")
    suspend fun getActiveOpeningBalanceCount(): Int
    
    // ================ VALIDATION QUERIES ================
    
    @Query("SELECT COUNT(*) > 0 FROM opening_balances WHERE accountId = :accountId AND isActive = 1")
    suspend fun hasActiveOpeningBalance(accountId: Long): Boolean
    
    @Query("""
        SELECT COUNT(*) > 0 
        FROM opening_balances ob
        INNER JOIN accounts a ON ob.accountId = a.id
        WHERE a.type = :accountType AND ob.isActive = 1
    """)
    suspend fun hasActiveOpeningBalanceForType(accountType: AccountType): Boolean
    
    // ================ COMPLEX QUERIES WITH ACCOUNTS ================
    
    @Query("""
        SELECT 
            a.id as accountId,
            a.name as accountName,
            a.type as accountType,
            a.balanceCents as currentBalanceCents,
            ob.id as openingBalanceId,
            ob.balanceCents as openingBalanceCents,
            ob.effectiveDate as effectiveDate,
            ob.note as note,
            ob.createdAt as createdAt
        FROM accounts a
        LEFT JOIN opening_balances ob ON a.id = ob.accountId AND ob.isActive = 1
        WHERE a.isActive = 1
        ORDER BY a.type, a.name
    """)
    suspend fun getAccountsWithOpeningBalances(): List<AccountWithOpeningBalanceRaw>
    
    @Query("""
        SELECT 
            a.id as accountId,
            a.name as accountName,
            a.type as accountType,
            a.balanceCents as currentBalanceCents,
            ob.id as openingBalanceId,
            ob.balanceCents as openingBalanceCents,
            ob.effectiveDate as effectiveDate,
            ob.note as note,
            ob.createdAt as createdAt
        FROM accounts a
        INNER JOIN opening_balances ob ON a.id = ob.accountId AND ob.isActive = 1
        WHERE a.isActive = 1
        ORDER BY a.type, a.name
    """)
    suspend fun getAccountsWithActiveOpeningBalances(): List<AccountWithOpeningBalanceRaw>
    
    // ================ SUMMARY QUERIES ================
    
    @Query("""
        SELECT 
            COALESCE(SUM(CASE WHEN a.type = 'BANK' THEN ob.balanceCents ELSE 0 END), 0) as bankOpening,
            COALESCE(SUM(CASE WHEN a.type = 'CASH' THEN ob.balanceCents ELSE 0 END), 0) as cashOpening,
            COALESCE(SUM(CASE WHEN a.type = 'BANK' THEN a.balanceCents ELSE 0 END), 0) as bankCurrent,
            COALESCE(SUM(CASE WHEN a.type = 'CASH' THEN a.balanceCents ELSE 0 END), 0) as cashCurrent,
            COALESCE(SUM(ob.balanceCents), 0) as totalOpening,
            COALESCE(SUM(a.balanceCents), 0) as totalCurrent,
            COUNT(ob.id) as accountsWithOpening,
            MAX(ob.effectiveDate) as mostRecentEffectiveDate
        FROM accounts a
        INNER JOIN opening_balances ob ON a.id = ob.accountId AND ob.isActive = 1
        WHERE a.isActive = 1
    """)
    suspend fun getOpeningBalanceSummaryRaw(): OpeningBalanceSummaryRaw?
}

// Raw data classes for Room query results
data class AccountWithOpeningBalanceRaw(
    val accountId: Long,
    val accountName: String,
    val accountType: AccountType,
    val currentBalanceCents: Long,
    val openingBalanceId: Long?,
    val openingBalanceCents: Long?,
    val effectiveDate: String?,
    val note: String?,
    val createdAt: Long?
)

data class OpeningBalanceSummaryRaw(
    val bankOpening: Long,
    val cashOpening: Long,
    val bankCurrent: Long,
    val cashCurrent: Long,
    val totalOpening: Long,
    val totalCurrent: Long,
    val accountsWithOpening: Int,
    val mostRecentEffectiveDate: String?
)