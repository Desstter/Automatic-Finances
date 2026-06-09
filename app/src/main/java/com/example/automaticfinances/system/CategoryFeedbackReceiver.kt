package com.example.automaticfinances.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.automaticfinances.domain.ApplyCategoryFeedbackUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles a tap on a category chip from the capture-feedback notification (PROD-2): retags the
 * transaction, teaches the learning layer, and swaps the notification for a brief confirmation. See
 * [CategoryChipsNotifier].
 *
 * Not exported and reached only via the explicit PendingIntents built by the notifier, so no other
 * app can spoof a category change.
 */
@AndroidEntryPoint
class CategoryFeedbackReceiver : BroadcastReceiver() {

    @Inject lateinit var applyFeedback: ApplyCategoryFeedbackUseCase

    override fun onReceive(context: Context, intent: Intent) {
        val txId = intent.getStringExtra(EXTRA_TX_ID) ?: return
        val categoryId = intent.getLongExtra(EXTRA_CATEGORY_ID, -1L)
        if (categoryId < 0L) return
        val categoryName = intent.getStringExtra(EXTRA_CATEGORY_NAME).orEmpty()
        val merchant = intent.getStringExtra(EXTRA_MERCHANT).orEmpty()
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, -1)
        val appContext = context.applicationContext

        // goAsync keeps the receiver alive (~10s) while we hop to IO for the DB write.
        val pending = goAsync()
        scope.launch {
            try {
                applyFeedback(txId, categoryId, merchant)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply category feedback", e)
            } finally {
                runCatching { CategoryChipsNotifier.showConfirmation(appContext, notifId, categoryName) }
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "CategoryFeedback"

        // Survives individual receiver instances (each onReceive gets a fresh BroadcastReceiver).
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        const val ACTION_APPLY = "com.example.automaticfinances.action.APPLY_CATEGORY_FEEDBACK"
        const val EXTRA_TX_ID = "tx_id"
        const val EXTRA_CATEGORY_ID = "category_id"
        const val EXTRA_CATEGORY_NAME = "category_name"
        const val EXTRA_MERCHANT = "merchant"
        const val EXTRA_NOTIF_ID = "notif_id"
    }
}
