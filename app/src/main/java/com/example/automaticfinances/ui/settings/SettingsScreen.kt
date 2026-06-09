package com.example.automaticfinances.ui.settings

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.RuleFolder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.automaticfinances.data.preferences.ThemeMode
import com.example.automaticfinances.ui.components.common.SectionCard
import com.example.automaticfinances.ui.components.common.SectionHeader
import com.example.automaticfinances.ui.theme.Sizes
import com.example.automaticfinances.ui.theme.Spacing
import com.example.automaticfinances.ui.theme.ThemeViewModel
import kotlinx.coroutines.launch

/**
 * Settings hub — the single home for everything that used to be scattered across the
 * top bar and the bottom nav (theme, categories, accounts/balances, incomes, automatic
 * detection). Reachable as a bottom-nav root, so it shows no back arrow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeViewModel: ThemeViewModel,
    onOpenNotifAccess: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToCategoryRules: () -> Unit,
    onNavigateToIncomes: () -> Unit,
    onNavigateToBalances: () -> Unit,
    onNavigateToUnparsed: () -> Unit,
    onNavigateToReview: () -> Unit,
    backupViewModel: BackupViewModel = hiltViewModel(),
    reviewViewModel: com.example.automaticfinances.ui.review.ReviewViewModel = hiltViewModel(),
    insightsViewModel: InsightsSettingsViewModel = hiltViewModel(),
) {
    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val context = androidx.compose.ui.platform.LocalContext.current
    val systemHealth by com.example.automaticfinances.system.SystemConfigurationChecker
        .rememberSystemHealth(context)
    val detectionActive = systemHealth.isListenerEnabled
    val smsCaptureActive = systemHealth.isSmsCaptureEnabled

    val backupState by backupViewModel.state.collectAsStateWithLifecycle()
    val reviewCount by reviewViewModel.count.collectAsStateWithLifecycle()
    val digestEnabled by insightsViewModel.digestEnabled.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var showRestoreConfirm by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri -> uri?.let(backupViewModel::export) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(backupViewModel::import) }

    LaunchedEffect(backupState) {
        when (val s = backupState) {
            is BackupUiState.ExportDone -> {
                snackbarHostState.showSnackbar("Copia de seguridad creada")
                backupViewModel.dismiss()
            }
            is BackupUiState.Error -> {
                snackbarHostState.showSnackbar(s.message)
                backupViewModel.dismiss()
            }
            else -> Unit
        }
    }

    androidx.compose.material3.Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Ajustes",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = Spacing.screen,
                end = Spacing.screen,
                top = Spacing.md,
                bottom = Spacing.xxxl,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            item {
                SectionHeader(title = "Apariencia")
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SettingIconBadge(Icons.Default.Palette)
                        Spacer(Modifier.size(Spacing.md))
                        Text(
                            "Tema de la aplicación",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Spacer(Modifier.size(Spacing.md))
                    ThemeModeSelector(
                        selected = themeMode,
                        onSelect = themeViewModel::setThemeMode,
                    )
                }
            }

            item {
                SectionHeader(title = "Gestión")
                SectionCard(contentPadding = Spacing.none) {
                    SettingRow(
                        icon = Icons.Default.AccountBalanceWallet,
                        title = "Cuentas y saldos",
                        subtitle = "Saldos de apertura de banco y efectivo",
                        onClick = onNavigateToBalances,
                    )
                    SettingDivider()
                    SettingRow(
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        title = "Ingresos",
                        subtitle = "Historial y resumen de ingresos",
                        onClick = onNavigateToIncomes,
                    )
                    SettingDivider()
                    SettingRow(
                        icon = Icons.Default.Category,
                        title = "Categorías",
                        subtitle = "Organiza y clasifica tus transacciones",
                        onClick = onNavigateToCategories,
                    )
                    SettingDivider()
                    SettingRow(
                        icon = Icons.Default.RuleFolder,
                        title = "Reglas de categoría",
                        subtitle = "Palabras clave que clasifican tus movimientos automáticamente",
                        onClick = onNavigateToCategoryRules,
                    )
                }
            }

            item {
                SectionHeader(title = "Detección automática")
                SectionCard(contentPadding = Spacing.none) {
                    SettingRow(
                        icon = Icons.Default.Notifications,
                        title = "Acceso a notificaciones",
                        subtitle = if (detectionActive)
                            "Activa · registrando gastos desde las alertas del banco"
                        else
                            "Inactiva · toca para otorgar el permiso",
                        onClick = onOpenNotifAccess,
                    )
                    SettingDivider()
                    SettingRow(
                        icon = Icons.AutoMirrored.Filled.Message,
                        title = "Lectura de SMS del banco",
                        subtitle = if (smsCaptureActive)
                            "Activa · la vía más confiable para bancos que envían SMS"
                        else
                            "Inactiva · sin este permiso se pierden las compras por SMS. Toca para activarlo",
                        onClick = { com.example.automaticfinances.system.ServiceManager.openAppDetailsSettings(context) },
                    )
                    SettingDivider()
                    SettingRow(
                        icon = Icons.Default.RuleFolder,
                        title = if (reviewCount > 0) "Por revisar ($reviewCount)" else "Por revisar",
                        subtitle = "Confirma o descarta capturas de baja confianza",
                        onClick = onNavigateToReview,
                    )
                    SettingDivider()
                    SettingRow(
                        icon = Icons.AutoMirrored.Filled.Message,
                        title = "Mensajes no reconocidos",
                        subtitle = "Revisa los SMS bancarios que no se pudieron registrar",
                        onClick = onNavigateToUnparsed,
                    )
                }
            }

            item {
                SectionHeader(title = "Resumen e insights")
                SectionCard(contentPadding = Spacing.none) {
                    SettingSwitchRow(
                        icon = Icons.Default.Insights,
                        title = "Resumen semanal",
                        subtitle = "Proyección de fin de mes, suscripciones y alertas de cargos",
                        checked = digestEnabled,
                        onCheckedChange = insightsViewModel::setDigestEnabled,
                    )
                    if (digestEnabled) {
                        SettingDivider()
                        SettingRow(
                            icon = Icons.Default.NotificationsActive,
                            title = "Ver resumen ahora",
                            subtitle = "Genera y envía el resumen como notificación",
                            onClick = {
                                insightsViewModel.runDigestNow()
                                scope.launch {
                                    snackbarHostState.showSnackbar("Generando tu resumen…")
                                }
                            },
                        )
                    }
                }
            }

            item {
                SectionHeader(title = "Copia de seguridad")
                SectionCard(contentPadding = Spacing.none) {
                    SettingRow(
                        icon = Icons.Default.Backup,
                        title = "Crear copia de seguridad",
                        subtitle = "Guarda todos tus datos en un archivo",
                        onClick = { exportLauncher.launch(backupViewModel.suggestedFileName()) },
                    )
                    SettingDivider()
                    SettingRow(
                        icon = Icons.Default.Restore,
                        title = "Restaurar copia",
                        subtitle = "Reemplaza tus datos con los de un archivo",
                        onClick = { showRestoreConfirm = true },
                    )
                }
            }
        }
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            icon = { Icon(Icons.Default.Restore, contentDescription = null) },
            title = { Text("Restaurar copia de seguridad") },
            text = {
                Text(
                    "Esto reemplazará todos los datos actuales de la app con los del archivo " +
                        "que elijas. Esta acción no se puede deshacer.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreConfirm = false
                    importLauncher.launch(arrayOf("*/*"))
                }) { Text("Elegir archivo") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) { Text("Cancelar") }
            },
        )
    }

    if (backupState is BackupUiState.Working) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            icon = { CircularProgressIndicator(modifier = Modifier.size(Sizes.iconMd)) },
            title = { Text("Procesando…") },
            text = { Text("Por favor espera un momento.") },
        )
    }

    if (backupState is BackupUiState.RestoreDone) {
        AlertDialog(
            onDismissRequest = {},
            icon = { Icon(Icons.Default.Restore, contentDescription = null) },
            title = { Text("Restauración completa") },
            text = { Text("La aplicación se reiniciará para aplicar los datos restaurados.") },
            confirmButton = {
                TextButton(onClick = { backupViewModel.restartApp() }) { Text("Reiniciar ahora") }
            },
        )
    }
}

@Composable
private fun ThemeModeSelector(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    val options = listOf(
        Triple(ThemeMode.LIGHT, "Claro", Icons.Default.BrightnessHigh),
        Triple(ThemeMode.DARK, "Oscuro", Icons.Default.Brightness3),
        Triple(ThemeMode.AUTO, "Auto", Icons.Default.BrightnessAuto),
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (mode, label, icon) ->
            SegmentedButton(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                icon = {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(Sizes.iconSm))
                },
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun SettingIconBadge(icon: ImageVector) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(Sizes.avatarSm),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(Sizes.iconMd),
            )
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = androidx.compose.ui.graphics.Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.card),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingIconBadge(icon)
            Spacer(Modifier.size(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.size(Spacing.sm))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.card),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingIconBadge(icon)
        Spacer(Modifier.size(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.size(Spacing.sm))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingDivider() {
    androidx.compose.material3.HorizontalDivider(
        modifier = Modifier.padding(start = Spacing.card + Sizes.avatarSm + Spacing.md),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}
