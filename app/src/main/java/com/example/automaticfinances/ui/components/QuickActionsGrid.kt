package com.example.automaticfinances.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.automaticfinances.ui.theme.FinanceTheme
import kotlin.math.ceil

data class QuickAction(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

private fun calculateGridHeight(itemCount: Int, columns: Int): androidx.compose.ui.unit.Dp {
    val rows = ceil(itemCount.toFloat() / columns.toFloat()).toInt()
    val itemHeight = 96.dp
    val verticalSpacing = 12.dp
    return itemHeight * rows + verticalSpacing * (rows - 1).coerceAtLeast(0)
}

@Composable
fun QuickActionsGrid(
    actions: List<QuickAction>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val screenWidthDp = context.resources.configuration.screenWidthDp

    val columns = when {
        screenWidthDp >= 840 -> 4
        screenWidthDp >= 600 -> 4
        else                 -> 3
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Acciones Rápidas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(calculateGridHeight(actions.size, columns))
            ) {
                items(actions) { action ->
                    QuickActionCard(
                        title = action.title,
                        icon = action.icon,
                        onClick = action.onClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 1.dp,
            pressedElevation = 4.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(36.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Preview(name = "Compact Phone — 3 Columns", widthDp = 360, heightDp = 640)
@Composable
private fun QuickActionsGridCompactPreview() {
    FinanceTheme {
        Surface {
            QuickActionsGrid(
                actions = listOf(
                    QuickAction("Presupuestos", Icons.Default.AccountBalanceWallet) { },
                    QuickAction("Metas",        Icons.Default.Flag)                 { },
                    QuickAction("Reportes",     Icons.Default.BarChart)             { }
                ),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "Medium Tablet — 4 Columns", widthDp = 720, heightDp = 1024)
@Composable
private fun QuickActionsGridMediumPreview() {
    FinanceTheme {
        Surface {
            QuickActionsGrid(
                actions = listOf(
                    QuickAction("Presupuestos", Icons.Default.AccountBalanceWallet) { },
                    QuickAction("Metas",        Icons.Default.Flag)                 { },
                    QuickAction("Reportes",     Icons.Default.BarChart)             { },
                    QuickAction("Categorías",   Icons.Default.Category)             { }
                ),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(
    name = "Dark Theme — Compact",
    widthDp = 360,
    heightDp = 640,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun QuickActionsGridDarkPreview() {
    FinanceTheme(darkTheme = true) {
        Surface {
            QuickActionsGrid(
                actions = listOf(
                    QuickAction("Presupuestos", Icons.Default.AccountBalanceWallet) { },
                    QuickAction("Metas",        Icons.Default.Flag)                 { },
                    QuickAction("Reportes",     Icons.Default.BarChart)             { }
                ),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
