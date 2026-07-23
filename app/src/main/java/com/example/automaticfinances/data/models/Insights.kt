package com.example.automaticfinances.data.models

/**
 * Output of the insights layer ("que la app hable"). Everything here is derived purely from
 * transaction history by [com.example.automaticfinances.data.repo.InsightsRepository]; it carries no
 * DB coupling and is safe to surface in notifications or screens.
 */
data class InsightsReport(
    val digest: MonthlyDigest,
    val subscriptions: List<Subscription>,
    val anomalies: List<Anomaly>,
    /** Where the money actually went this month: top merchants by spend (MTD), highest first. */
    val topMerchants: List<MerchantSpend> = emptyList(),
) {
    /** Estimated monthly cost of all detected recurring charges, in cents. */
    val subscriptionsMonthlyTotalCents: Long get() = subscriptions.sumOf { it.monthlyAmountCents }
}

/** A single named bucket of spending (category or merchant), a positive magnitude in cents. */
data class CategorySpend(val name: String, val amountCents: Long)

/** A single merchant's month-to-date spend, a positive magnitude in cents. */
data class MerchantSpend(val name: String, val amountCents: Long)

/**
 * Month-to-date summary plus an end-of-month projection from the current daily run-rate (PROD-9).
 * Amounts are positive magnitudes in cents (COP).
 */
data class MonthlyDigest(
    /** "junio", lowercase Spanish month name of the period the digest covers. */
    val monthLabel: String,
    val spentMtdCents: Long,
    val incomeMtdCents: Long,
    /** income − expense so far this month (can be negative). */
    val netBalanceCents: Long,
    /** Run-rate projection: spent-so-far + daily-average × days-remaining. */
    val projectedMonthEndCents: Long,
    val lastMonthTotalCents: Long,
    /** Projected month-end vs. last month's actual total, as a signed percentage (rounded). */
    val projectedVsLastMonthPct: Int,
    val topCategoryName: String?,
    val topCategoryCents: Long,
    /** Spending broken down by category this month (MTD), highest first. */
    val topCategories: List<CategorySpend> = emptyList(),
    val expenseCount: Int,
) {
    val transactionCount: Int get() = expenseCount
}

/** A charge that repeats month to month at a roughly stable amount (PROD-5). */
data class Subscription(
    val merchantName: String,
    val monthlyAmountCents: Long,
    /** How many distinct calendar months this merchant was charged in the analysis window. */
    val occurrenceMonths: Int,
    /** "2026-06-01" of the most recent charge. */
    val lastChargeDate: String,
    val categoryName: String?,
)

enum class AnomalyKind {
    /** Same merchant + same amount charged twice in a short window — a likely double charge. */
    DUPLICATE,

    /** A single charge far larger than the category's recent typical amount. */
    UNUSUAL_AMOUNT,
}

/** A charge worth a second look (PROD-6). */
data class Anomaly(
    val kind: AnomalyKind,
    val merchantName: String,
    val amountCents: Long,
    /** "2026-06-01" of the flagged charge. */
    val date: String,
    val categoryName: String?,
    /** Short, user-facing explanation already formatted for display. */
    val message: String,
)
