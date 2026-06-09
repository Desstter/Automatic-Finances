package com.example.automaticfinances

import com.example.automaticfinances.data.db.Transaction
import com.example.automaticfinances.data.models.AnomalyKind
import com.example.automaticfinances.data.repo.InsightsRepository
import com.example.automaticfinances.fakes.FakeCategoryDao
import com.example.automaticfinances.fakes.FakeTransactionDao
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Unit coverage for the insights engine (PROD-5/6/9): run-rate projection, recurring-charge
 * detection and anomaly flags, all derived purely from in-memory history.
 */
class InsightsRepositoryTest {

    private lateinit var txDao: FakeTransactionDao
    private lateinit var repo: InsightsRepository

    private val today: LocalDate = LocalDate.of(2026, 6, 15)

    @Before
    fun setup() {
        txDao = FakeTransactionDao()
        repo = InsightsRepository(txDao, FakeCategoryDao())
    }

    private fun expense(
        id: String,
        date: String,
        amountCents: Long,
        categoryId: Long?,
        description: String,
        ts: Long = 0L,
    ) = Transaction(
        id = id, ts = ts, date = date, time = "12:00", type = "COMPRA",
        description = description, amountCents = amountCents, currency = "COP",
        srcLast4 = null, dstLast4 = null, source = "test", categoryId = categoryId,
        accountId = null, notes = "", isIncome = false, rawPreview = "",
    )

    private suspend fun seed(vararg txs: Transaction) {
        txs.forEach { txDao.insertIgnore(it) }
    }

    @Test
    fun `digest computes run-rate projection and top category`() = runBlocking {
        seed(
            expense("e1", "2026-06-05", 10_000_000, 1, "Almuerzo"),
            expense("e2", "2026-06-10", 5_000_000, 2, "Bus"),
            expense("n1", "2026-05-08", 3_000_000, 1, "Netflix *1234"),
            expense("n2", "2026-06-08", 3_000_000, 1, "Netflix"),
            expense("m1", "2026-05-20", 20_000_000, 2, "Mercado"),
        )

        val digest = repo.generateReport(today).digest

        // June MTD expenses: e1 + e2 + n2 = 18.000.000
        assertEquals(18_000_000, digest.spentMtdCents)
        assertEquals(3, digest.expenseCount)
        // 18.000.000 / 15 days * 30 days = 36.000.000
        assertEquals(36_000_000, digest.projectedMonthEndCents)
        // May expenses (n1 + m1) = 23.000.000  ->  (36-23)/23 ≈ +57%
        assertEquals(23_000_000, digest.lastMonthTotalCents)
        assertEquals(57, digest.projectedVsLastMonthPct)
        // Category 1 (e1 + n2 = 13.000.000) beats category 2 (5.000.000).
        assertEquals(13_000_000, digest.topCategoryCents)
        assertNotNull(digest.topCategoryName)
    }

    @Test
    fun `detects a monthly recurring charge as a subscription`() = runBlocking {
        seed(
            expense("e1", "2026-06-05", 10_000_000, 1, "Almuerzo"),
            expense("n1", "2026-05-08", 3_000_000, 1, "Netflix *1234"),
            expense("n2", "2026-06-08", 3_000_000, 1, "Netflix"),
        )

        val subs = repo.generateReport(today).subscriptions

        assertEquals(1, subs.size)
        assertEquals("Netflix", subs.first().merchantName)
        assertEquals(3_000_000, subs.first().monthlyAmountCents)
        assertEquals(2, subs.first().occurrenceMonths)
    }

    @Test
    fun `flags duplicate charge and unusual spend`() = runBlocking {
        // Duplicate: same merchant + amount, 2h apart, both inside the 7-day window.
        val base = 1_000_000_000L
        seed(
            expense("u1", "2026-06-14", 2_000_000, 4, "Uber", ts = base),
            expense("u2", "2026-06-14", 2_000_000, 4, "Uber", ts = base + 2 * 3_600_000),
        )
        // Unusual: category 3 has a stable ~10.000 baseline, then a 50.000 charge in the window.
        seed(
            *(1..6).map { i ->
                expense("b$i", "2026-05-1$i", 1_000_000, 3, "Tienda", ts = i.toLong())
            }.toTypedArray(),
            expense("big", "2026-06-12", 5_000_000, 3, "Tienda", ts = base),
        )

        val anomalies = repo.generateReport(today).anomalies

        assertEquals(2, anomalies.size)
        assertTrue(anomalies.any { it.kind == AnomalyKind.DUPLICATE && it.merchantName == "Uber" })
        assertTrue(anomalies.any { it.kind == AnomalyKind.UNUSUAL_AMOUNT && it.amountCents == 5_000_000L })
    }
}
