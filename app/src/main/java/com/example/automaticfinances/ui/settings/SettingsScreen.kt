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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.automaticfinances.data.preferences.AccentColor
import com.example.automaticfinances.data.preferences.ThemeMode
import com.example.automaticfinances.ui.components.common.SectionCard
import com.example.automaticfinances.ui.components.common.SectionHeader
import com.example.automaticfinances.ui.theme.Sizes
import com.example.automaticfinances.ui.theme.Spacing
import com.example.automaticfinances.ui.theme.ThemeViewModel
import com.example.automaticfinances.ui.theme.label
import com.example.automaticfinances.ui.theme.swatch
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
    val accentColor by themeViewModel.accentColor.collectAsStateWithLifecycle()
    val storedName by themeViewModel.userName.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val context = androidx.compose.ui.platform.LocalContext.current
    val systemHealth by com.example.automaticfinances.system.SystemConfigurationChecker
        .rememberSystemHealth(context)
    val detectionActive = systemHealth.isListenerEnabled
    val smsCaptureActive = systemHealth.isSmsCaptureEnabled

    val backupState by backupViewModel.state.collectAsStateWithLifecycle()
    val reviewCount by reviewViewModel.count.collectAsStateWithLifecycle()
    val digestEnabled by insightsViewModel.digestEnabled.collectAsStateWithLifecycle()
    val aiAdvisorEnabled by insightsViewModel.aiAdvisorEnabled.collectAsStateWithLifecycle()
    val aiApiKey by insightsViewModel.aiApiKey.collectAsStateWithLifecycle()
    val aiModel by insightsViewModel.aiModel.collectAsStateWithLifecycle()
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
                SectionHeader(title = "Personalización")
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SettingIconBadge(Icons.Default.Person)
                        Spacer(Modifier.size(Spacing.md))
                        Text(
                            "Tu nombre",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Spacer(Modifier.size(Spacing.md))
                    NameField(
                        storedName = storedName,
                        onNameChange = themeViewModel::setUserName,
                    )
                    Spacer(Modifier.size(Spacing.lg))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SettingIconBadge(Icons.Default.ColorLens)
                        Spacer(Modifier.size(Spacing.md))
                        Text(
                            "Color de acento",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Spacer(Modifier.size(Spacing.md))
                    AccentColorSelector(
                        selected = accentColor,
                        onSelect = themeViewModel::setAccentColor,
                    )
                }
            }

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
                AiSettingsSection(
                    enabled = aiAdvisorEnabled,
                    apiKey = aiApiKey,
                    model = aiModel,
                    onEnabledChange = insightsViewModel::setAiAdvisorEnabled,
                    onApiKeyChange = insightsViewModel::setAiApiKey,
                    onModelChange = insightsViewModel::setAiModel,
                )
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

/**
 * AI advisor settings: enable toggle + the DeepSeek key/model fields. The model id drives both the
 * advisor and voice parsing; if no key is set the app still works via the Gemini build-time fallback.
 */
@Composable
private fun AiSettingsSection(
    enabled: Boolean,
    apiKey: String,
    model: String,
    onEnabledChange: (Boolean) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
) {
    SectionHeader(title = "Inteligencia artificial")
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SettingIconBadge(Icons.Default.Psychology)
            Spacer(Modifier.size(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Asesor financiero IA",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    "Análisis y consejos con DeepSeek en tus reportes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.size(Spacing.sm))
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
            )
        }
        Spacer(Modifier.size(Spacing.lg))
        Text(
            "Clave de API de DeepSeek",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.size(Spacing.sm))
        ApiKeyField(
            storedKey = apiKey,
            onKeyChange = onApiKeyChange,
        )
        Spacer(Modifier.size(Spacing.lg))
        Text(
            "Modelo",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.size(Spacing.sm))
        ModelField(
            storedModel = model,
            onModelChange = onModelChange,
        )
        Spacer(Modifier.size(Spacing.sm))
        Text(
            "Tu clave se guarda solo en este dispositivo. La voz y el asesor usan este modelo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NameField(
    storedName: String,
    onNameChange: (String) -> Unit,
) {
    // Seed the local field once from the persisted value, then treat local state as the source of
    // truth so per-keystroke persistence never fights what the user is typing.
    var initialized by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    LaunchedEffect(storedName) {
        if (!initialized) {
            text = storedName
            initialized = true
        }
    }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onNameChange(it)
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text("¿Cómo te saludamos?") },
    )
}

@Composable
private fun ApiKeyField(
    storedKey: String,
    onKeyChange: (String) -> Unit,
) {
    // Same seed-once pattern as NameField so per-keystroke persistence never fights the user.
    var initialized by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(storedKey) {
        if (!initialized) {
            text = storedKey
            initialized = true
        }
    }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onKeyChange(it)
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text("sk-...") },
        visualTransformation = if (revealed) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { revealed = !revealed }) {
                Icon(
                    imageVector = if (revealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (revealed) "Ocultar clave" else "Mostrar clave",
                )
            }
        },
    )
}

@Composable
private fun ModelField(
    storedModel: String,
    onModelChange: (String) -> Unit,
) {
    var initialized by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    LaunchedEffect(storedModel) {
        if (!initialized) {
            text = storedModel
            initialized = true
        }
    }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onModelChange(it)
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text("deepseek-chat") },
    )
}

@Composable
private fun AccentColorSelector(
    selected: AccentColor,
    onSelect: (AccentColor) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        AccentColor.entries.forEach { accent ->
            val isSelected = accent == selected
            Box(
                modifier = Modifier
                    .size(Sizes.avatarSm)
                    .clip(CircleShape)
                    .background(accent.swatch())
                    .then(
                        if (isSelected) Modifier.border(
                            width = 3.dp,
                            color = MaterialTheme.colorScheme.onSurface,
                            shape = CircleShape,
                        ) else Modifier
                    )
                    .clickable { onSelect(accent) }
                    .semantics { contentDescription = accent.label() },
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(Sizes.iconSm),
                    )
                }
            }
        }
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
