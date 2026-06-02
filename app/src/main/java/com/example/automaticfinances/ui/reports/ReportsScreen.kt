package com.example.automaticfinances.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Thin orchestrator for the Reports screen: owns the scaffold and decides, per slot, whether to
 * show a skeleton, the real section, or an empty state. The sections themselves live in
 * ReportsSections.kt / ReportsInsights.kt and the transient states in ReportsLoadingStates.kt so
 * each renders only the slice of state it needs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: ReportsViewModel = hiltViewModel()

    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadReports()
    }

    // Error handling with retry
    state.error?.let { error ->
        LaunchedEffect(error) {
            // Auto-dismiss error after showing it briefly
            kotlinx.coroutines.delay(5000)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Reportes",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    TextButton(onClick = { /* TODO: Export functionality */ }) {
                        Text("Exportar")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Time period selector with quick filters
            item {
                TimePeriodSelector(
                    selectedPeriod = state.selectedPeriod,
                    onPeriodChanged = viewModel::selectPeriod
                )
            }

            // Error state with retry
            state.error?.let { error ->
                item {
                    ErrorCard(
                        error = error,
                        onRetry = { viewModel.loadReports() },
                        onDismiss = { viewModel.clearError() }
                    )
                }
            }

            // Summary cards or empty state
            if (state.isLoading) {
                item {
                    SummarySkeletonLoader()
                }
            } else if (state.summary != null) {
                item {
                    ReportsSummarySection(
                        summary = state.summary,
                        selectedPeriod = state.selectedPeriod
                    )
                }
            } else if (!state.isLoading && state.error == null) {
                item {
                    EmptyReportsCard(
                        onRetry = { viewModel.loadReports() }
                    )
                }
            }

            // Category breakdown
            if (state.isLoading) {
                item {
                    CategorySkeletonLoader()
                }
            } else if (state.categoryBreakdown.isNotEmpty()) {
                item {
                    CategoryBreakdownSection(
                        breakdown = state.categoryBreakdown
                    )
                }
            }

            // Monthly trends
            if (state.isLoading) {
                item {
                    TrendsSkeletonLoader()
                }
            } else if (state.monthlyTrends.isNotEmpty()) {
                item {
                    MonthlyTrendsSection(
                        trends = state.monthlyTrends
                    )
                }
            }

            // Top transactions
            if (state.isLoading) {
                item {
                    TransactionsSkeletonLoader()
                }
            } else if (state.topTransactions.isNotEmpty()) {
                item {
                    TopTransactionsSection(
                        transactions = state.topTransactions
                    )
                }
            }

            // Additional insights
            item {
                InsightsSection(
                    insights = state.insights
                )
            }
        }
    }

    // Loading overlay for initial load only
    if (state.isLoading && state.summary == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Cargando reportes...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
