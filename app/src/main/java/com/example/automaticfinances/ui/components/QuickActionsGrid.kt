package com.example.automaticfinances.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlin.math.ceil
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.example.automaticfinances.ui.theme.FinanceTheme

data class QuickAction(
    val title: String,
    val icon: String,
    val onClick: () -> Unit
)

@Composable
private fun getIconForAction(icon: String): Any {
    return when (icon) {
        "⚙️" -> Icons.Filled.Settings
        "❓" -> Icons.Filled.Help
        else -> icon // Return emoji as string for others
    }
}

/**
 * Calculates the required height for the LazyVerticalGrid based on content
 */
private fun calculateGridHeight(itemCount: Int, columns: Int): androidx.compose.ui.unit.Dp {
    val rows = ceil(itemCount.toFloat() / columns.toFloat()).toInt()
    val itemHeight = 96.dp // Height of each QuickActionCard
    val verticalSpacing = 12.dp // Spacing between rows
    
    return itemHeight * rows + verticalSpacing * (rows - 1).coerceAtLeast(0)
}

@Composable
fun QuickActionsGrid(
    actions: List<QuickAction>,
    modifier: Modifier = Modifier
) {
    // Calculate columns based on screen size
    val context = LocalContext.current
    val configuration = context.resources.configuration
    val screenWidthDp = configuration.screenWidthDp
    
    val columns = when {
        screenWidthDp >= 840 -> 4  // Expanded (tablet)
        screenWidthDp >= 600 -> 4  // Medium (large phone/small tablet)
        else -> 3              // Compact (phone)
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "⚡ Acciones Rápidas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
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
    icon: String,
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
            defaultElevation = 2.dp,
            pressedElevation = 6.dp
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
            // Icon container
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(32.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    val iconContent = getIconForAction(icon)
                    when (iconContent) {
                        is androidx.compose.ui.graphics.vector.ImageVector -> {
                            Icon(
                                imageVector = iconContent,
                                contentDescription = title,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        else -> {
                            Text(
                                text = iconContent.toString(),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
            
            // Title
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Preview Data Provider for different screen scenarios
class QuickActionsPreviewProvider : PreviewParameterProvider<List<QuickAction>> {
    override val values = sequenceOf(
        // Standard 3 actions
        listOf(
            QuickAction("Presupuestos", "💰") { },
            QuickAction("Metas", "🎯") { },
            QuickAction("Reportes", "📊") { }
        ),
        // Extended 4 actions for tablet
        listOf(
            QuickAction("Presupuestos", "💰") { },
            QuickAction("Metas", "🎯") { },
            QuickAction("Reportes", "📊") { },
            QuickAction("Categorías", "🏷️") { }
        ),
        // Extended 6 actions for large tablets
        listOf(
            QuickAction("Presupuestos", "💰") { },
            QuickAction("Metas", "🎯") { },
            QuickAction("Reportes", "📊") { },
            QuickAction("Categorías", "🏷️") { },
            QuickAction("Configuración", "⚙️") { },
            QuickAction("Ayuda", "❓") { }
        )
    )
}

@Preview(
    name = "Compact Phone - 3 Columns",
    widthDp = 360,
    heightDp = 640
)
@Composable
private fun QuickActionsGridCompactPreview() {
    FinanceTheme {
        Surface {
            QuickActionsGrid(
                actions = listOf(
                    QuickAction("Presupuestos", "💰") { },
                    QuickAction("Metas", "🎯") { },
                    QuickAction("Reportes", "📊") { }
                ),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(
    name = "Medium Tablet - 4 Columns",
    widthDp = 720,
    heightDp = 1024
)
@Composable
private fun QuickActionsGridMediumPreview() {
    FinanceTheme {
        Surface {
            QuickActionsGrid(
                actions = listOf(
                    QuickAction("Presupuestos", "💰") { },
                    QuickAction("Metas", "🎯") { },
                    QuickAction("Reportes", "📊") { },
                    QuickAction("Categorías", "🏷️") { }
                ),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(
    name = "Large Tablet - 4 Columns Extended",
    widthDp = 1024,
    heightDp = 768
)
@Composable
private fun QuickActionsGridExpandedPreview() {
    FinanceTheme {
        Surface {
            QuickActionsGrid(
                actions = listOf(
                    QuickAction("Presupuestos", "💰") { },
                    QuickAction("Metas", "🎯") { },
                    QuickAction("Reportes", "📊") { },
                    QuickAction("Categorías", "🏷️") { },
                    QuickAction("Configuración", "⚙️") { },
                    QuickAction("Ayuda", "❓") { }
                ),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(
    name = "Dark Theme - Compact",
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
                    QuickAction("Presupuestos", "💰") { },
                    QuickAction("Metas", "🎯") { },
                    QuickAction("Reportes", "📊") { }
                ),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}