package com.example.automaticfinances.system

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.automaticfinances.data.preferences.InsightsPreferences
import com.example.automaticfinances.data.repo.InsightsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

/**
 * Periodic insights digest (PROD-9). Once a week it asks [InsightsRepository] for a fresh report and
 * lets [InsightNotifier] surface it — the run-rate summary plus any recurring charges (PROD-5) and
 * anomalies (PROD-6) worth a glance.
 *
 * Dependencies are pulled via a Hilt [EntryPoint] rather than @HiltWorker so we avoid adding the
 * androidx.hilt-work artifact + a custom WorkerFactory, matching the plain-CoroutineWorker style of
 * [ListenerRebindWorker].
 */
class DigestWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun insightsRepository(): InsightsRepository
        fun insightsPreferences(): InsightsPreferences
    }

    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(applicationContext, Deps::class.java)

        // Respect the user's toggle; the digest is opt-out, not silently forced.
        if (!deps.insightsPreferences().isDigestEnabled()) return Result.success()

        return try {
            val report = deps.insightsRepository().generateReport()
            // Nothing recorded yet this month — don't nag with an empty summary.
            if (report.digest.transactionCount > 0) {
                InsightNotifier.showReport(applicationContext, report)
            }
            Result.success()
        } catch (e: Exception) {
            // History didn't load (e.g. transient DB issue) — let WorkManager retry the heartbeat.
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_PERIODIC = "insights_digest_weekly"
        private const val UNIQUE_ONE_SHOT = "insights_digest_now"

        /** Schedules the weekly digest, preserving any existing schedule across app restarts. */
        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<DigestWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(1, TimeUnit.DAYS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        /** Fires a digest right now (used by the "ver resumen ahora" settings action). */
        fun runNow(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_ONE_SHOT,
                androidx.work.ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<DigestWorker>().build(),
            )
        }
    }
}
