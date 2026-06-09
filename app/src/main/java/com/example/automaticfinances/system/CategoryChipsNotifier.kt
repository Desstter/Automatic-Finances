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
import com.example.automaticfinances.data.db.Category
import com.example.automaticfinances.data.db.Transaction
import com.example.automaticfinances.utils.centsToCopString
import kotlin.math.abs

/**
 * Posts the capture-feedback notification (PROD-2): right after a transaction is auto-captured from a
 * bank SMS/notification, this offers 2–3 category chips so the user can confirm or correct the
 * category with one tap — closing the learning loop without opening the app. Taps are handled by
 * [CategoryFeedbackReceiver].
 *
 * An ordinary low-importance notification (no foreground service, no sound — the bank already
 * notified the user), no-op when notifications are disabled. Idempotent per transaction: the
 * notification id is derived from the transaction id, so a re-delivered capture updates in place.
 */
object CategoryChipsNotifier {

    private const val TAG = "CategoryChipsNotifier"
    const val CHANNEL_ID = "capture_category"

    /** Action labels intentionally omit the category emoji icon (no emojis in app chrome). */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Categoría de la captura",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Sugiere y confirma la categoría de los movimientos detectados"
                setShowBadge(false)
                enableVibration(false)
            },
        )
    }

    fun notificationId(transactionId: String): Int = abs(("cap_$transactionId").hashCode())

    /**
     * Shows the chips for a just-captured [tx]. [chips] is the ordered candidate list (assigned
     * category first); the assigned one lets the user confirm, the rest let them correct. No-op if
     * there are no chips or notifications are disabled.
     */
    fun show(context: Context, tx: Transaction, chips: List<Category>) {
        if (chips.isEmpty()) return
        ensureChannel(context)
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) {
            Log.d(TAG, "Notifications disabled; skipping capture chips")
            return
        }

        val notifId = notificationId(tx.id)
        val assignedName = chips.firstOrNull { it.id == tx.categoryId }?.name
        val amount = tx.amountCents.centsToCopString()
        val header = if (tx.isIncome) "Ingreso registrado" else "Gasto registrado"
        val line = "$amount · ${tx.description}"
        val detail = buildString {
            append(line)
            if (assignedName != null) append("\nCategoría: $assignedName. Toca para confirmar o corregir.")
            else append("\nToca una categoría para clasificarlo.")
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(header)
            .setContentText(line)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)

        // Android shows up to 3 notification actions; the candidate list is already capped at 3.
        chips.forEach { category ->
            builder.addAction(
                NotificationCompat.Action.Builder(
                    android.R.drawable.ic_menu_edit,
                    category.name,
                    feedbackPendingIntent(context, tx, category, notifId),
                ).build(),
            )
        }

        notifySafely(manager, notifId, builder.build())
    }

    /** Replaces the chips with a brief confirmation once the user taps a category, then it auto-dismisses. */
    fun showConfirmation(context: Context, notificationId: Int, categoryName: String) {
        if (notificationId < 0) return
        ensureChannel(context)
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Categoría actualizada")
            .setContentText("Clasificado como $categoryName")
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setTimeoutAfter(4_000)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()

        notifySafely(manager, notificationId, notification)
    }

    private fun feedbackPendingIntent(
        context: Context,
        tx: Transaction,
        category: Category,
        notifId: Int,
    ): PendingIntent {
        val intent = Intent(context, CategoryFeedbackReceiver::class.java).apply {
            // Unique action string per (tx, category) so PendingIntent.getBroadcast doesn't collapse
            // the three chip intents into one (extras only differ otherwise).
            action = "${CategoryFeedbackReceiver.ACTION_APPLY}.${tx.id}.${category.id}"
            putExtra(CategoryFeedbackReceiver.EXTRA_TX_ID, tx.id)
            putExtra(CategoryFeedbackReceiver.EXTRA_CATEGORY_ID, category.id)
            putExtra(CategoryFeedbackReceiver.EXTRA_CATEGORY_NAME, category.name)
            putExtra(CategoryFeedbackReceiver.EXTRA_MERCHANT, tx.description)
            putExtra(CategoryFeedbackReceiver.EXTRA_NOTIF_ID, notifId)
        }
        return PendingIntent.getBroadcast(
            context,
            abs(("${tx.id}.${category.id}").hashCode()),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun notifySafely(manager: NotificationManagerCompat, id: Int, notification: android.app.Notification) {
        try {
            manager.notify(id, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Notify denied for capture chips", e)
        }
    }
}
