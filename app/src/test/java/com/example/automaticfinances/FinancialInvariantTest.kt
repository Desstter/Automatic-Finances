package com.example.automaticfinances

import com.example.automaticfinances.data.db.Account
import com.example.automaticfinances.data.db.Transaction
import com.example.automaticfinances.data.repo.AccountRepository
import com.example.automaticfinances.data.repo.CategoryRepository
import com.example.automaticfinances.data.repo.MerchantResolutionRepository
import com.example.automaticfinances.data.repo.OpeningBalanceRepository
import com.example.automaticfinances.data.repo.TransactionRepository
import com.example.automaticfinances.data.repo.UserCategoryPreferenceRepository
import com.example.automaticfinances.domain.AddTransactionUseCase
import com.example.automaticfinances.domain.DeleteTransactionUseCase
import com.example.automaticfinances.domain.RestoreTransactionUseCase
import com.example.automaticfinances.fakes.FakeAccountDao
import com.example.automaticfinances.fakes.FakeCategoryDao
import com.example.automaticfinances.fakes.FakeCategoryRuleDao
import com.example.automaticfinances.fakes.FakeMerchantResolutionDao
import com.example.automaticfinances.fakes.FakeOpeningBalanceDao
import com.example.automaticfinances.fakes.FakeTransactionDao
import com.example.automaticfinances.fakes.FakeTransactionRunner
import com.example.automaticfinances.fakes.FakeUserCategoryPreferenceDao
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * End-to-end coverage of the financial integrity invariant:
 *   opening balance + applied movements == current balance
 * across add, delete and undo, including idempotency guards.
 *
 * Post-ARQ-1, `account.balanceCents` is a materialized cache of the derived value (opening
 * snapshot + movements), recomputed wholesale after each operation. These tests seed an opening
 * snapshot per account and assert the cache always equals the derived figure.
 */
class FinancialInvariantTest {

    private val bankOpening = 100_000L
    private val cashOpening = 200_000L
    // Effective date well before any test transaction, so all movements count toward the balance.
    private val openingDate = "2020-01-01"
    private var bankId = 0L
    private var cashId = 0L

    private lateinit var accountDao: FakeAccountDao
    private lateinit var txDao: FakeTransactionDao
    private lateinit var openingDao: FakeOpeningBalanceDao
    private lateinit var accountRepo: AccountRepository
    private lateinit var openingRepo: OpeningBalanceRepository
    private lateinit var txRepo: TransactionRepository
    private lateinit var addUseCase: AddTransactionUseCase
    private lateinit var deleteUseCase: DeleteTransactionUseCase
    private lateinit var restoreUseCase: RestoreTransactionUseCase

    @Before
    fun setup() {
        accountDao = FakeAccountDao()
        bankId = accountDao.seed(Account.createBankAccount("Banco", bankOpening))
        cashId = accountDao.seed(Account.createCashAccount("Efectivo", cashOpening))
        txDao = FakeTransactionDao()
        openingDao = FakeOpeningBalanceDao()
        // The opening snapshot is the source of truth; the seeded account.balanceCents above is its
        // initial cache (no movements yet, so cache == opening).
        openingDao.seed(bankId, bankOpening, openingDate)
        openingDao.seed(cashId, cashOpening, openingDate)

        accountRepo = AccountRepository(accountDao, txDao)
        openingRepo = OpeningBalanceRepository(openingDao, accountDao, txDao)
        txRepo = TransactionRepository(txDao)
        val categoryRepo = CategoryRepository(
            FakeCategoryDao(),
            FakeCategoryRuleDao(),
            UserCategoryPreferenceRepository(FakeUserCategoryPreferenceDao())
        )
        val merchantRepo = MerchantResolutionRepository(FakeMerchantResolutionDao(), FakeCategoryDao())
        val runner = FakeTransactionRunner(txDao, accountDao)
        addUseCase = AddTransactionUseCase(txRepo, accountRepo, categoryRepo, merchantRepo, openingRepo, runner)
        deleteUseCase = DeleteTransactionUseCase(txRepo, openingRepo, runner)
        restoreUseCase = RestoreTransactionUseCase(txRepo, openingRepo, runner)
    }

    private fun cashBalance(): Long = runBlocking { accountRepo.getAccountBalance(cashId)!! }
    private fun bankBalance(): Long = runBlocking { accountRepo.getAccountBalance(bankId)!! }

    private fun expense(id: String, amount: Long, source: String = "manual") =
        Transaction.fromTimestamp(
            id = id, ts = 1_700_000_000_000L, type = "COMPRA", description = "Compra prueba",
            amountCents = amount, currency = "COP", srcLast4 = null, dstLast4 = null,
            source = source, rawPreview = "test", isIncome = false
        )

    @Test
    fun expense_addDecrements_deleteReverts_undoReapplies() = runBlocking {
        val tx = expense("t1", 10_000L)

        addUseCase(tx)
        assertEquals("Expense should decrement cash", cashOpening - 10_000L, cashBalance())

        val stored = txRepo.getById("t1")!!
        assertTrue(deleteUseCase(stored))
        assertEquals("Delete must revert the balance", cashOpening, cashBalance())
        assertNull("Row must be gone after delete", txRepo.getById("t1"))

        assertTrue(restoreUseCase(stored))
        assertEquals("Undo must re-apply the balance", cashOpening - 10_000L, cashBalance())
    }

    @Test
    fun income_increasesBalance() = runBlocking {
        val tx = Transaction.fromTimestamp(
            id = "inc1", ts = 1_700_000_000_000L, type = "INGRESO", description = "Pago",
            amountCents = 7_000L, currency = "COP", srcLast4 = null, dstLast4 = null,
            source = "manual", rawPreview = "test", isIncome = true
        )

        addUseCase(tx)

        assertEquals(cashOpening + 7_000L, cashBalance())
    }

    @Test
    fun redeliveredNotification_doesNotDoubleCount() = runBlocking {
        val tx = expense("dup", 5_000L, source = "notif:sms") // -> bank account

        addUseCase(tx)
        addUseCase(tx) // same stable id, must be ignored

        assertEquals("Duplicate insert must not double count", bankOpening - 5_000L, bankBalance())
    }

    @Test
    fun doubleUndo_doesNotDoubleCount() = runBlocking {
        val tx = expense("t2", 8_000L)
        addUseCase(tx)
        val stored = txRepo.getById("t2")!!
        deleteUseCase(stored)

        assertTrue("First undo inserts", restoreUseCase(stored))
        assertEquals(cashOpening - 8_000L, cashBalance())

        assertTrue("Second undo is a no-op insert", !restoreUseCase(stored))
        assertEquals("Double undo must not double count", cashOpening - 8_000L, cashBalance())
    }

    @Test
    fun atmWithdrawal_splitsBankToCash_preservingTotal() = runBlocking {
        val totalBefore = bankBalance() + cashBalance()
        val retiro = Transaction.fromTimestamp(
            id = "r1", ts = 1_700_000_000_000L, type = "RETIRO", description = "Retiro cajero ATM",
            amountCents = 50_000L, currency = "COP", srcLast4 = "6045", dstLast4 = null,
            source = "notif:sms", rawPreview = "test", isIncome = false
        )

        addUseCase(retiro)

        assertEquals("Bank decreases by withdrawal", bankOpening - 50_000L, bankBalance())
        assertEquals("Cash increases by withdrawal", cashOpening + 50_000L, cashBalance())
        assertEquals("Total balance is conserved across the transfer", totalBefore, bankBalance() + cashBalance())
    }

    @Test
    fun atmWithdrawal_legsAreTransferFlaggedAndGrouped_soTheyAreNeitherIncomeNorExpense() = runBlocking {
        val retiro = Transaction.fromTimestamp(
            id = "r3", ts = 1_700_000_000_000L, type = "RETIRO", description = "Retiro cajero",
            amountCents = 50_000L, currency = "COP", srcLast4 = null, dstLast4 = null,
            source = "notif:retiro", rawPreview = "Sacaste $50000 Retiro en Cajero", isIncome = false
        )

        addUseCase(retiro)

        val legs = txDao.rows.values.filter { it.transferGroupId == "r3" }
        assertEquals("A withdrawal produces exactly two legs", 2, legs.size)
        // Both legs flagged isTransfer -> every income/expense/category/chart DAO query (all filter
        // isTransfer = 0) ignores them: a withdrawal is a relocation of money, not a real gasto/ingreso.
        assertTrue("Both legs must be transfer-flagged", legs.all { it.isTransfer })
        assertTrue("Both legs share the withdrawal's group id", legs.all { it.transferGroupId == "r3" })
        assertTrue("One outgoing (bank) leg drives the balance down", legs.any { !it.isIncome })
        assertTrue("One incoming (cash) leg drives the balance up", legs.any { it.isIncome })
        assertTrue("Transfer legs carry no category", legs.all { it.categoryId == null })
    }

    @Test
    fun atmWithdrawal_isAtomic_whenCashLegFailsTheBankLegRollsBack() = runBlocking {
        val bankBefore = bankBalance()
        val cashBefore = cashBalance()

        // Simulate a failure inserting the cash leg AFTER the bank leg has already been applied.
        txDao.failOnInsertId = "r2_CASH"
        val retiro = Transaction.fromTimestamp(
            id = "r2", ts = 1_700_000_000_000L, type = "RETIRO", description = "Retiro cajero ATM",
            amountCents = 50_000L, currency = "COP", srcLast4 = "6045", dstLast4 = null,
            source = "notif:sms", rawPreview = "test", isIncome = false
        )

        var threw = false
        try {
            addUseCase(retiro)
        } catch (e: Exception) {
            threw = true
        }

        assertTrue("The mid-operation failure must surface to the caller", threw)
        assertEquals("Bank leg must be rolled back, not left half-applied", bankBefore, bankBalance())
        assertEquals("Cash must be untouched", cashBefore, cashBalance())
        assertNull("No bank leg row may survive a rolled-back transaction", txRepo.getById("r2_BANK"))
        assertNull("No cash leg row may survive a rolled-back transaction", txRepo.getById("r2_CASH"))
    }

    @Test
    fun cachedBalance_alwaysEqualsDerivedSource_acrossOperations() = runBlocking {
        // The whole point of ARQ-1: the cached account.balanceCents can never drift from the
        // derived authority (opening snapshot + movements), no matter the sequence of operations.
        addUseCase(expense("a", 12_000L))                 // cash -12k
        addUseCase(expense("b", 3_000L, "notif:sms"))     // bank -3k
        val a = txRepo.getById("a")!!
        deleteUseCase(a)                                  // cash back +12k
        restoreUseCase(a)                                 // cash -12k again

        for (id in listOf(bankId, cashId)) {
            val cached = accountRepo.getAccountBalance(id)!!
            val derived = openingRepo.calculateCurrentBalanceWithOpening(id).currentBalanceCents
            assertEquals("Cache must equal the derived source for account $id", derived, cached)
        }
        assertEquals("Cash = opening - 12k", cashOpening - 12_000L, cashBalance())
        assertEquals("Bank = opening - 3k", bankOpening - 3_000L, bankBalance())
    }
}
