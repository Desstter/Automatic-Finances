package com.example.automaticfinances.domain

import com.example.automaticfinances.data.db.Category
import com.example.automaticfinances.data.db.Transaction

/**
 * Abstraction over the capture-feedback notification (PROD-2) so the domain layer stays free of any
 * Android dependency. [CaptureTransactionUseCase] resolves the candidate categories and hands them
 * here; the production implementation posts the chips notification, while tests use a no-op.
 */
interface CaptureFeedbackNotifier {
    /** Offers [chips] (assigned category first) as one-tap category corrections for a new [tx]. */
    fun notifyCaptured(tx: Transaction, chips: List<Category>)
}
