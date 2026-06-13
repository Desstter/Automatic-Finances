package com.example.automaticfinances.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.automaticfinances.data.models.AdvisorUiState
import com.example.automaticfinances.data.models.AiFinancialInsights
import com.example.automaticfinances.data.models.AiTip
import com.example.automaticfinances.data.models.AiTone
import com.example.automaticfinances.data.remote.LlmFailure
import com.example.automaticfinances.ui.theme.FinanceTheme

/**
 * Renders the AI advisor section on the dashboard from a single [AdvisorUiState]:
 * - [Hidden][AdvisorUiState.Hidden]  → nothing (advisor off / nothing to analyze).
 * - [Loading][AdvisorUiState.Loading] → a subtle spinner card while the model responds.
 * - [Success][AdvisorUiState.Success] → the narrative + tips, with a refresh affordance.
 * - [Error][AdvisorUiState.Error]    → a short reason plus the right recovery (fix key / retry).
 */
@Composable
fun AiAdvisorCard(
    state: AdvisorUiState,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        AdvisorUiState.Hidden -> Unit
        AdvisorUiState.Loading -> AiAdvisorLoadingCard(modifier)
        is AdvisorUiState.Success -> AiAdvisorContentCard(state.insights, onRefresh, modifier)
        is AdvisorUiState.Error -> AiAdvisorErrorCard(state.failure, onRetry, onOpenSettings, modifier)
    }
}

@Composable
private fun AiAdvisorContentCard(
    insights: AiFinancialInsights,
    onRefresh: () -> Unit,
    modifier: Modifier,
) {
    AdvisorCardShell(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = "Asesor financiero",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Actualizar consejo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        if (insights.summary.isNotBlank()) {
            Text(
                text = insights.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        insights.tips.forEach { tip -> TipRow(tip) }
    }
}

@Composable
private fun TipRow(tip: AiTip) {
    val (icon, color) = tip.tone.style()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            color = color.copy(alpha = 0.15f),
            shape = CircleShape,
            modifier = Modifier.size(28.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            if (tip.title.isNotBlank()) {
                Text(
                    text = tip.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (tip.body.isNotBlank()) {
                Text(
                    text = tip.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AiAdvisorLoadingCard(modifier: Modifier) {
    AdvisorCardShell(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Analizando tus finanzas…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun AiAdvisorErrorCard(
    failure: LlmFailure,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier,
) {
    val message = failure.userMessage()
    // A bad / missing key is the only thing the user fixes in Settings; everything else is retryable.
    val needsKey = failure == LlmFailure.MISSING_KEY || failure == LlmFailure.AUTH
    AdvisorCardShell(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                color = FinanceTheme.colors.warning.copy(alpha = 0.15f),
                shape = CircleShape,
                modifier = Modifier.size(28.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = FinanceTheme.colors.warning,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Asesor financiero",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        FilledTonalButton(
            onClick = if (needsKey) onOpenSettings else onRetry,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (needsKey) "Ir a Ajustes" else "Reintentar")
        }
    }
}

/** Shared card chrome so loading / success / error states stay visually identical. */
@Composable
private fun AdvisorCardShell(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()
        }
    }
}

private fun LlmFailure.userMessage(): String = when (this) {
    LlmFailure.MISSING_KEY -> "Configura tu API key en Ajustes para activar el asesor."
    LlmFailure.AUTH -> "Tu API key no es válida. Revísala en Ajustes."
    LlmFailure.QUOTA -> "Se agotó el cupo del modelo. Intenta más tarde."
    LlmFailure.NETWORK -> "No se pudo conectar. Revisa tu conexión."
    LlmFailure.SERVER -> "El servicio no está disponible ahora mismo."
    LlmFailure.BLOCKED -> "El contenido fue bloqueado por el filtro del modelo."
    LlmFailure.EMPTY -> "El modelo no devolvió un análisis. Intenta de nuevo."
    LlmFailure.UNKNOWN -> "No se pudo generar el análisis. Intenta de nuevo."
}

@Composable
private fun AiTone.style(): Pair<ImageVector, Color> {
    val finance = FinanceTheme.colors
    return when (this) {
        AiTone.POSITIVE -> Icons.Default.CheckCircle to finance.profit
        AiTone.WARNING -> Icons.Default.WarningAmber to finance.warning
        AiTone.INFO -> Icons.Default.Lightbulb to finance.info
    }
}
