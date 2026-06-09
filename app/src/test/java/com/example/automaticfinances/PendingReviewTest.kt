package com.example.automaticfinances

import com.example.automaticfinances.data.db.Account
import com.example.automaticfinances.data.db.Category
import com.example.automaticfinances.data.db.Transaction
import com.example.automaticfinances.data.repo.AccountRepository
import com.example.automaticfinances.data.repo.CategoryRepository
import com.example.automaticfinances.data.repo.MerchantResolutionRepository
import com.example.automaticfinances.data.repo.OpeningBalanceRepository
import com.example.automaticfinances.data.repo.PendingTransactionRepository
import com.example.automaticfinances.data.repo.TransactionRepository
import com.example.automaticfinances.data.repo.UserCategoryPreferenceRepository
import com.example.automaticfinances.domain.AddTransactionUseCase
import com.example.automaticfinances.domain.CaptureFeedbackNotifier
import com.example.automaticfinances.domain.CaptureTransactionUseCase
import com.example.automaticfinances.domain.ConfirmPendingTransactionUseCase
import com.example.automaticfinances.fakes.FakeAccountDao
import com.example.automaticfinances.fakes.FakeCategoryDao
import com.example.automaticfinances.fakes.FakeCategoryRuleDao
import com.example.automaticfinances.fakes.FakeMerchantResolutionDao
import com.example.automaticfinances.fakes.FakeOpeningBalanceDao
import com.example.automaticfinances.fakes.FakePendingTransactionDao
import com.example.automaticfinances.fakes.FakeTransactionDao
import com.example.automaticfinances.fakes.FakeTransactionRunner
import com.example.automaticfinances.fakes.FakeUserCategoryPreferenceDao
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * PROD-1 trust invariant: a low-confidence auto-capture must NOT move any balance until the user
 * confirms it. These tests exercise the real [CaptureTransactionUseCase] /
 * [ConfirmPendingTransactionUseCase] over the same fakes as [FinancialInvariantTest].
 */
class PendingReviewTest {

    private val bankOpening = 100_000L
    private val cashOpening = 200_000L
    private val openingDate = "2020-01-01"
    private var bankId = 0L
    private var cashId = 0L

    private lateinit var accountDao: FakeAccountDao
    private lateinit var txDao: FakeTransactionDao
    private lateinit var pendingDao: FakePendingTransactionDao
    private lateinit var accountRepo: AccountRepository
    private lateinit var txRepo: TransactionRepository
    private lateinit var pendingRepo: PendingTransactionRepository
    private lateinit var captureUseCase: CaptureTransactionUseCase
    private lateinit var confirmUseCase: ConfirmPendingTransactionUseCase

    @Before
    fun setup() {
        accountDao = FakeAccountDao()
        bankId = accountDao.seed(Account.createBankAccount("Banco", bankOpening))
        cashId = accountDao.seed(Account.createCashAccount("Efectivo", cashOpening))
        txDao = FakeTransactionDao()
        pendingDao = FakePendingTransactionDao()
        val openingDao = FakeOpeningBalanceDao()
        openingDao.seed(bankId, bankOpening, openingDate)
        openingDao.seed(cashId, cashOpening, openingDate)

        accountRepo = AccountRepository(accountDao, txDao)
        val openingRepo = OpeningBalanceRepository(openingDao, accountDao, txDao)
        txRepo = TransactionRepository(txDao)
        pendingRepo = PendingTransactionRepository(pendingDao)
        val categoryRepo = CategoryRepository(
            FakeCategoryDao(),
            FakeCategoryRuleDao(),
            UserCategoryPreferenceRepository(FakeUserCategoryPreferenceDao())
        )
        val merchantRepo = MerchantResolutionRepository(FakeMerchantResolutionDao(), FakeCategoryDao())
        val runner = FakeTransactionRunner(txDao, accountDao)
        val addUseCase = AddTransactionUseCase(txRepo, accountRepo, categoryRepo, merchantRepo, openingRepo, runner)
        val noopNotifier = object : CaptureFeedbackNotifier {
            override fun notifyCaptured(tx: Transaction, chips: List<Category>) {}
        }
        captureUseCase = CaptureTransactionUseCase(addUseCase, pendingRepo, txRepo, categoryRepo, noopNotifier)
        confirmUseCase = ConfirmPendingTransactionUseCase(addUseCase, pendingRepo)
    }

    private fun bankBalance(): Long = runBlocking { accountRepo.getAccountBalance(bankId)!! }
    private fun cashBalance(): Long = runBlocking { accountRepo.getAccountBalance(cashId)!! }

    private fun generic(id: String, amount: Long) = Transaction.fromTimestamp(
        id = id, ts = 1_700_000_000_000L, type = "COMPRA", description = "Comercio dudoso",
        amountCents = amount, currency = "COP", srcLast4 = "1234", dstLast4 = null,
        source = CaptureTransactionUseCase.SOURCE_GENERIC, rawPreview = "test", isIncome = false
    )

    private fun bankSms(id: String, amount: Long) = Transaction.fromTimestamp(
        id = id, ts = 1_700_000_000_000L, type = "COMPRA", description = "Compra clara",
        amountCents = amount, currency = "COP", srcLast4 = "6045", dstLast4 = null,
        source = "notif:sms", rawPreview = "test", isIncome = false
    )

    @Test
    fun genericCapture_doesNotTouchAnyBalance_andSitsInQueue() = runBlocking {
        captureUseCase(generic("g1", 10_000L))

        assertEquals("Bank untouched by an unreviewed capture", bankOpening, bankBalance())
        assertEquals("Cash untouched by an unreviewed capture", cashOpening, cashBalance())
        assertNull("Draft must NOT be in the transactions table", txRepo.getById("g1"))
        assertNotNull("Draft must sit in the review queue", pendingRepo.getById("g1"))
    }

    @Test
    fun highConfidenceCapture_appliesToBalanceImmediately_andSkipsQueue() = runBlocking {
        captureUseCase(bankSms("s1", 5_000L))

        assertEquals("High-confidence capture hits the balance now", bankOpening - 5_000L, bankBalance())
        assertNotNull("Confirmed row lives in transactions", txRepo.getById("s1"))
        assertNull("Nothing should be queued for a high-confidence capture", pendingRepo.getById("s1"))
    }

    @Test
    fun confirm_appliesToBalance_andRemovesFromQueue() = runBlocking {
        captureUseCase(generic("g2", 12_000L))
        val pending = pendingRepo.getById("g2")!!

        confirmUseCase(pending, categoryId = null)

        assertEquals("Confirm moves the saldo (generic -> cash)", cashOpening - 12_000L, cashBalance())
        assertNotNull("Confirmed draft becomes a real transaction", txRepo.getById("g2"))
        assertNull("Draft must leave the queue once confirmed", pendingRepo.getById("g2"))
    }

    @Test
    fun discard_removesFromQueue_withoutTouchingBalance() = runBlocking {
        captureUseCase(generic("g3", 9_000L))

        pendingRepo.delete("g3")

        assertNull("Discarded draft is gone", pendingRepo.getById("g3"))
        assertNull("Discard never creates a transaction", txRepo.getById("g3"))
        assertEquals("Cash untouched by a discard", cashOpening, cashBalance())
        assertEquals("Bank untouched by a discard", bankOpening, bankBalance())
    }

    @Test
    fun redeliveredGenericCapture_doesNotDuplicateInQueue() = runBlocking {
        val tx = generic("dup", 7_000L)
        captureUseCase(tx)
        captureUseCase(tx) // same stable id, re-delivered

        assertFalse("Second identical capture is ignored", pendingRepo.record(tx))
        assertEquals(cashOpening, cashBalance())
    }

    @Test
    fun redeliveryAfterConfirm_doesNotRequeue() = runBlocking {
        captureUseCase(generic("g4", 4_000L))
        confirmUseCase(pendingRepo.getById("g4")!!, categoryId = null)

        // The bank/messaging app re-posts the same SMS after the user already confirmed it.
        captureUseCase(generic("g4", 4_000L))

        assertNull("Already-confirmed message must not re-enter the queue", pendingRepo.getById("g4"))
        assertEquals("And must not double-count the balance", cashOpening - 4_000L, cashBalance())
    }
}
