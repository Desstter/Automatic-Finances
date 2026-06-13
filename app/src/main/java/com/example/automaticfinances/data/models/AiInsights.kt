package com.example.automaticfinances.data.models

import com.example.automaticfinances.data.remote.LlmFailure

/**
 * Natural-language financial analysis produced by the AI advisor
 * ([com.example.automaticfinances.data.repo.FinancialAdvisorRepository]) on top of the deterministic
 * [InsightsReport]. The numbers stay the source of truth; this layer only adds human framing and
 * actionable advice, so it is safe to render as null/absent when the AI is disabled or unreachable.
 */
data class AiFinancialInsights(
    /** One or two sentences summarizing how the month is going. */
    val summary: String,
    /** Concrete, prioritized observations and recommendations. */
    val tips: List<AiTip>,
)

enum class AiTone {
    /** Neutral observation or context. */
    INFO,

    /** Something going well — savings, under-budget, lower run-rate. */
    POSITIVE,

    /** Something to watch — overspending, recurring leaks, anomalies. */
    WARNING,
}

/** A single advisor recommendation: a short headline plus a one-line explanation. */
data class AiTip(
    val title: String,
    val body: String,
    val tone: AiTone,
)

/**
 * What the dashboard should render for the AI advisor section. Produced by
 * [com.example.automaticfinances.data.repo.FinancialAdvisorRepository] (everything except [Loading],
 * which the ViewModel sets while a call is in flight) and consumed by
 * [com.example.automaticfinances.ui.components.AiAdvisorCard].
 */
sealed interface AdvisorUiState {
    /** Advisor off, or nothing to analyze yet — the card is not shown at all. */
    data object Hidden : AdvisorUiState

    /** A call is in flight. */
    data object Loading : AdvisorUiState

    /** Got a usable narrative. */
    data class Success(val insights: AiFinancialInsights) : AdvisorUiState

    /** The call failed; [failure] drives the recovery the card offers (fix key / retry). */
    data class Error(val failure: LlmFailure) : AdvisorUiState
}
