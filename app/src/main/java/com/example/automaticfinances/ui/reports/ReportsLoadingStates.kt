package com.example.automaticfinances.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.automaticfinances.ui.components.common.PremiumEmptyState

// Transient states for the Reports screen: error, empty, and per-section skeleton loaders.
// The skeletons share a single [SkeletonBlock] primitive so their shape stays consistent and the
// boilerplate stays in one place.

/** A neutral placeholder block used to compose the loading skeletons. */
@Composable
private fun SkeletonBlock(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    corner: Dp = 4.dp,
) {
    Box(
        modifier = modifier
            .height(height)
            .background(
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                RoundedCornerShape(corner)
            )
    )
}

@Composable
internal fun ErrorCard(
    error: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Error",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onRetry) {
                    Text(
                        text = "Reintentar",
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
internal fun EmptyReportsCard(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        PremiumEmptyState(
            icon = Icons.AutoMirrored.Filled.ShowChart,
            title = "Sin datos en este período",
            description = "No se encontraron transacciones para el período seleccionado.",
            actionLabel = "Actualizar",
            onAction = onRetry
        )
    }
}

@Composable
internal fun SummarySkeletonLoader(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SkeletonBlock(modifier = Modifier.fillMaxWidth(0.4f), height = 20.dp)

            repeat(3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    repeat(2) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SkeletonBlock(modifier = Modifier.size(24.dp), height = 24.dp, corner = 12.dp)
                            SkeletonBlock(modifier = Modifier.fillMaxWidth(0.7f))
                            SkeletonBlock(modifier = Modifier.fillMaxWidth(0.5f), height = 14.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CategorySkeletonLoader(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SkeletonBlock(modifier = Modifier.fillMaxWidth(0.5f), height = 20.dp)

            repeat(5) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        SkeletonBlock(modifier = Modifier.size(24.dp), height = 24.dp, corner = 12.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            SkeletonBlock(modifier = Modifier.width(80.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            SkeletonBlock(modifier = Modifier.width(60.dp), height = 12.dp)
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        SkeletonBlock(modifier = Modifier.width(70.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        SkeletonBlock(modifier = Modifier.width(30.dp), height = 12.dp)
                    }
                }
            }
        }
    }
}

@Composable
internal fun TrendsSkeletonLoader(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SkeletonBlock(modifier = Modifier.fillMaxWidth(0.4f), height = 20.dp)

            repeat(6) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SkeletonBlock(modifier = Modifier.width(80.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SkeletonBlock(modifier = Modifier.width(70.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        SkeletonBlock(modifier = Modifier.size(16.dp), height = 16.dp, corner = 8.dp)
                    }
                }
            }
        }
    }
}

@Composable
internal fun TransactionsSkeletonLoader(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SkeletonBlock(modifier = Modifier.fillMaxWidth(0.6f), height = 20.dp)

            repeat(5) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        SkeletonBlock(modifier = Modifier.fillMaxWidth(0.7f))
                        Spacer(modifier = Modifier.height(4.dp))
                        SkeletonBlock(modifier = Modifier.fillMaxWidth(0.5f), height = 12.dp)
                    }

                    SkeletonBlock(modifier = Modifier.width(70.dp))
                }
            }
        }
    }
}
