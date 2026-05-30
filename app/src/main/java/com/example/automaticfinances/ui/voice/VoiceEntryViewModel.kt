package com.example.automaticfinances.ui.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automaticfinances.data.db.Category
import com.example.automaticfinances.data.remote.GeminiException
import com.example.automaticfinances.data.remote.GeminiFailure
import com.example.automaticfinances.data.repo.CategoryRepository
import com.example.automaticfinances.data.voice.VoiceTransactionParser
import com.example.automaticfinances.data.voice.VoiceTransactionDraft
import com.example.automaticfinances.domain.AddTransactionUseCase
import com.example.automaticfinances.domain.BuildVoiceTransactionsUseCase
import com.example.automaticfinances.system.SpeechRecognizerManager
import com.example.automaticfinances.system.SttError
import com.example.automaticfinances.system.SttEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Why a voice entry failed, so the UI can offer the right recovery (retry vs. manual entry). */
enum class VoiceErrorKind { NOT_AVAILABLE, MIC, NETWORK, QUOTA, NO_RESULT, SAVE, GENERIC }

/** Single source of truth for the voice overlay. The Activity renders one screen per state. */
sealed interface VoiceUiState {
    /** Before mic permission is resolved by the Activity. */
    data object Preparing : VoiceUiState
    data object PermissionDenied : VoiceUiState
    data class Listening(
        val partialText: String = "",
        val rms: Float = 0f,
        val isSpeaking: Boolean = false,
    ) : VoiceUiState
    data class Processing(val transcript: String) : VoiceUiState
    data class Review(
        val transcript: String,
        val drafts: List<VoiceTransactionDraft>,
    ) : VoiceUiState
    data object Saving : VoiceUiState
    data class Saved(val count: Int) : VoiceUiState
    data class Failed(
        val kind: VoiceErrorKind,
        val message: String,
        val transcript: String? = null,
    ) : VoiceUiState
}

@HiltViewModel
class VoiceEntryViewModel @Inject constructor(
    private val speechRecognizer: SpeechRecognizerManager,
    private val parser: VoiceTransactionParser,
    private val buildVoiceTransactions: BuildVoiceTransactionsUseCase,
    private val addTransaction: AddTransactionUseCase,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<VoiceUiState>(VoiceUiState.Preparing)
    val state: StateFlow<VoiceUiState> = _state.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private var listenJob: Job? = null

    init {
        viewModelScope.launch {
            _categories.value = categoryRepository.getAllActiveSync()
        }
    }

    // ---- Permission gate (driven by the Activity) ----

    fun onMicPermissionGranted() {
        if (!speechRecognizer.isAvailable()) {
            _state.value = VoiceUiState.Failed(
                VoiceErrorKind.NOT_AVAILABLE,
                "El reconocimiento de voz no está disponible en este dispositivo",
            )
            return
        }
        startListening()
    }

    fun onMicPermissionDenied() {
        _state.value = VoiceUiState.PermissionDenied
    }

    // ---- Listening ----

    fun startListening() {
        listenJob?.cancel()
        _state.value = VoiceUiState.Listening()
        listenJob = speechRecognizer.listen()
            .onEach { event -> handleSttEvent(event) }
            .launchIn(viewModelScope)
    }

    private fun handleSttEvent(event: SttEvent) {
        val current = _state.value
        when (event) {
            is SttEvent.ReadyForSpeech ->
                if (current is VoiceUiState.Listening) _state.value = current.copy(isSpeaking = false)
            is SttEvent.BeginningOfSpeech ->
                if (current is VoiceUiState.Listening) _state.value = current.copy(isSpeaking = true)
            is SttEvent.RmsChanged ->
                if (current is VoiceUiState.Listening) _state.value = current.copy(rms = event.rms)
            is SttEvent.PartialResult ->
                if (current is VoiceUiState.Listening) _state.value = current.copy(partialText = event.text)
            is SttEvent.FinalResult -> onTranscript(event.text)
            is SttEvent.Failed -> _state.value = VoiceUiState.Failed(event.error.toKind(), event.message)
        }
    }

    private fun onTranscript(transcript: String) {
        val clean = transcript.trim()
        if (clean.isEmpty()) {
            _state.value = VoiceUiState.Failed(VoiceErrorKind.NO_RESULT, "No te entendí, intenta de nuevo")
            return
        }
        _state.value = VoiceUiState.Processing(clean)
        viewModelScope.launch {
            try {
                val parsed = parser.parse(clean, _categories.value)
                val drafts = buildVoiceTransactions.toDrafts(parsed)
                _state.value = if (drafts.isEmpty()) {
                    VoiceUiState.Failed(
                        VoiceErrorKind.NO_RESULT,
                        "No encontré ninguna transacción en lo que dijiste",
                        transcript = clean,
                    )
                } else {
                    VoiceUiState.Review(clean, drafts)
                }
            } catch (e: GeminiException) {
                _state.value = VoiceUiState.Failed(e.failure.toKind(), userMessageFor(e), transcript = clean)
            } catch (e: Exception) {
                _state.value = VoiceUiState.Failed(
                    VoiceErrorKind.GENERIC,
                    "No se pudo interpretar lo que dijiste",
                    transcript = clean,
                )
            }
        }
    }

    fun cancelListening() {
        listenJob?.cancel()
        listenJob = null
    }

    fun retry() {
        cancelListening()
        startListening()
    }

    // ---- Review editing ----

    fun updateDraftAmount(draftId: String, amountCents: Long) = mutateDraft(draftId) {
        it.copy(amountCents = amountCents.coerceAtLeast(0L), needsReview = false)
    }

    fun updateDraftDescription(draftId: String, description: String) = mutateDraft(draftId) {
        it.copy(description = description)
    }

    fun updateDraftCategory(draftId: String, categoryId: Long) = mutateDraft(draftId) {
        it.copy(categoryId = categoryId)
    }

    fun setDraftIsIncome(draftId: String, isIncome: Boolean) = mutateDraft(draftId) { draft ->
        if (draft.isIncome == isIncome) return@mutateDraft draft
        // Switching expense<->income invalidates the selected category (different set).
        draft.copy(isIncome = isIncome, categoryId = null)
    }

    fun removeDraft(draftId: String) {
        val review = _state.value as? VoiceUiState.Review ?: return
        val remaining = review.drafts.filterNot { it.draftId == draftId }
        _state.value = if (remaining.isEmpty()) {
            VoiceUiState.Failed(
                VoiceErrorKind.NO_RESULT,
                "Quitaste todas las transacciones",
                transcript = review.transcript,
            )
        } else {
            review.copy(drafts = remaining)
        }
    }

    private inline fun mutateDraft(draftId: String, transform: (VoiceTransactionDraft) -> VoiceTransactionDraft) {
        val review = _state.value as? VoiceUiState.Review ?: return
        _state.value = review.copy(
            drafts = review.drafts.map { if (it.draftId == draftId) transform(it) else it },
        )
    }

    // ---- Save ----

    /** Persists every reviewed draft. Guarded so a double-tap can't double-save (idempotency). */
    fun confirmSave() {
        val review = _state.value as? VoiceUiState.Review ?: return
        val valid = review.drafts.filter { it.amountCents > 0 && it.description.isNotBlank() }
        if (valid.isEmpty()) return

        _state.value = VoiceUiState.Saving
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                valid.forEach { draft ->
                    val tx = buildVoiceTransactions.buildTransaction(draft, now)
                    addTransaction(tx)
                }
                _state.value = VoiceUiState.Saved(valid.size)
            } catch (e: Exception) {
                _state.value = VoiceUiState.Failed(
                    VoiceErrorKind.SAVE,
                    "No se pudo guardar: ${e.message ?: "error desconocido"}",
                    transcript = review.transcript,
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        listenJob?.cancel()
    }

    private fun userMessageFor(e: GeminiException): String = when (e.failure) {
        GeminiFailure.MISSING_KEY -> "Falta configurar la API de voz"
        GeminiFailure.NETWORK -> "Sin conexión. Revisa tu internet e intenta de nuevo"
        GeminiFailure.QUOTA -> "Se alcanzó el límite diario del servicio de voz"
        GeminiFailure.AUTH -> "La clave del servicio de voz no es válida"
        GeminiFailure.BLOCKED -> "No se pudo procesar ese audio"
        GeminiFailure.SERVER -> "El servicio de voz no está disponible ahora"
        GeminiFailure.EMPTY, GeminiFailure.UNKNOWN -> "No se pudo interpretar lo que dijiste"
    }
}

private fun GeminiFailure.toKind(): VoiceErrorKind = when (this) {
    GeminiFailure.NETWORK -> VoiceErrorKind.NETWORK
    GeminiFailure.QUOTA -> VoiceErrorKind.QUOTA
    GeminiFailure.MISSING_KEY, GeminiFailure.AUTH -> VoiceErrorKind.GENERIC
    GeminiFailure.BLOCKED, GeminiFailure.EMPTY, GeminiFailure.SERVER, GeminiFailure.UNKNOWN -> VoiceErrorKind.GENERIC
}

private fun SttError.toKind(): VoiceErrorKind = when (this) {
    SttError.NOT_AVAILABLE -> VoiceErrorKind.NOT_AVAILABLE
    SttError.PERMISSION -> VoiceErrorKind.MIC
    SttError.NETWORK -> VoiceErrorKind.NETWORK
    SttError.AUDIO, SttError.NO_MATCH, SttError.SPEECH_TIMEOUT, SttError.BUSY, SttError.OTHER -> VoiceErrorKind.NO_RESULT
}
