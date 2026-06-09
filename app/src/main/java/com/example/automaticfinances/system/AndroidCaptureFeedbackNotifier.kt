package com.example.automaticfinances.system

import android.content.Context
import com.example.automaticfinances.data.db.Category
import com.example.automaticfinances.data.db.Transaction
import com.example.automaticfinances.domain.CaptureFeedbackNotifier
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Production [CaptureFeedbackNotifier]: posts the category-chips notification (PROD-2) via
 * [CategoryChipsNotifier]. Kept thin and Android-bound so the domain layer ([CaptureTransactionUseCase])
 * stays testable without a Context.
 */
class AndroidCaptureFeedbackNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) : CaptureFeedbackNotifier {
    override fun notifyCaptured(tx: Transaction, chips: List<Category>) {
        runCatching { CategoryChipsNotifier.show(context, tx, chips) }
    }
}
