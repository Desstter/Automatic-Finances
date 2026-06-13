package com.example.automaticfinances.fakes

import com.example.automaticfinances.data.db.Account
import com.example.automaticfinances.data.db.AccountDao
import com.example.automaticfinances.data.db.AccountType
import com.example.automaticfinances.data.db.AccountWithStats
import com.example.automaticfinances.data.db.Category
import com.example.automaticfinances.data.db.CategoryAccuracy
import com.example.automaticfinances.data.db.CategoryDao
import com.example.automaticfinances.data.db.CategoryRule
import com.example.automaticfinances.data.db.CategoryRuleDao
import com.example.automaticfinances.data.db.CategorySuggestion
import com.example.automaticfinances.data.db.CategoryWithCount
import com.example.automaticfinances.data.db.DefaultCategories
import com.example.automaticfinances.data.db.DefaultCategoryRules
import com.example.automaticfinances.data.db.AccountWithOpeningBalanceRaw
import com.example.automaticfinances.data.db.MerchantResolution
import com.example.automaticfinances.data.db.MerchantResolutionDao
import com.example.automaticfinances.data.db.OpeningBalance
import com.example.automaticfinances.data.db.OpeningBalanceDao
import com.example.automaticfinances.data.db.OpeningBalanceSummaryRaw
import com.example.automaticfinances.data.db.PendingTransaction
import com.example.automaticfinances.data.db.PendingTransactionDao
import com.example.automaticfinances.data.db.Transaction
import com.example.automaticfinances.data.db.TransactionDao
import com.example.automaticfinances.data.db.UserCategoryPreference
import com.example.automaticfinances.data.db.UserCategoryPreferenceDao
import com.example.automaticfinances.data.repo.TransactionWithCategory
import com.example.automaticfinances.domain.TransactionRunner
import kotlinx.coroutines.flow.Flow

/**
 * Lightweight in-memory test doubles. Only the methods exercised by the unit tests are
 * implemented; everything else throws so an accidental dependency is loud rather than silent.
 */

/** Seeds [DefaultCategories] with positive ids so categorization resolves to a real id. */
class FakeCategoryDao(
    seed: List<Category> = DefaultCategories.list
) : CategoryDao {
    private val store: List<Category> =
        seed.mapIndexed { index, c -> c.copy(id = (index + 1).toLong()) }

    override suspend fun getAllActiveSync(): List<Category> = store.filter { it.isActive }

    override suspend fun getActiveSyncByType(isIncome: Boolean): List<Category> =
        store.filter { it.isActive && it.isIncome == isIncome }

    override suspend fun getById(id: Long): Category? = store.find { it.id == id }

    override suspend fun countDefaultCategories(): Int = store.count { it.isDefault }

    override fun getAllActive(): Flow<List<Category>> = throw NotImplementedError()
    override fun getActiveByType(isIncome: Boolean): Flow<List<Category>> = throw NotImplementedError()
    override suspend fun insert(category: Category): Long = throw NotImplementedError()
    override suspend fun insertAll(categories: List<Category>) = throw NotImplementedError()
    override suspend fun update(category: Category) = throw NotImplementedError()
    override suspend fun delete(category: Category) = throw NotImplementedError()
    override suspend fun softDelete(id: Long) = throw NotImplementedError()
    override suspend fun countTransactionsInCategory(categoryId: Long): Int = throw NotImplementedError()
    override fun getCategoriesWithTransactionCount(): Flow<List<CategoryWithCount>> = throw NotImplementedError()
    override fun getCategoriesWithTransactionCountByType(isIncome: Boolean): Flow<List<CategoryWithCount>> = throw NotImplementedError()
    override suspend fun getCategoriesWithCountByTypeSync(isIncome: Boolean): List<CategoryWithCount> =
        store.filter { it.isActive && it.isIncome == isIncome }.map {
            CategoryWithCount(it.id, it.name, it.color, it.icon, it.isDefault, it.isActive, it.isIncome, 0)
        }
}

/** Seeds [DefaultCategoryRules] so the table-driven keyword categorization resolves like production. */
class FakeCategoryRuleDao(
    seed: List<CategoryRule> = DefaultCategoryRules.list,
) : CategoryRuleDao {
    private val store: MutableList<CategoryRule> =
        seed.mapIndexed { index, r -> r.copy(id = (index + 1).toLong()) }.toMutableList()
    private var nextId: Long = (store.size + 1).toLong()

    override suspend fun getByType(isIncome: Boolean): List<CategoryRule> = store.filter { it.isIncome == isIncome }
    override suspend fun count(): Int = store.size
    override suspend fun insert(rule: CategoryRule): Long {
        if (store.any { it.keyword == rule.keyword && it.isIncome == rule.isIncome }) return -1L
        val id = nextId++
        store.add(rule.copy(id = id))
        return id
    }
    override suspend fun insertAll(rules: List<CategoryRule>) { rules.forEach { insert(it) } }
    override suspend fun deleteById(id: Long) { store.removeAll { it.id == id } }

    override fun getAll(): Flow<List<CategoryRule>> = throw NotImplementedError()
    override suspend fun update(rule: CategoryRule) = throw NotImplementedError()
}

/** No learned preferences -> falls through to keyword rules. */
class FakeUserCategoryPreferenceDao : UserCategoryPreferenceDao {
    override suspend fun getPreferenceForMerchant(merchantKey: String): UserCategoryPreference? = null
    override suspend fun getSuggestionsForMerchant(merchantKey: String): List<CategorySuggestion> = emptyList()

    override fun getAllActive(): Flow<List<UserCategoryPreference>> = throw NotImplementedError()
    override suspend fun insert(preference: UserCategoryPreference): Long = throw NotImplementedError()
    override suspend fun update(preference: UserCategoryPreference) = throw NotImplementedError()
    override suspend fun softDelete(preferenceId: Long) = throw NotImplementedError()
    override suspend fun deleteAllForMerchant(merchantKey: String) = throw NotImplementedError()
    override suspend fun getCategoryAccuracyStats(): List<CategoryAccuracy> = throw NotImplementedError()
    override suspend fun getTotalPreferences(): Int = throw NotImplementedError()
    override suspend fun getTopMerchants(limit: Int): List<UserCategoryPreference> = throw NotImplementedError()
    override suspend fun reinforcePreference(merchantKey: String, categoryId: Long, timestamp: Long) = throw NotImplementedError()
    override suspend fun penalizeWrongPreferences(merchantKey: String, correctCategoryId: Long) = throw NotImplementedError()
}

/** In-memory transactions table with idempotent insertIgnore semantics (rowId or -1). */
class FakeTransactionDao : TransactionDao {
    val rows = LinkedHashMap<String, Transaction>()
    private var nextRowId = 1L

    /** When set, inserting a row with this id throws — used to simulate a mid-operation failure. */
    var failOnInsertId: String? = null

    override suspend fun insertIgnore(tx: Transaction): Long {
        if (tx.id == failOnInsertId) throw RuntimeException("Simulated insert failure for ${tx.id}")
        if (rows.containsKey(tx.id)) return -1L
        rows[tx.id] = tx
        return nextRowId++
    }

    override suspend fun update(tx: Transaction) {
        rows[tx.id] = tx
    }

    override suspend fun getById(id: String): Transaction? = rows[id]

    override suspend fun getByTransferGroupId(transferGroupId: String): List<Transaction> =
        rows.values.filter { it.transferGroupId == transferGroupId }

    override suspend fun deleteById(transactionId: String): Int =
        if (rows.remove(transactionId) != null) 1 else 0

    override fun all(): Flow<List<Transaction>> = throw NotImplementedError()
    override fun getByCategoryId(categoryId: Long): Flow<List<Transaction>> = throw NotImplementedError()
    override fun getByDateRange(startDate: String, endDate: String): Flow<List<Transaction>> = throw NotImplementedError()
    override suspend fun getByDateRangeSync(startDate: String, endDate: String): List<Transaction> =
        rows.values.filter { it.date in startDate..endDate }.sortedWith(compareBy({ it.date }, { it.time }))
    override fun getByCategoryAndDateRange(categoryId: Long, startDate: String, endDate: String): Flow<List<Transaction>> = throw NotImplementedError()
    override fun getTransactionsWithCategories(): Flow<List<TransactionWithCategory>> = throw NotImplementedError()
    override suspend fun getTotalByCategory(categoryId: Long): Long = throw NotImplementedError()
    override suspend fun getTotalByCategoryAndDateRange(categoryId: Long, startDate: String, endDate: String): Long = throw NotImplementedError()
    override suspend fun getMonthlyTotal(year: Int, month: Int): Long = throw NotImplementedError()
    override fun sumByType(from: Long, to: Long, type: String): Flow<Long> = throw NotImplementedError()
    override suspend fun getTransactionsWithCategoriesSync(): List<TransactionWithCategory> = throw NotImplementedError()
    override suspend fun updateCategory(transactionId: String, categoryId: Long) = throw NotImplementedError()
    override suspend fun getUncategorizedTotalForDateRange(startDate: String, endDate: String): Long? = throw NotImplementedError()
    override suspend fun getUncategorizedExpenseTotalForDateRange(startDate: String, endDate: String): Long? = throw NotImplementedError()
    override suspend fun getTransactionCountForDateRange(startDate: String, endDate: String): Int = throw NotImplementedError()
    override fun getIncomes(): Flow<List<Transaction>> = throw NotImplementedError()
    override fun getExpenses(): Flow<List<Transaction>> = throw NotImplementedError()
    override fun getIncomesWithCategories(): Flow<List<TransactionWithCategory>> = throw NotImplementedError()
    override fun getExpensesWithCategories(): Flow<List<TransactionWithCategory>> = throw NotImplementedError()
    override suspend fun getMonthlyIncomeTotal(year: Int, month: Int): Long = throw NotImplementedError()
    override suspend fun getMonthlyExpenseTotal(year: Int, month: Int): Long = throw NotImplementedError()
    override suspend fun getIncomeTotalForDateRange(startDate: String, endDate: String): Long = throw NotImplementedError()
    override suspend fun getExpenseTotalForDateRange(startDate: String, endDate: String): Long = throw NotImplementedError()
    override suspend fun getIncomeCountForDateRange(startDate: String, endDate: String): Int = throw NotImplementedError()
    override suspend fun getExpenseCountForDateRange(startDate: String, endDate: String): Int = throw NotImplementedError()
    override suspend fun getExpenseTotalByCategoryAndDateRange(categoryId: Long, startDate: String, endDate: String): Long = throw NotImplementedError()
    override suspend fun getIncomeTotalByCategoryAndDateRange(categoryId: Long, startDate: String, endDate: String): Long = throw NotImplementedError()
    override suspend fun getIncomeCountByCategoryAndDateRange(categoryId: Long, startDate: String, endDate: String): Int = throw NotImplementedError()
    override suspend fun getExpenseCountByCategoryAndDateRange(categoryId: Long, startDate: String, endDate: String): Int = throw NotImplementedError()
    override suspend fun getByAccountAndDateRangeSync(accountId: Long, startDate: String, endDate: String): List<Transaction> =
        rows.values.filter { it.accountId == accountId && it.date in startDate..endDate }.sortedBy { it.date }
    override suspend fun getTransactionCountByAccountAndDateRange(accountId: Long, startDate: String, endDate: String): Int =
        rows.values.count { it.accountId == accountId && it.date in startDate..endDate }
    override suspend fun getByAccountFromDate(accountId: Long, fromDate: String): List<Transaction> =
        rows.values.filter { it.accountId == accountId && it.date >= fromDate }.sortedBy { it.date }
    override suspend fun getTransactionCountByAccountFromDate(accountId: Long, fromDate: String): Int =
        rows.values.count { it.accountId == accountId && it.date >= fromDate }
    override suspend fun getNetAmountByAccount(accountId: Long): Long =
        rows.values.filter { it.accountId == accountId }.sumOf { if (it.isIncome) it.amountCents else -it.amountCents }
    override suspend fun getNetAmountByAccountAndDateRange(accountId: Long, startDate: String, endDate: String): Long =
        rows.values.filter { it.accountId == accountId && it.date in startDate..endDate }
            .sumOf { if (it.isIncome) it.amountCents else -it.amountCents }
    override fun getAllTransactionsFlow(): Flow<List<Transaction>> = throw NotImplementedError()
    override suspend fun getTransactionsByAccountId(accountId: Long): List<Transaction> = throw NotImplementedError()
    override fun getTransactionsByAccountIdFlow(accountId: Long): Flow<List<Transaction>> = throw NotImplementedError()
}

/** In-memory pending_transactions table with idempotent insertIgnore semantics (rowId or -1). */
class FakePendingTransactionDao : PendingTransactionDao {
    val rows = LinkedHashMap<String, PendingTransaction>()
    private var nextRowId = 1L

    override suspend fun insertIgnore(item: PendingTransaction): Long {
        if (rows.containsKey(item.id)) return -1L
        rows[item.id] = item
        return nextRowId++
    }

    override suspend fun getById(id: String): PendingTransaction? = rows[id]
    override suspend fun deleteById(id: String) { rows.remove(id) }
    override suspend fun clear() { rows.clear() }

    override fun getAllFlow(): Flow<List<PendingTransaction>> = throw NotImplementedError()
    override fun countFlow(): Flow<Int> = throw NotImplementedError()
}

/** In-memory accounts table supporting balance adjustments. */
class FakeAccountDao : AccountDao {
    private val store = LinkedHashMap<Long, Account>()
    private var nextId = 1L

    fun seed(account: Account): Long {
        val id = if (account.id == 0L) nextId++ else account.id
        store[id] = account.copy(id = id)
        return id
    }

    /** Snapshot/restore hooks so a [FakeTransactionRunner] can roll back balance changes. */
    fun snapshot(): Map<Long, Account> = LinkedHashMap(store)
    fun restore(snapshot: Map<Long, Account>) {
        store.clear()
        store.putAll(snapshot)
    }

    override suspend fun getAllActiveAccounts(): List<Account> = store.values.filter { it.isActive }
    override suspend fun getAccountById(accountId: Long): Account? = store[accountId]
    override suspend fun getAccountByName(name: String): Account? = store.values.find { it.name == name }
    override suspend fun insertAccount(account: Account): Long = seed(account)
    override suspend fun updateAccount(account: Account) { store[account.id] = account }
    override suspend fun getAccountBalance(accountId: Long): Long? = store[accountId]?.balanceCents
    override suspend fun updateAccountBalance(accountId: Long, balanceCents: Long) {
        store[accountId]?.let { store[accountId] = it.copy(balanceCents = balanceCents) }
    }
    override suspend fun getAccountCount(): Int = store.size

    override fun getAllActiveAccountsFlow(): Flow<List<Account>> = throw NotImplementedError()
    override fun getAccountByIdFlow(accountId: Long): Flow<Account?> = throw NotImplementedError()
    override suspend fun getAccountsByType(type: AccountType): List<Account> = throw NotImplementedError()
    override suspend fun deleteAccount(account: Account) = throw NotImplementedError()
    override suspend fun getAccountsWithMonthlyStats(): List<AccountWithStats> = throw NotImplementedError()
    override suspend fun calculateBankBalanceFromTransactions(): Long = throw NotImplementedError()
    override suspend fun calculateCashBalanceFromTransactions(): Long = throw NotImplementedError()
}

/**
 * In-memory opening-balance store keyed by account. Only the single active opening per account is
 * modeled — enough for the financial-invariant tests, which seed one opening per account and rely
 * on the derived-balance recompute reading it back via [getActiveByAccount].
 */
class FakeOpeningBalanceDao : OpeningBalanceDao {
    private val active = LinkedHashMap<Long, OpeningBalance>()
    private var nextId = 1L

    /** Seeds the active opening balance for an account (test setup helper). */
    fun seed(accountId: Long, balanceCents: Long, effectiveDate: String) {
        active[accountId] = OpeningBalance(
            id = nextId++, accountId = accountId, effectiveDate = effectiveDate,
            balanceCents = balanceCents, note = "test", isActive = true,
        )
    }

    override suspend fun getActiveByAccount(accountId: Long): OpeningBalance? = active[accountId]
    override suspend fun insert(openingBalance: OpeningBalance): Long {
        val id = if (openingBalance.id == 0L) nextId++ else openingBalance.id
        active[openingBalance.accountId] = openingBalance.copy(id = id)
        return id
    }
    override suspend fun deactivateByAccount(accountId: Long) { active.remove(accountId) }
    override suspend fun hasActiveOpeningBalance(accountId: Long): Boolean = active.containsKey(accountId)
    override suspend fun getActiveOpeningBalanceCount(): Int = active.size

    override suspend fun update(openingBalance: OpeningBalance) = throw NotImplementedError()
    override suspend fun delete(openingBalance: OpeningBalance) = throw NotImplementedError()
    override suspend fun deleteById(id: Long) = throw NotImplementedError()
    override suspend fun getById(id: Long): OpeningBalance? = throw NotImplementedError()
    override fun getAll(): Flow<List<OpeningBalance>> = throw NotImplementedError()
    override fun getAllActive(): Flow<List<OpeningBalance>> = throw NotImplementedError()
    override suspend fun getAllActiveSync(): List<OpeningBalance> = active.values.toList()
    override fun getByAccount(accountId: Long): Flow<List<OpeningBalance>> = throw NotImplementedError()
    override fun getActiveByAccountFlow(accountId: Long): Flow<OpeningBalance?> = throw NotImplementedError()
    override suspend fun getHistoryByAccount(accountId: Long): List<OpeningBalance> = throw NotImplementedError()
    override suspend fun getByEffectiveDate(date: String): List<OpeningBalance> = throw NotImplementedError()
    override suspend fun getActiveOnOrBefore(date: String): List<OpeningBalance> = throw NotImplementedError()
    override suspend fun getActiveByAccountOnOrBefore(accountId: Long, date: String): OpeningBalance? = throw NotImplementedError()
    override suspend fun getActiveByAccountType(accountType: AccountType): List<OpeningBalance> = throw NotImplementedError()
    override suspend fun getActiveBankOpeningBalance(): OpeningBalance? = throw NotImplementedError()
    override suspend fun getActiveCashOpeningBalance(): OpeningBalance? = throw NotImplementedError()
    override suspend fun deactivateAll() = throw NotImplementedError()
    override suspend fun getTotalActiveOpeningBalance(): Long = throw NotImplementedError()
    override suspend fun getTotalActiveOpeningBalanceByType(accountType: AccountType): Long = throw NotImplementedError()
    override suspend fun hasActiveOpeningBalanceForType(accountType: AccountType): Boolean = throw NotImplementedError()
    override suspend fun getAccountsWithOpeningBalances(): List<AccountWithOpeningBalanceRaw> = throw NotImplementedError()
    override suspend fun getAccountsWithActiveOpeningBalances(): List<AccountWithOpeningBalanceRaw> = throw NotImplementedError()
    override suspend fun getOpeningBalanceSummaryRaw(): OpeningBalanceSummaryRaw? = throw NotImplementedError()
}

/**
 * In-memory [TransactionRunner] with real rollback semantics over the fake DAOs: it snapshots
 * their state before running the block and restores it if the block throws. This lets unit tests
 * assert that a failure mid-operation leaves no partial writes (atomicity).
 */
class FakeTransactionRunner(
    private val txDao: FakeTransactionDao,
    private val accountDao: FakeAccountDao,
) : TransactionRunner {
    override suspend fun <R> runInTransaction(block: suspend () -> R): R {
        val txSnapshot = LinkedHashMap(txDao.rows)
        val accountSnapshot = accountDao.snapshot()
        return try {
            block()
        } catch (t: Throwable) {
            txDao.rows.clear()
            txDao.rows.putAll(txSnapshot)
            accountDao.restore(accountSnapshot)
            throw t
        }
    }
}

/**
 * No gateway mappings. The financial tests use plain descriptions (never gateway-prefixed names),
 * so `isGatewayMerchant` is false and `resolve()` is never reached; everything throws to keep an
 * accidental dependency loud.
 */
class FakeMerchantResolutionDao : MerchantResolutionDao {
    override suspend fun getByGatewayMerchant(gatewayMerchant: String): MerchantResolution? = null
    override suspend fun count(): Int = 0
    override suspend fun countPrePopulated(): Int = 0

    override suspend fun insert(resolution: MerchantResolution): Long = throw NotImplementedError()
    override suspend fun insertAll(resolutions: List<MerchantResolution>) = throw NotImplementedError()
    override suspend fun update(resolution: MerchantResolution) = throw NotImplementedError()
    override suspend fun delete(resolution: MerchantResolution) = throw NotImplementedError()
    override suspend fun deleteById(id: Long) = throw NotImplementedError()
    override suspend fun incrementUsage(gatewayMerchant: String, timestamp: Long) = throw NotImplementedError()
    override suspend fun findSimilar(pattern: String): List<MerchantResolution> = throw NotImplementedError()
    override suspend fun updateSuggestedCategory(gatewayMerchant: String, categoryId: Long) = throw NotImplementedError()
    override fun getAll(): Flow<List<MerchantResolution>> = throw NotImplementedError()
    override fun getPrePopulated(): Flow<List<MerchantResolution>> = throw NotImplementedError()
    override fun getUserCreated(): Flow<List<MerchantResolution>> = throw NotImplementedError()
    override fun getTopUsed(): Flow<List<MerchantResolution>> = throw NotImplementedError()
    override fun getByCategory(categoryId: Long): Flow<List<MerchantResolution>> = throw NotImplementedError()
}
