package com.example.automaticfinances.ui.unparsed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.automaticfinances.data.db.UnparsedSms
import com.example.automaticfinances.ui.components.common.SectionCard
import com.example.automaticfinances.ui.theme.Sizes
import com.example.automaticfinances.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Log of bank messages the parser missed (OBS-1). The user can copy them out, clear the list, or
 * **rescue** one into a real transaction via [onRegister] (which pre-fills the manual add flow).
 * Nothing here touches balances until the user confirms the rescued transaction.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnparsedSmsScreen(
    onNavigateBack: () -> Unit,
    onRegister: (UnparsedSms) -> Unit,
    viewModel: UnparsedSmsViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val clipboard = LocalClipboardManager.current
    var showClearConfirm by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Mensajes no reconocidos",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (items.isNotEmpty()) {
                        IconButton(onClick = { showClearConfirm = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Borrar todo")
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        if (items.isEmpty()) {
            EmptyState(modifier = Modifier.fillMaxSize().padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(
                    start = Spacing.screen,
                    end = Spacing.screen,
                    top = Spacing.md,
                    bottom = Spacing.xxxl,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                item {
                    Text(
                        "Estos mensajes parecían transacciones pero no se pudieron registrar. " +
                            "Revísalos para mejorar la detección; no afectan tus saldos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = Spacing.xs),
                    )
                }
                items(items, key = { it.id }) { sms ->
                    UnparsedSmsCard(
                        sms = sms,
                        onCopy = { clipboard.setText(AnnotatedString(sms.text)) },
                        onDelete = { viewModel.delete(sms.id) },
                        onRegister = { onRegister(sms) },
                    )
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
            title = { Text("Borrar todos los mensajes") },
            text = { Text("Se eliminará la lista de mensajes no reconocidos. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    viewModel.clearAll()
                }) { Text("Borrar todo") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun UnparsedSmsCard(
    sms: UnparsedSms,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onRegister: () -> Unit,
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sourceLabel(sms.source),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = formatDate(sms.receivedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onCopy) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Copiar",
                    modifier = Modifier.size(Sizes.iconSm),
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Eliminar",
                    modifier = Modifier.size(Sizes.iconSm),
                )
            }
        }
        Spacer(Modifier.size(Spacing.sm))
        Text(
            text = sms.text,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.size(Spacing.sm))
        Button(
            onClick = onRegister,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                Icons.Default.AddCircleOutline,
                contentDescription = null,
                modifier = Modifier.size(Sizes.iconSm),
            )
            Spacer(Modifier.size(Spacing.sm))
            Text("Registrar como transacción")
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(Spacing.screen), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Icon(
                Icons.Default.Inbox,
                contentDescription = null,
                modifier = Modifier.size(Sizes.avatarLg),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "No hay mensajes sin reconocer",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
            Text(
                "Cuando llegue un SMS bancario que la app no logre registrar, aparecerá aquí para " +
                    "que puedas revisarlo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun sourceLabel(source: String): String = when {
    source == "sms" -> "SMS"
    source.startsWith("notif:") -> "Notificación · " + source.removePrefix("notif:")
    else -> source
}

private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "CO")).apply {
    timeZone = TimeZone.getTimeZone("America/Bogota")
}

private fun formatDate(epochMillis: Long): String = dateFormat.format(Date(epochMillis))
