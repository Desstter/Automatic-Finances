package com.example.automaticfinances.data.repo

import com.example.automaticfinances.data.db.CategoryDao
import com.example.automaticfinances.data.db.Transaction
import com.example.automaticfinances.data.db.TransactionDao
import com.example.automaticfinances.data.models.Anomaly
import com.example.automaticfinances.data.models.AnomalyKind
import com.example.automaticfinances.data.models.InsightsReport
import com.example.automaticfinances.data.models.MonthlyDigest
import com.example.automaticfinances.data.models.Subscription
import com.example.automaticfinances.utils.centsToCopString
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The insights layer: turns raw transaction history into things worth telling the user (PROD-5/6/9).
 *
 * It is intentionally decoupled from the rest of the app — it reads a few months of history through
 * the DAO once and derives everything in memory, so it stays cheap to run from a background worker
 * and easy to unit-test with fake DAOs. No DB writes, no balance side effects.
 *
 * Sign convention follows the rest of the codebase: `amountCents` is a positive magnitude and the
 * direction comes from [Transaction.isIncome]. We defensively `abs()` anyway.
 */
class InsightsRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
) {
    private val zone: ZoneId = ZoneId.of("America/Bogota")

    suspend fun generateReport(today: LocalDate = LocalDate.now(zone)): InsightsReport {
        // Three full months of history is enough for a monthly run-rate, recurring-charge detection,
        // and a category baseline for anomalies.
        val rangeStart = today.minusMonths(3).withDayOfMonth(1)
        // Internal transfers are not income or expense; drop them so digests, subscription
        // detection and anomalies never treat a money move between own accounts as spending.
        val all = transactionDao.getByDateRangeSync(rangeStart.toString(), today.toString())
            .filter { !it.isTransfer }
        val categoryNames = categoryDao.getAllActiveSync().associate { it.id to it.name }

        return InsightsReport(
            digest = buildDigest(all, today, categoryNames),
            subscriptions = detectSubscriptions(all, today, categoryNames),
            anomalies = detectAnomalies(all, today, categoryNames),
        )
    }

    // ---- PROD-9: monthly digest + run-rate projection ----

    private fun buildDigest(
        all: List<Transaction>,
        today: LocalDate,
        categoryNames: Map<Long, String>,
    ): MonthlyDigest {
        val curPrefix = monthPrefix(YearMonth.from(today))
        val prevPrefix = monthPrefix(YearMonth.from(today).minusMonths(1))

        val expenses = all.filter { !it.isIncome }
        val mtdExpenses = expenses.filter { it.date.startsWith(curPrefix) }
        val spentMtd = mtdExpenses.sumOf { abs(it.amountCents) }
        val incomeMtd = all.filter { it.isIncome && it.date.startsWith(curPrefix) }.sumOf { abs(it.amountCents) }
        val lastMonthTotal = expenses.filter { it.date.startsWith(prevPrefix) }.sumOf { abs(it.amountCents) }

        val daysElapsed = today.dayOfMonth
        val lengthOfMonth = today.lengthOfMonth()
        val dailyAverage = if (daysElapsed > 0) spentMtd / daysElapsed else 0L
        val projected = spentMtd + dailyAverage * (lengthOfMonth - daysElapsed)

        val projectedVsLastMonthPct = if (lastMonthTotal > 0) {
            (((projected - lastMonthTotal).toDouble() / lastMonthTotal) * 100).roundToInt()
        } else 0

        val topCategory = mtdExpenses
            .groupBy { it.categoryId }
            .mapValues { (_, txs) -> txs.sumOf { abs(it.amountCents) } }
            .maxByOrNull { it.value }

        return MonthlyDigest(
            monthLabel = YearMonth.from(today).month
                .getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es"))
                .lowercase(Locale.forLanguageTag("es")),
            spentMtdCents = spentMtd,
            incomeMtdCents = incomeMtd,
            netBalanceCents = incomeMtd - spentMtd,
            projectedMonthEndCents = projected,
            lastMonthTotalCents = lastMonthTotal,
            projectedVsLastMonthPct = projectedVsLastMonthPct,
            topCategoryName = topCategory?.key?.let { categoryNames[it] },
            topCategoryCents = topCategory?.value ?: 0L,
            expenseCount = mtdExpenses.size,
        )
    }

    // ---- PROD-5: recurring-charge (subscription) detection ----

    private fun detectSubscriptions(
        all: List<Transaction>,
        today: LocalDate,
        categoryNames: Map<Long, String>,
    ): List<Subscription> {
        val expenses = all.filter { !it.isIncome && abs(it.amountCents) >= MIN_SUBSCRIPTION_CENTS }

        return expenses
            .groupBy { normalizeMerchant(it.description) }
            .filter { it.key.isNotBlank() }
            .mapNotNull { (_, charges) ->
                val distinctMonths = charges.map { it.date.take(7) }.distinct()
                if (distinctMonths.size < MIN_RECURRING_MONTHS) return@mapNotNull null

                // Stable amount: at least MIN_RECURRING_MONTHS charges within ±25% of the median.
                val median = medianCents(charges.map { abs(it.amountCents) })
                val consistent = charges.count { withinTolerance(abs(it.amountCents), median) }
                if (consistent < MIN_RECURRING_MONTHS) return@mapNotNull null

                val latest = charges.maxByOrNull { it.date + it.time }!!
                Subscription(
                    merchantName = displayName(latest.description),
                    monthlyAmountCents = median,
                    occurrenceMonths = distinctMonths.size,
                    lastChargeDate = latest.date,
                    categoryName = latest.categoryId?.let { categoryNames[it] },
                )
            }
            .sortedByDescending { it.monthlyAmountCents }
    }

    // ---- PROD-6: anomaly detection (duplicate charge / unusual spend) ----

    private fun detectAnomalies(
        all: List<Transaction>,
        today: LocalDate,
        categoryNames: Map<Long, String>,
    ): List<Anomaly> {
        val windowStart = today.minusDays(ANOMALY_WINDOW_DAYS.toLong()).toString()
        val expenses = all.filter { !it.isIncome }
        val recent = expenses.filter { it.date >= windowStart }
        if (recent.isEmpty()) return emptyList()

        val anomalies = mutableListOf<Anomaly>()
        val flaggedIds = mutableSetOf<String>()

        // Duplicates: same merchant + same amount, two+ times within DUPLICATE_WINDOW_HOURS.
        recent
            .groupBy { normalizeMerchant(it.description) to abs(it.amountCents) }
            .forEach { (_, group) ->
                if (group.size < 2) return@forEach
                val sorted = group.sortedBy { it.ts }
                for (i in 1 until sorted.size) {
                    val prev = sorted[i - 1]
                    val cur = sorted[i]
                    val gapHours = abs(cur.ts - prev.ts) / 3_600_000.0
                    if (gapHours <= DUPLICATE_WINDOW_HOURS && cur.id !in flaggedIds) {
                        flaggedIds += cur.id
                        anomalies += Anomaly(
                            kind = AnomalyKind.DUPLICATE,
                            merchantName = displayName(cur.description),
                            amountCents = abs(cur.amountCents),
                            date = cur.date,
                            categoryName = cur.categoryId?.let { categoryNames[it] },
                            message = "Posible cargo duplicado en ${displayName(cur.description)} " +
                                "por ${abs(cur.amountCents).centsToCopString()}.",
                        )
                    }
                }
            }

        // Unusual amount: a recent charge far above its category's recent typical charge.
        val baseline = expenses.filter { it.date < windowStart }
            .groupBy { it.categoryId }
        recent.forEach { tx ->
            if (tx.id in flaggedIds) return@forEach
            val history = baseline[tx.categoryId] ?: return@forEach
            if (history.size < MIN_BASELINE_SAMPLES) return@forEach
            val mean = history.sumOf { abs(it.amountCents) }.toDouble() / history.size
            val amount = abs(tx.amountCents)
            if (mean > 0 && amount >= UNUSUAL_AMOUNT_FLOOR_CENTS && amount > mean * UNUSUAL_AMOUNT_FACTOR) {
                flaggedIds += tx.id
                val catName = tx.categoryId?.let { categoryNames[it] } ?: "esa categoría"
                anomalies += Anomaly(
                    kind = AnomalyKind.UNUSUAL_AMOUNT,
                    merchantName = displayName(tx.description),
                    amountCents = amount,
                    date = tx.date,
                    categoryName = tx.categoryId?.let { categoryNames[it] },
                    message = "Gasto inusual en $catName: ${amount.centsToCopString()} " +
                        "(≈${(amount / mean).roundToInt()}× tu gasto habitual).",
                )
            }
        }

        return anomalies.sortedByDescending { it.date }
    }

    // ---- helpers ----

    private fun monthPrefix(ym: YearMonth): String = "%04d-%02d".format(ym.year, ym.monthValue)

    private fun withinTolerance(amount: Long, median: Long): Boolean {
        if (median <= 0) return false
        return abs(amount - median).toDouble() / median <= AMOUNT_TOLERANCE
    }

    private fun medianCents(values: List<Long>): Long {
        if (values.isEmpty()) return 0L
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2
    }

    /**
     * Collapses a description to a comparison key: lowercased, digits/card-last4 and punctuation
     * stripped, whitespace squashed. "NETFLIX *1234" and "Netflix" both map to "netflix".
     */
    private fun normalizeMerchant(description: String): String =
        description.lowercase(Locale.forLanguageTag("es"))
            .replace(Regex("[*#]?\\d+"), " ")
            .replace(Regex("[^a-záéíóúñü ]"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")

    /** A clean, human-facing label for a merchant (trimmed, length-capped). */
    private fun displayName(description: String): String =
        description.replace(Regex("\\s+"), " ").trim().take(40).ifEmpty { "Comercio" }

    companion object {
        // A recurring charge must appear in at least this many distinct calendar months.
        private const val MIN_RECURRING_MONTHS = 2
        // Ignore trivial charges as subscriptions (< 5.000 COP).
        private const val MIN_SUBSCRIPTION_CENTS = 500_000L
        // Allowed spread around the median for a charge to count as "the same" recurring amount.
        private const val AMOUNT_TOLERANCE = 0.25

        private const val ANOMALY_WINDOW_DAYS = 7
        private const val DUPLICATE_WINDOW_HOURS = 48.0
        // Only flag "unusual" spend once a category has enough history to define "usual".
        private const val MIN_BASELINE_SAMPLES = 5
        private const val UNUSUAL_AMOUNT_FACTOR = 3.0
        // Don't nag about small absolute amounts even if they're a multiple of the average (< 30.000 COP).
        private const val UNUSUAL_AMOUNT_FLOOR_CENTS = 3_000_000L
    }
}
