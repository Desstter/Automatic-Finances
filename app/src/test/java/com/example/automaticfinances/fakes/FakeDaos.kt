package com.example.automaticfinances.fakes

import com.example.automaticfinances.data.db.Account
import com.example.automaticfinances.data.db.AccountDao
import com.example.automaticfinances.data.db.AccountSummaryRaw
import com.example.automaticfinances.data.db.AccountType
import com.example.automaticfinances.data.db.AccountWithStats
import com.example.automaticfinances.data.db.BalanceHistory
import com.example.automaticfinances.data.db.Category
import com.example.automaticfinances.data.db.CategoryAccuracy
import com.example.automaticfinances.data.db.CategoryDao
import com.example.automaticfinances.data.db.CategorySuggestion
import com.example.automaticfinances.data.db.CategoryWithCount
import com.example.automaticfinances.data.db.DefaultCategories
import com.example.automaticfinances.data.db.Transaction
import com.example.automaticfinances.data.db.TransactionDao
import com.example.automaticfinances.data.db.UserCategoryPreference
import com.example.automaticfinances.data.db.UserCategoryPreferenceDao
import com.example.automaticfinances.data.repo.TransactionWithCategory
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

    override suspend fun insertIgnore(tx: Transaction): Long {
        if (rows.containsKey(tx.id)) return -1L
        rows[tx.id] = tx
        return nextRowId++
    }

    override suspend fun update(tx: Transaction) {
        rows[tx.id] = tx
    }

    override suspend fun getById(id: String): Transaction? = rows[id]

    override suspend fun deleteById(transactionId: String): Int =
        if (rows.remove(transactionId) != null) 1 else 0

    override fun all(): Flow<List<Transaction>> = throw NotImplementedError()
    override fun getByCategoryId(categoryId: Long): Flow<List<Transaction>> = throw NotImplementedError()
    override fun getByDateRange(startDate: String, endDate: String): Flow<List<Transaction>> = throw NotImplementedError()
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
    override suspend fun getByAccountAndDateRangeSync(accountId: Long, startDate: String, endDate: String): List<Transaction> = throw NotImplementedError()
    override suspend fun getTransactionCountByAccountAndDateRange(accountId: Long, startDate: String, endDate: String): Int = throw NotImplementedError()
    override suspend fun getByAccountFromDate(accountId: Long, fromDate: String): List<Transaction> = throw NotImplementedError()
    override suspend fun getTransactionCountByAccountFromDate(accountId: Long, fromDate: String): Int = throw NotImplementedError()
    override fun getAllTransactionsFlow(): Flow<List<Transaction>> = throw NotImplementedError()
    override suspend fun getTransactionsByAccountId(accountId: Long): List<Transaction> = throw NotImplementedError()
    override fun getTransactionsByAccountIdFlow(accountId: Long): Flow<List<Transaction>> = throw NotImplementedError()
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

    override suspend fun getAllActiveAccounts(): List<Account> = store.values.filter { it.isActive }
    override suspend fun getAccountById(accountId: Long): Account? = store[accountId]
    override suspend fun getAccountByName(name: String): Account? = store.values.find { it.name == name }
    override suspend fun insertAccount(account: Account): Long = seed(account)
    override suspend fun updateAccount(account: Account) { store[account.id] = account }
    override suspend fun getAccountBalance(accountId: Long): Long? = store[accountId]?.balanceCents
    override suspend fun updateAccountBalance(accountId: Long, balanceCents: Long) {
        store[accountId]?.let { store[accountId] = it.copy(balanceCents = balanceCents) }
    }
    override suspend fun adjustAccountBalance(accountId: Long, amountCents: Long) {
        store[accountId]?.let { store[accountId] = it.copy(balanceCents = it.balanceCents + amountCents) }
    }
    override suspend fun getAccountCount(): Int = store.size

    override fun getAllActiveAccountsFlow(): Flow<List<Account>> = throw NotImplementedError()
    override fun getAccountByIdFlow(accountId: Long): Flow<Account?> = throw NotImplementedError()
    override suspend fun getAccountsByType(type: AccountType): List<Account> = throw NotImplementedError()
    override suspend fun deleteAccount(account: Account) = throw NotImplementedError()
    override suspend fun getAccountSummary(): AccountSummaryRaw? = throw NotImplementedError()
    override fun getAccountSummaryFlow(): Flow<AccountSummaryRaw?> = throw NotImplementedError()
    override suspend fun getAccountsWithMonthlyStats(): List<AccountWithStats> = throw NotImplementedError()
    override suspend fun getCurrentBalanceHistory(accountId: Long): BalanceHistory? = throw NotImplementedError()
    override suspend fun calculateBankBalanceFromTransactions(): Long = throw NotImplementedError()
    override suspend fun calculateCashBalanceFromTransactions(): Long = throw NotImplementedError()
    override suspend fun deactivateAccount(accountId: Long) = throw NotImplementedError()
    override suspend fun activateAccount(accountId: Long) = throw NotImplementedError()
    override suspend fun getInactiveAccounts(): List<Account> = throw NotImplementedError()
}
