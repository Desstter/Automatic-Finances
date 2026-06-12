package com.example.automaticfinances.ui.components.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.automaticfinances.data.db.Category
import com.example.automaticfinances.ui.theme.FinanceShapes
import com.example.automaticfinances.ui.theme.Sizes
import com.example.automaticfinances.ui.theme.Spacing
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// ===========================================
// AutomaticFinances — Shared form components
// Date/time picker fields, category chip grid, error card, save button.
// Used by every manual-entry form so they all behave and look the same.
// ===========================================

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** One day in millis — DatePickerState works in UTC-midnight epoch millis. */
private const val DAY_MILLIS = 86_400_000L

/**
 * Read-only field that opens a Material 3 [DatePickerDialog] on tap.
 * A read-only [OutlinedTextField] swallows pointer input, so the tap target is a
 * transparent overlay box drawn on top of the field.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    value: LocalDate,
    onValueChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Fecha",
) {
    var showDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value.format(dateFormatter),
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            shape = FinanceShapes.textField,
            leadingIcon = {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(Sizes.iconMd),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClickLabel = "Seleccionar fecha",
                ) { showDialog = true },
        )
    }

    if (showDialog) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = value.toEpochDay() * DAY_MILLIS,
        )
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onValueChange(LocalDate.ofEpochDay(millis / DAY_MILLIS))
                    }
                    showDialog = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = pickerState, showModeToggle = false)
        }
    }
}

/**
 * Read-only field that opens a Material 3 [TimePicker] dialog on tap.
 * material3 1.3 has no TimePickerDialog wrapper yet, so the dialog chrome is built here
 * following the same layout as DatePickerDialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerField(
    value: LocalTime,
    onValueChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Hora",
) {
    var showDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value.format(timeFormatter),
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            shape = FinanceShapes.textField,
            leadingIcon = {
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(Sizes.iconMd),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClickLabel = "Seleccionar hora",
                ) { showDialog = true },
        )
    }

    if (showDialog) {
        val pickerState = rememberTimePickerState(
            initialHour = value.hour,
            initialMinute = value.minute,
            is24Hour = true,
        )
        Dialog(onDismissRequest = { showDialog = false }) {
            Surface(
                shape = FinanceShapes.dialog,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.xxl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Selecciona la hora",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Spacing.xl),
                    )
                    TimePicker(state = pickerState)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.sm),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { showDialog = false }) { Text("Cancelar") }
                        Spacer(Modifier.width(Spacing.sm))
                        TextButton(onClick = {
                            onValueChange(LocalTime.of(pickerState.hour, pickerState.minute))
                            showDialog = false
                        }) { Text("Aceptar") }
                    }
                }
            }
        }
    }
}

/**
 * Category picker as a wrapping grid of chips — replaces the old full-width vertical
 * card list, which forced a long scroll to reach the save button. The selected chip is
 * tinted with the category's own color so the choice reads at a glance.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryChipsGrid(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        categories.forEach { category ->
            val selected = category.id == selectedCategoryId
            val categoryColor = remember(category.color) {
                runCatching { Color(android.graphics.Color.parseColor(category.color)) }.getOrNull()
            }
            FilterChip(
                selected = selected,
                onClick = { onCategorySelected(category.id) },
                shape = FinanceShapes.categoryChip,
                leadingIcon = { Text(category.icon, style = MaterialTheme.typography.bodyLarge) },
                label = {
                    Text(
                        text = category.name,
                        maxLines = 1,
                        softWrap = false,
                    )
                },
                colors = if (categoryColor != null) {
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = categoryColor.copy(alpha = 0.22f),
                        selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onSurface,
                    )
                } else {
                    FilterChipDefaults.filterChipColors()
                },
            )
        }
    }
}

/** Inline form error surface, consistent across every entry form. */
@Composable
fun FormErrorCard(message: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(
            modifier = Modifier.padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(Sizes.iconMd),
            )
            Spacer(Modifier.width(Spacing.md))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

/** Full-width pill save button with built-in loading state. */
@Composable
fun SaveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    loadingText: String = "Guardando…",
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = FinanceShapes.primaryButton,
        modifier = modifier
            .fillMaxWidth()
            .height(Sizes.minTouchTarget + Spacing.xs),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(Sizes.iconSm),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(Spacing.sm))
        }
        Text(
            text = if (loading) loadingText else text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
