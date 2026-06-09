package com.example.automaticfinances

import com.example.automaticfinances.data.db.Account
import com.example.automaticfinances.data.repo.AccountRepository
import com.example.automaticfinances.data.repo.OpeningBalanceRepository
import com.example.automaticfinances.data.repo.TransactionRepository
import com.example.automaticfinances.domain.DeleteTransactionUseCase
import com.example.automaticfinances.domain.RestoreTransactionUseCase
import com.example.automaticfinances.domain.TransferUseCase
import com.example.automaticfinances.fakes.FakeAccountDao
import com.example.automaticfinances.fakes.FakeOpeningBalanceDao
import com.example.automaticfinances.fakes.FakeTransactionDao
import com.example.automaticfinances.fakes.FakeTransactionRunner
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Transfers move money between the user's own accounts. They must (a) decrement the origin and
 * increment the destination by the same amount, conserving the total, (b) be idempotent against a
 * double tap, and (c) delete/undo as a pair so neither account is ever left half-updated.
 */
class TransferUseCaseTest {

    private val bankOpening = 300_000L
    private val cashOpening = 50_000L
    private val openingDate = "2020-01-01"
    private var bankId = 0L
    private var cashId = 0L
    private val ts = 1_700_000_000_000L

    private lateinit var accountDao: FakeAccountDao
    private lateinit var txDao: FakeTransactionDao
    private lateinit var openingDao: FakeOpeningBalanceDao
    private lateinit var accountRepo: AccountRepository
    private lateinit var openingRepo: OpeningBalanceRepository
    private lateinit var txRepo: TransactionRepository
    private lateinit var transfer: TransferUseCase
    private lateinit var deleteUseCase: DeleteTransactionUseCase
    private lateinit var restoreUseCase: RestoreTransactionUseCase

    @Before
    fun setup() {
        accountDao = FakeAccountDao()
        bankId = accountDao.seed(Account.createBankAccount("Banco", bankOpening))
        cashId = accountDao.seed(Account.createCashAccount("Efectivo", cashOpening))
        txDao = FakeTransactionDao()
        openingDao = FakeOpeningBalanceDao()
        openingDao.seed(bankId, bankOpening, openingDate)
        openingDao.seed(cashId, cashOpening, openingDate)

        accountRepo = AccountRepository(accountDao, txDao)
        openingRepo = OpeningBalanceRepository(openingDao, accountDao, txDao)
        txRepo = TransactionRepository(txDao)
        val runner = FakeTransactionRunner(txDao, accountDao)
        transfer = TransferUseCase(txRepo, accountRepo, openingRepo, runner)
        deleteUseCase = DeleteTransactionUseCase(txRepo, openingRepo, runner)
        restoreUseCase = RestoreTransactionUseCase(txRepo, openingRepo, runner)
    }

    private fun bank() = runBlocking { accountRepo.getAccountBalance(bankId)!! }
    private fun cash() = runBlocking { accountRepo.getAccountBalance(cashId)!! }

    @Test
    fun transfer_movesMoney_conservingTotal() = runBlocking {
        val totalBefore = bank() + cash()

        val result = transfer(bankId, cashId, 100_000L, ts)
        assertTrue(result is TransferUseCase.Result.Success)

        assertEquals("Origin (bank) decreases", bankOpening - 100_000L, bank())
        assertEquals("Destination (cash) increases", cashOpening + 100_000L, cash())
        assertEquals("Total is conserved", totalBefore, bank() + cash())
    }

    @Test
    fun transfer_legsAreFlaggedAndGrouped() = runBlocking {
        transfer(bankId, cashId, 100_000L, ts)
        // Both legs carry the transfer flag (so the income/expense DAO queries — which all filter
        // `isTransfer = 0` — ignore them) and share a single transfer group id.
        val legs = txDao.rows.values.toList()
        assertTrue("Both legs flagged isTransfer", legs.all { it.isTransfer })
        assertEquals("Both legs share one group", 1, legs.mapNotNull { it.transferGroupId }.distinct().size)
        assertTrue("One outgoing (expense) leg", legs.any { !it.isIncome })
        assertTrue("One incoming (income) leg", legs.any { it.isIncome })
    }

    @Test
    fun transfer_isIdempotent_onDoubleTap() = runBlocking {
        transfer(bankId, cashId, 100_000L, ts)
        transfer(bankId, cashId, 100_000L, ts) // same group id -> ignored

        assertEquals(2, txDao.rows.size)
        assertEquals(bankOpening - 100_000L, bank())
        assertEquals(cashOpening + 100_000L, cash())
    }

    @Test
    fun transfer_rejectsSameAccount() = runBlocking {
        val result = transfer(bankId, bankId, 100_000L, ts)
        assertTrue(result is TransferUseCase.Result.Failure)
        assertEquals(bankOpening, bank())
    }

    @Test
    fun transfer_deleteAndUndo_handleBothLegsAtomically() = runBlocking {
        transfer(bankId, cashId, 100_000L, ts)
        val legs = txDao.rows.values.toList()
        assertEquals(2, legs.size)

        assertTrue(deleteUseCase.deleteGroup(legs))
        assertEquals("Both legs gone, balances reverted", bankOpening, bank())
        assertEquals(cashOpening, cash())
        legs.forEach { assertNull(txRepo.getById(it.id)) }

        assertTrue(restoreUseCase.restoreGroup(legs))
        assertEquals("Undo restores both legs", bankOpening - 100_000L, bank())
        assertEquals(cashOpening + 100_000L, cash())
    }
}
