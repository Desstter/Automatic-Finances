package com.example.automaticfinances.system

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.automaticfinances.MainActivity
import com.example.automaticfinances.data.models.InsightsReport
import com.example.automaticfinances.utils.centsToCopString
import kotlin.math.abs

/**
 * Posts the insights notifications — the channel through which "the app speaks" (PROD-9/5/6).
 *
 * Two distinct channels so the user can mute one without losing the other:
 *  - [CHANNEL_DIGEST] (default importance): the periodic summary + run-rate projection + recurring
 *    charges.
 *  - [CHANNEL_ALERTS] (high importance): duplicate / unusual-charge alerts that deserve attention now.
 *
 * Like [VoiceQuickActionNotifier] these are ordinary notifications (no foreground service) and no-op
 * cleanly when notifications are disabled.
 */
object InsightNotifier {

    private const val TAG = "InsightNotifier"
    const val CHANNEL_DIGEST = "insights_digest"
    const val CHANNEL_ALERTS = "insights_alerts"
    private const val DIGEST_NOTIFICATION_ID = 1002
    private const val ALERT_NOTIFICATION_ID = 1003
    private const val REQUEST_OPEN_APP = 2002

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_DIGEST) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_DIGEST,
                    "Resumen e insights",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { description = "Resumen periódico de tus finanzas y suscripciones" },
            )
        }
        if (manager.getNotificationChannel(CHANNEL_ALERTS) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ALERTS,
                    "Alertas de cargos",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply { description = "Avisos de cargos duplicados o gastos inusuales" },
            )
        }
    }

    /** Posts the digest summary, and (separately) a high-priority alert if anomalies were found. */
    fun showReport(context: Context, report: InsightsReport) {
        ensureChannels(context)
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) {
            Log.d(TAG, "Notifications disabled; skipping insights report")
            return
        }

        val pending = openAppIntent(context)
        postDigest(context, manager, report, pending)
        if (report.anomalies.isNotEmpty()) postAlerts(context, manager, report, pending)
    }

    private fun postDigest(
        context: Context,
        manager: NotificationManagerCompat,
        report: InsightsReport,
        pending: PendingIntent,
    ) {
        val d = report.digest
        val lines = buildList {
            add("Llevas ${d.spentMtdCents.centsToCopString()} gastados en ${d.monthLabel}.")
            add(runRateLine(report))
            d.topCategoryName?.let {
                if (d.topCategoryCents > 0) add("Tu mayor gasto: $it (${d.topCategoryCents.centsToCopString()}).")
            }
            if (report.subscriptions.isNotEmpty()) add(subscriptionsLine(report))
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_DIGEST)
            .setContentTitle("Tu resumen de ${d.monthLabel}")
            .setContentText(lines.first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(lines.joinToString("\n")))
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()

        notifySafely(manager, DIGEST_NOTIFICATION_ID, notification)
    }

    private fun postAlerts(
        context: Context,
        manager: NotificationManagerCompat,
        report: InsightsReport,
        pending: PendingIntent,
    ) {
        val messages = report.anomalies.take(3).map { it.message }
        val title = if (report.anomalies.size == 1) "Revisa este cargo" else "Revisa estos cargos"

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setContentTitle(title)
            .setContentText(messages.first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(messages.joinToString("\n")))
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()

        notifySafely(manager, ALERT_NOTIFICATION_ID, notification)
    }

    private fun runRateLine(report: InsightsReport): String {
        val d = report.digest
        val base = "A este ritmo cerrarás el mes en ${d.projectedMonthEndCents.centsToCopString()}"
        return when {
            d.lastMonthTotalCents <= 0 -> "$base."
            d.projectedVsLastMonthPct > 0 -> "$base, ${d.projectedVsLastMonthPct}% más que el mes pasado."
            d.projectedVsLastMonthPct < 0 -> "$base, ${abs(d.projectedVsLastMonthPct)}% menos que el mes pasado."
            else -> "$base, igual que el mes pasado."
        }
    }

    private fun subscriptionsLine(report: InsightsReport): String {
        val count = report.subscriptions.size
        val total = report.subscriptionsMonthlyTotalCents.centsToCopString()
        val noun = if (count == 1) "suscripción" else "suscripciones"
        return "$count $noun ≈ $total/mes."
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            REQUEST_OPEN_APP,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun notifySafely(manager: NotificationManagerCompat, id: Int, notification: android.app.Notification) {
        try {
            manager.notify(id, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Notify denied for insights", e)
        }
    }
}
