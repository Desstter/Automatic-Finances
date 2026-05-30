package com.example.automaticfinances

import com.example.automaticfinances.data.db.Account
import com.example.automaticfinances.data.db.Transaction
import com.example.automaticfinances.data.repo.AccountRepository
import com.example.automaticfinances.data.repo.CategoryRepository
import com.example.automaticfinances.data.repo.TransactionRepository
import com.example.automaticfinances.data.repo.UserCategoryPreferenceRepository
import com.example.automaticfinances.domain.AddTransactionUseCase
import com.example.automaticfinances.domain.DeleteTransactionUseCase
import com.example.automaticfinances.domain.RestoreTransactionUseCase
import com.example.automaticfinances.fakes.FakeAccountDao
import com.example.automaticfinances.fakes.FakeCategoryDao
import com.example.automaticfinances.fakes.FakeTransactionDao
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
 * across add, delete (revert) and undo (re-apply), including idempotency guards.
 */
class FinancialInvariantTest {

    private val bankOpening = 100_000L
    private val cashOpening = 200_000L
    private var bankId = 0L
    private var cashId = 0L

    private lateinit var accountDao: FakeAccountDao
    private lateinit var txDao: FakeTransactionDao
    private lateinit var accountRepo: AccountRepository
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

        accountRepo = AccountRepository(accountDao, txDao)
        txRepo = TransactionRepository(txDao)
        val categoryRepo = CategoryRepository(
            FakeCategoryDao(),
            UserCategoryPreferenceRepository(FakeUserCategoryPreferenceDao())
        )
        addUseCase = AddTransactionUseCase(txRepo, accountRepo, categoryRepo)
        deleteUseCase = DeleteTransactionUseCase(txRepo, accountRepo)
        restoreUseCase = RestoreTransactionUseCase(txRepo, accountRepo)
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
}
