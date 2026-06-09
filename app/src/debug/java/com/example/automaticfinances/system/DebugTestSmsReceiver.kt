package com.example.automaticfinances.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
 * DEBUG-ONLY test hook. Exists only in the debug source set, so it is never compiled into or
 * registered by release builds.
 *
 * Lets us inject a synthetic bank SMS through the *exact same* capture pipeline the real
 * [SmsReceiver] uses (BancolombiaParser → CaptureTransactionUseCase), so the end-to-end flow can be
 * verified over ADB in seconds without waiting for a real bank SMS or making a real purchase:
 *
 *   adb shell am broadcast -p com.example.automaticfinances.debug \
 *     -a com.example.automaticfinances.debug.TEST_SMS \
 *     --es body "Bancolombia: Compraste COP6.000,00 en PAYU COLOMBIA SAS con tu T.Cred *9335, el 04/06/2026 a las 21:34."
 *
 * Optional --el ts <epochMillis> overrides the timestamp (defaults to now); it folds into the stable
 * dedup id exactly like the real path, so re-broadcasting the same body+ts is a harmless no-op.
 */
@AndroidEntryPoint
class DebugTestSmsReceiver : BroadcastReceiver() {

    @Inject lateinit var captureTx: CaptureTransactionUseCase
    @Inject lateinit var unparsedRepo: UnparsedSmsRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val body = intent.getStringExtra("body")?.takeIf { it.isNotBlank() } ?: run {
            Log.w(TAG, "TEST_SMS ignored: missing --es body")
            return
        }
        val ts = intent.getLongExtra("ts", System.currentTimeMillis())

        val pending = goAsync()
        scope.launch {
            try {
                val tx = BancolombiaParser.tryParse(body, ts)
                if (tx == null) {
                    Log.w(TAG, "TEST_SMS did not parse; recording as unparsed: $body")
                    unparsedRepo.record(body, source = "sms:test", receivedAt = ts)
                    return@launch
                }
                captureTx(tx)
                Log.i(TAG, "TEST_SMS captured ${tx.type} amountCents=${tx.amountCents} merchant=${tx.description} id=${tx.id}")
            } catch (e: Exception) {
                Log.e(TAG, "TEST_SMS processing failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "DebugTestSms"
        const val ACTION = "com.example.automaticfinances.debug.TEST_SMS"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
