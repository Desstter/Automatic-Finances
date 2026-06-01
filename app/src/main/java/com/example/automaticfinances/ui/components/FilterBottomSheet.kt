package com.example.automaticfinances.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.automaticfinances.data.db.Category
import com.example.automaticfinances.ui.theme.Sizes
import com.example.automaticfinances.ui.theme.Spacing
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Single advanced-filter surface for the whole app. State-agnostic: the caller owns
 * the current filter values and applies the result, so the same sheet drives both the
 * transactions screen and any future list. Selections are previewed locally and only
 * committed on "Aplicar", with a live result count provided by [resultCount].
 *
 * Lives inside a [ModalBottomSheet] that should be given a bounded height
 * (e.g. `Modifier.fillMaxHeight(0.9f)`) so the scrollable body + pinned actions lay out
 * correctly without a hardcoded content height.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    categories: List<Category>,
    selectedCategoryId: Long?,
    dateStart: String?,
    dateEnd: String?,
    minAmount: Long?,
    maxAmount: Long?,
    searchQuery: String,
    numberFormat: NumberFormat,
    resultCount: (categoryId: Long?, search: String, dateStart: String?, dateEnd: String?, min: Long?, max: Long?) -> Int,
    onApply: (categoryId: Long?, search: String, dateStart: String?, dateEnd: String?, min: Long?, max: Long?) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Categorías", "Fechas", "Montos", "Buscar")

    // Pending (un-applied) selection — committed only on "Aplicar".
    var tempCategory by remember(selectedCategoryId) { mutableStateOf(selectedCategoryId ?: -1L) }
    var tempSearch by remember(searchQuery) { mutableStateOf(searchQuery) }
    var tempDateStart by remember(dateStart) { mutableStateOf(dateStart) }
    var tempDateEnd by remember(dateEnd) { mutableStateOf(dateEnd) }
    var tempMin by remember(minAmount) { mutableStateOf(minAmount ?: -1L) }
    var tempMax by remember(maxAmount) { mutableStateOf(maxAmount ?: -1L) }

    val hasActiveFilters = tempCategory != -1L ||
        tempSearch.isNotBlank() ||
        tempDateStart != null ||
        tempDateEnd != null ||
        tempMin != -1L ||
        tempMax != -1L

    // Live preview count, re-derived by the caller whenever a pending value changes.
    val filteredCount = remember(tempCategory, tempSearch, tempDateStart, tempDateEnd, tempMin, tempMax) {
        resultCount(
            if (tempCategory == -1L) null else tempCategory,
            tempSearch,
            tempDateStart,
            tempDateEnd,
            if (tempMin == -1L) null else tempMin,
            if (tempMax == -1L) null else tempMax,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(horizontal = Spacing.lg)
            .padding(bottom = Spacing.lg),
    ) {
        // Header with title + live result count
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Filtros",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            if (hasActiveFilters) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = "$filteredCount resultados",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        // Scrollable so each tab sizes to its own label — no equal-width squeeze that would
        // truncate longer words like "Categorías". Text-only keeps the row calm and legible.
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = Spacing.none,
            modifier = Modifier.fillMaxWidth(),
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            maxLines = 1,
                            softWrap = false,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        // Scrollable body fills the remaining sheet height (no hardcoded height).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            when (selectedTab) {
                0 -> CategoryFilterContent(
                    categories = categories,
                    selectedCategoryId = if (tempCategory == -1L) null else tempCategory,
                    onCategorySelected = { tempCategory = it ?: -1L },
                )
                1 -> DateFilterContent(
                    startDate = tempDateStart,
                    endDate = tempDateEnd,
                    onDateRangeChange = { start, end ->
                        tempDateStart = start
                        tempDateEnd = end
                    },
                )
                2 -> AmountFilterContent(
                    minAmount = if (tempMin == -1L) null else tempMin,
                    maxAmount = if (tempMax == -1L) null else tempMax,
                    numberFormat = numberFormat,
                    onAmountRangeChange = { min, max ->
                        tempMin = min ?: -1L
                        tempMax = max ?: -1L
                    },
                )
                3 -> SearchFilterContent(
                    searchQuery = tempSearch,
                    onSearchQueryChange = { tempSearch = it },
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            OutlinedButton(
                onClick = {
                    tempCategory = -1L
                    tempSearch = ""
                    tempDateStart = null
                    tempDateEnd = null
                    tempMin = -1L
                    tempMax = -1L
                    onClearAll()
                },
                modifier = Modifier.weight(1f),
                enabled = hasActiveFilters,
            ) {
                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(Sizes.iconSm))
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text("Limpiar")
            }

            Button(
                onClick = {
                    onApply(
                        if (tempCategory == -1L) null else tempCategory,
                        tempSearch,
                        tempDateStart,
                        tempDateEnd,
                        if (tempMin == -1L) null else tempMin,
                        if (tempMax == -1L) null else tempMax,
                    )
                    onDismiss()
                },
                modifier = Modifier.weight(2f),
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(Sizes.iconSm))
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text("Aplicar filtros")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilterContent(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long?) -> Unit,
) {
    Column {
        Text(
            text = "Selecciona una categoría",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = Spacing.md),
        )

        FilterChip(
            selected = selectedCategoryId == null,
            onClick = { onCategorySelected(null) },
            label = { Text("Todas las categorías") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.sm),
        )

        categories.chunked(2).forEach { rowCategories ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                rowCategories.forEach { category ->
                    FilterChip(
                        selected = selectedCategoryId == category.id,
                        onClick = { onCategorySelected(category.id) },
                        label = { Text("${category.icon} ${category.name}", maxLines = 1) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowCategories.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DateFilterContent(
    startDate: String?,
    endDate: String?,
    onDateRangeChange: (String?, String?) -> Unit,
) {
    val isoFormat = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
    Column {
        Text(
            text = "Rango de fechas",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = Spacing.md),
        )

        val quickFilters = listOf(
            "Hoy" to {
                val today = LocalDate.now().format(isoFormat)
                onDateRangeChange(today, today)
            },
            "Esta semana" to {
                val today = LocalDate.now()
                val weekStart = today.minusDays(today.dayOfWeek.value - 1L)
                onDateRangeChange(weekStart.format(isoFormat), today.format(isoFormat))
            },
            "Este mes" to {
                val today = LocalDate.now()
                onDateRangeChange(today.withDayOfMonth(1).format(isoFormat), today.format(isoFormat))
            },
            "Últimos 30 días" to {
                val today = LocalDate.now()
                onDateRangeChange(today.minusDays(30).format(isoFormat), today.format(isoFormat))
            },
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.padding(bottom = Spacing.lg),
        ) {
            items(quickFilters) { (label, action) ->
                FilterChip(selected = false, onClick = action, label = { Text(label) })
            }
        }

        if (startDate != null || endDate != null) {
            SelectionCard(
                label = "Rango seleccionado",
                value = "${startDate ?: "…"} → ${endDate ?: "…"}",
            )
        }

        OutlinedButton(
            onClick = { onDateRangeChange(null, null) },
            enabled = startDate != null || endDate != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Limpiar fechas")
        }
    }
}

@Composable
private fun AmountFilterContent(
    minAmount: Long?,
    maxAmount: Long?,
    numberFormat: NumberFormat,
    onAmountRangeChange: (Long?, Long?) -> Unit,
) {
    Column {
        Text(
            text = "Rango de montos",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = Spacing.md),
        )

        val quickFilters = listOf(
            "< \$50K" to { onAmountRangeChange(null, 5_000_000L) },
            "\$50K – \$200K" to { onAmountRangeChange(5_000_000L, 20_000_000L) },
            "> \$200K" to { onAmountRangeChange(20_000_000L, null) },
            "> \$500K" to { onAmountRangeChange(50_000_000L, null) },
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.padding(bottom = Spacing.lg),
        ) {
            items(quickFilters) { (label, action) ->
                FilterChip(selected = false, onClick = action, label = { Text(label) })
            }
        }

        if (minAmount != null || maxAmount != null) {
            SelectionCard(
                label = "Rango seleccionado",
                value = "${if (minAmount != null) numberFormat.format(minAmount / 100.0) else "Sin mínimo"} – " +
                    (if (maxAmount != null) numberFormat.format(maxAmount / 100.0) else "Sin máximo"),
            )
        }

        OutlinedButton(
            onClick = { onAmountRangeChange(null, null) },
            enabled = minAmount != null || maxAmount != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Limpiar montos")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchFilterContent(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
) {
    Column {
        Text(
            text = "Buscar en transacciones",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = Spacing.md),
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text("Buscar por descripción…") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                    }
                }
            },
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        Text(
            text = "Busca por nombre del comercio, descripción o cualquier texto de la transacción.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}

/** Tonal card that echoes the currently-selected range for date / amount tabs. */
@Composable
private fun SelectionCard(label: String, value: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.md),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}
