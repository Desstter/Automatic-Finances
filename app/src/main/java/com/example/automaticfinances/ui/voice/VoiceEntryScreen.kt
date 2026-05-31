package com.example.automaticfinances.ui.voice

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.automaticfinances.data.db.Category
import com.example.automaticfinances.data.voice.VoiceTransactionDraft
import com.example.automaticfinances.ui.theme.Spacing
import com.example.automaticfinances.utils.parseColombiaCents
import java.text.NumberFormat
import java.util.Locale

private val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceEntryScreen(
    state: VoiceUiState,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onConfirmSave: () -> Unit,
    onAmountChange: (String, Long) -> Unit,
    onDescriptionChange: (String, String) -> Unit,
    onCategorySelect: (String, Long) -> Unit,
    onIncomeToggle: (String, Boolean) -> Unit,
    onRemoveDraft: (String) -> Unit,
) {
    // Keep the dismissable flag fresh inside confirmValueChange so a drag can't dismiss
    // the sheet mid-save.
    val dismissable by rememberUpdatedState(state !is VoiceUiState.Saving)
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { dismissable },
    )

    ModalBottomSheet(
        onDismissRequest = { if (dismissable) onDismiss() },
        sheetState = sheetState,
        shape = sheetShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        // Custom grab handle is rendered inside the content for consistent spacing.
        dragHandle = null,
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "voiceState",
        ) { current ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.xl, vertical = Spacing.xl),
            ) {
                GrabHandle()
                Spacer(Modifier.height(Spacing.lg))
                when (current) {
                    is VoiceUiState.Preparing -> CenteredLoader("Preparando micrófono…")
                    is VoiceUiState.PermissionDenied -> PermissionContent(onOpenAppSettings, onDismiss)
                    is VoiceUiState.Listening -> ListeningContent(current, onDismiss)
                    is VoiceUiState.Processing -> ProcessingContent(current)
                    is VoiceUiState.Review -> ReviewContent(
                        state = current,
                        categories = categories,
                        onConfirmSave = onConfirmSave,
                        onDismiss = onDismiss,
                        onAmountChange = onAmountChange,
                        onDescriptionChange = onDescriptionChange,
                        onCategorySelect = onCategorySelect,
                        onIncomeToggle = onIncomeToggle,
                        onRemoveDraft = onRemoveDraft,
                    )
                    is VoiceUiState.Saving -> CenteredLoader("Guardando…")
                    is VoiceUiState.Saved -> SavedContent(current)
                    is VoiceUiState.Failed -> FailedContent(current, onRetry, onDismiss, onOpenAppSettings)
                }
            }
        }
    }
}

@Composable
private fun GrabHandle() {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)),
        )
    }
}

@Composable
private fun CenteredLoader(label: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(Spacing.lg))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ListeningContent(state: VoiceUiState.Listening, onCancel: () -> Unit) {
    // Normalize RMS (~ -2..10 dB) into a gentle pulse on the mic badge.
    val targetScale = 1f + (state.rms.coerceIn(0f, 10f) / 10f) * 0.35f
    val scale by animateFloatAsState(targetValue = if (state.isSpeaking) targetScale else 1f, label = "micPulse")

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = Spacing.md)) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .size(96.dp)
                    .scale(scale),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Mic,
                        contentDescription = "Escuchando",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(Spacing.lg))
        Text(
            text = if (state.isSpeaking) "Te escucho…" else "Habla ahora",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = state.partialText.ifBlank { "Ej: \"galletas 12 mil y un tinto 2\"" },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md),
        )
        Spacer(Modifier.height(Spacing.xl))
        TextButton(onClick = onCancel) { Text("Cancelar") }
    }
}

@Composable
private fun ProcessingContent(state: VoiceUiState.Processing) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(Spacing.lg))
        Text("Entendiendo lo que dijiste…", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = "\"${state.transcript}\"",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewContent(
    state: VoiceUiState.Review,
    categories: List<Category>,
    onConfirmSave: () -> Unit,
    onDismiss: () -> Unit,
    onAmountChange: (String, Long) -> Unit,
    onDescriptionChange: (String, String) -> Unit,
    onCategorySelect: (String, Long) -> Unit,
    onIncomeToggle: (String, Boolean) -> Unit,
    onRemoveDraft: (String) -> Unit,
) {
    val nf = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO")) }
    val total = state.drafts.sumOf { if (it.isIncome) it.amountCents else -it.amountCents }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (state.drafts.size == 1) "Confirma la transacción" else "Confirma las transacciones",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = "\"${state.transcript}\"",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.lg))

        Column(
            modifier = Modifier
                .heightIn(max = 380.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            state.drafts.forEach { draft ->
                DraftCard(
                    draft = draft,
                    categories = categories,
                    nf = nf,
                    canRemove = state.drafts.size > 1,
                    onAmountChange = { cents -> onAmountChange(draft.draftId, cents) },
                    onDescriptionChange = { text -> onDescriptionChange(draft.draftId, text) },
                    onCategorySelect = { id -> onCategorySelect(draft.draftId, id) },
                    onIncomeToggle = { income -> onIncomeToggle(draft.draftId, income) },
                    onRemove = { onRemoveDraft(draft.draftId) },
                )
            }
        }

        if (state.drafts.size > 1) {
            Spacer(Modifier.height(Spacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = nf.format(total / 100.0),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (total < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(Modifier.height(Spacing.xl))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar") }
            Button(onClick = onConfirmSave, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text(if (state.drafts.size > 1) "Guardar ${state.drafts.size}" else "Guardar")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DraftCard(
    draft: VoiceTransactionDraft,
    categories: List<Category>,
    nf: NumberFormat,
    canRemove: Boolean,
    onAmountChange: (Long) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCategorySelect: (Long) -> Unit,
    onIncomeToggle: (Boolean) -> Unit,
    onRemove: () -> Unit,
) {
    val borderColor = if (draft.needsReview) MaterialTheme.colorScheme.tertiary else Color.Transparent
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (draft.isIncome) "Ingreso" else "Gasto",
                    style = MaterialTheme.typography.labelLarge,
                    color = borderColor.takeIf { draft.needsReview } ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (draft.needsReview) {
                    Text(
                        text = "Revisa el monto",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                if (canRemove) {
                    // 48dp keeps the tap target at the accessibility minimum even though the
                    // glyph itself is small.
                    IconButton(onClick = onRemove, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Quitar", modifier = Modifier.size(18.dp))
                    }
                }
            }
            Spacer(Modifier.height(Spacing.sm))

            // Description
            OutlinedTextField(
                value = draft.description,
                onValueChange = onDescriptionChange,
                label = { Text("Descripción") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Spacing.sm))

            // Amount (whole pesos; converted to cents on change)
            var amountText by rememberSaveable(draft.draftId) {
                mutableStateOf(if (draft.amountCents > 0) (draft.amountCents / 100).toString() else "")
            }
            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    val cleaned = input.filter { it.isDigit() || it == '.' || it == ',' }.take(15)
                    amountText = cleaned
                    onAmountChange(cleaned.parseColombiaCents() ?: 0L)
                },
                label = { Text("Monto (COP)") },
                prefix = { Text("$") },
                singleLine = true,
                isError = draft.amountCents <= 0L,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Spacing.sm))

            // Income / expense toggle
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                FilterChip(
                    selected = !draft.isIncome,
                    onClick = { onIncomeToggle(false) },
                    label = { Text("Gasto") },
                )
                FilterChip(
                    selected = draft.isIncome,
                    onClick = { onIncomeToggle(true) },
                    label = { Text("Ingreso") },
                )
            }
            Spacer(Modifier.height(Spacing.sm))

            // Category picker (filtered by income/expense)
            CategoryDropdown(
                categories = categories.filter { it.isIncome == draft.isIncome },
                selectedId = draft.categoryId,
                onSelect = onCategorySelect,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<Category>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = categories.firstOrNull { it.id == selectedId }
    val labelText = selected?.let { "${it.icon}  ${it.name}" } ?: "Sin categoría"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = labelText,
            onValueChange = {},
            readOnly = true,
            label = { Text("Categoría") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text("${category.icon}  ${category.name}") },
                    onClick = {
                        onSelect(category.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun SavedContent(state: VoiceUiState.Saved) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(72.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        Spacer(Modifier.height(Spacing.lg))
        Text(
            text = if (state.count == 1) "Transacción guardada" else "${state.count} transacciones guardadas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun PermissionContent(onOpenAppSettings: () -> Unit, onDismiss: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.md)) {
        Text("Permiso de micrófono", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Spacing.sm))
        Text(
            "Para registrar gastos por voz necesito acceso al micrófono. Actívalo en los ajustes de la app.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.xl))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cerrar") }
            Button(onClick = onOpenAppSettings, modifier = Modifier.weight(1f)) { Text("Abrir ajustes") }
        }
    }
}

@Composable
private fun FailedContent(
    state: VoiceUiState.Failed,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    onOpenAppSettings: () -> Unit,
) {
    val canRetry = state.kind != VoiceErrorKind.NOT_AVAILABLE
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.md), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.size(64.dp)) {
            Box(contentAlignment = Alignment.Center) {
                val icon = if (state.kind == VoiceErrorKind.NETWORK) Icons.Outlined.CloudOff else Icons.Filled.Close
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(30.dp))
            }
        }
        Spacer(Modifier.height(Spacing.lg))
        Text(state.message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Spacing.xl))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cerrar") }
            if (state.kind == VoiceErrorKind.MIC) {
                Button(onClick = onOpenAppSettings, modifier = Modifier.weight(1f)) { Text("Ajustes") }
            } else if (canRetry) {
                Button(onClick = onRetry, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Reintentar")
                }
            }
        }
    }
}
