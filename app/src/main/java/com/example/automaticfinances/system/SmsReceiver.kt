package com.example.automaticfinances.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.automaticfinances.data.parse.BancolombiaParser
import com.example.automaticfinances.data.repo.UnparsedSmsRepository
import com.example.automaticfinances.domain.CaptureTransactionUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Primary, OEM-proof capture path for SMS-based banks (Bancolombia).
 *
 * Unlike [SmsNotifListener], this does NOT depend on the notification listener being bound: the
 * `SMS_RECEIVED` broadcast is delivered by the OS even when the app is idle or was killed, so a bank
 * SMS is captured regardless of MIUI/HyperOS unbinding the listener. The notification listener is
 * kept as a secondary path for app-push banks (Nequi, DaviPlata, BBVA, Banco de Bogotá) that don't
 * send SMS.
 *
 * Dedup is by [BancolombiaParser.tryParse]'s stable id (folds in the message timestamp), so when both
 * this receiver and the listener see the same Bancolombia SMS, whichever fires first wins and the
 * other resolves to a harmless no-op insert — never a duplicate, never a double balance adjustment
 * (see the financial idempotency invariant in [AddTransactionUseCase]).
 */
@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject lateinit var captureTx: CaptureTransactionUseCase
    @Inject lateinit var unparsedRepo: UnparsedSmsRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = runCatching { Telephony.Sms.Intents.getMessagesFromIntent(intent) }
            .getOrNull()
            ?.takeIf { it.isNotEmpty() } ?: return

        // Reassemble a (possibly multipart) SMS: all parts share one originating address and
        // timestamp, so concatenating the bodies reconstructs the full bank message.
        val body = messages.joinToString(separator = "") {
            it.displayMessageBody ?: it.messageBody ?: ""
        }
        if (body.isBlank()) return

        // Cheap content gate before touching the regex engine: never run the parser (or log) on
        // ordinary personal SMS. Single source of truth shared with the unparsed log and the
        // notification listener, so the capture gate can never disagree with what we'd keep as a
        // miss — a bank SMS that looks like a transaction is either parsed or recorded, never lost.
        if (!UnparsedSmsRepository.looksLikeTransaction(body)) return

        // Use the message timestamp so the stable id matches the listener path for the same SMS.
        val ts = messages.first().timestampMillis

        // goAsync keeps the broadcast alive (~10s) while we hop to IO for the DB write.
        val pending = goAsync()
        scope.launch {
            try {
                val tx = BancolombiaParser.tryParse(body, ts)
                if (tx == null) {
                    // A bank SMS that carries an amount but didn't parse is exactly the signal we
                    // want to keep: it tells us which formats the parser is missing. Stored on-device
                    // only (see UnparsedSmsRepository), never reported anywhere.
                    if (UnparsedSmsRepository.looksLikeTransaction(body)) {
                        unparsedRepo.record(body, source = "sms", receivedAt = ts)
                    }
                    return@launch
                }
                // Routes high-confidence captures straight to the balance; low-confidence
                // (generic-fallback) ones go to the "Por revisar" queue instead.
                captureTx(tx)
                // Type only — amount/merchant are sensitive and must not reach logcat.
                Log.d(TAG, "Captured ${tx.type} transaction from direct SMS")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process direct SMS", e)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "SmsReceiver"

        // Survives individual receiver instances (each onReceive gets a fresh BroadcastReceiver).
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
