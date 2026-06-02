package com.example.automaticfinances.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.automaticfinances.system.ServiceManager
import com.example.automaticfinances.ui.components.common.SectionCard
import com.example.automaticfinances.ui.components.common.StatusPill
import com.example.automaticfinances.ui.components.common.StatusTone
import com.example.automaticfinances.ui.theme.Sizes
import com.example.automaticfinances.ui.theme.Spacing

/** Live snapshot of the permissions the auto-detection feature depends on. */
data class NotificationPermissionsState(
    val listenerEnabled: Boolean,
    val postNotificationsRequired: Boolean,
    val postNotificationsGranted: Boolean,
    val smsReceiveGranted: Boolean,
    val batteryUnrestricted: Boolean,
)

/**
 * Reads the current notification-permission state and re-reads it every time the activity resumes.
 * That ON_RESUME refresh is what makes the onboarding feel live: the user leaves to grant access in
 * system Settings and the screen reflects the new state the instant they come back.
 */
@Composable
fun rememberNotificationPermissionsState(): NotificationPermissionsState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val required = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    var listenerEnabled by remember {
        mutableStateOf(ServiceManager.isNotificationListenerEnabled(context))
    }
    var postNotifGranted by remember { mutableStateOf(hasPostNotificationsPermission(context)) }
    var smsGranted by remember { mutableStateOf(hasSmsPermission(context)) }
    var batteryUnrestricted by remember {
        mutableStateOf(ServiceManager.isIgnoringBatteryOptimizations(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                listenerEnabled = ServiceManager.isNotificationListenerEnabled(context)
                postNotifGranted = hasPostNotificationsPermission(context)
                smsGranted = hasSmsPermission(context)
                batteryUnrestricted = ServiceManager.isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return NotificationPermissionsState(
        listenerEnabled = listenerEnabled,
        postNotificationsRequired = required,
        postNotificationsGranted = postNotifGranted,
        smsReceiveGranted = smsGranted,
        batteryUnrestricted = batteryUnrestricted,
    )
}

private fun hasPostNotificationsPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        context, Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
}

private fun hasSmsPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context, Manifest.permission.RECEIVE_SMS,
    ) == PackageManager.PERMISSION_GRANTED
}

/**
 * First-run onboarding centered on the one thing the app cannot work without: notification access.
 * It explains the value, walks the user through granting the listener permission (and, on Android
 * 13+, the runtime POST_NOTIFICATIONS permission), and reflects each grant live. Granting is never
 * forced — the user can continue and still use manual/voice entry.
 */
@Composable
fun OnboardingScreen(
    state: NotificationPermissionsState,
    onGrantNotificationAccess: () -> Unit,
    onRequestPostNotifications: () -> Unit,
    onRequestSmsAccess: () -> Unit,
    onRequestBatteryExemption: () -> Unit,
    onFinish: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = Spacing.screen,
                        end = Spacing.screen,
                        top = Spacing.huge,
                        bottom = Spacing.lg,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(88.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Savings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.xl))
                Text(
                    text = "Tus gastos, registrados solos",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = "AutomaticFinances lee las alertas de tu banco y crea cada transacción por ti. " +
                        "Para lograrlo necesita un permiso.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(Spacing.xxl))

                PermissionStepCard(
                    icon = Icons.Filled.Sms,
                    title = "Leer SMS del banco",
                    description = "La forma más confiable de registrar tus compras: la app lee el SMS " +
                        "de Bancolombia directamente, aunque el teléfono restrinja la app en segundo plano.",
                    granted = state.smsReceiveGranted,
                    actionLabel = "Permitir",
                    onAction = onRequestSmsAccess,
                )

                Spacer(Modifier.height(Spacing.md))

                PermissionStepCard(
                    icon = Icons.Filled.Notifications,
                    title = "Acceso a notificaciones",
                    description = "Para bancos que avisan por notificación de su app (Nequi, DaviPlata) " +
                        "en vez de SMS. Complementa la lectura de SMS.",
                    granted = state.listenerEnabled,
                    actionLabel = "Otorgar acceso",
                    onAction = onGrantNotificationAccess,
                )

                Spacer(Modifier.height(Spacing.md))

                PermissionStepCard(
                    icon = Icons.Filled.BatteryAlert,
                    title = "Sin restricción de batería",
                    description = "Evita que el sistema cierre la app y deje de captar tus movimientos. " +
                        "Imprescindible en Xiaomi/Redmi y similares.",
                    granted = state.batteryUnrestricted,
                    actionLabel = "Permitir",
                    onAction = onRequestBatteryExemption,
                )

                if (state.postNotificationsRequired) {
                    Spacer(Modifier.height(Spacing.md))
                    PermissionStepCard(
                        icon = Icons.Filled.NotificationsActive,
                        title = "Mostrar notificaciones",
                        description = "Para confirmarte cada gasto detectado y avisarte sobre tus presupuestos.",
                        granted = state.postNotificationsGranted,
                        actionLabel = "Permitir",
                        onAction = onRequestPostNotifications,
                    )
                }

                Spacer(Modifier.height(Spacing.xl))

                FeatureHint(
                    icon = Icons.Filled.GraphicEq,
                    text = "¿Pagaste en efectivo? También puedes dictar o escribir tus gastos a mano.",
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.screen, vertical = Spacing.lg),
                ) {
                    Button(
                        onClick = onFinish,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Sizes.minTouchTarget),
                    ) {
                        Text(
                            text = if (state.smsReceiveGranted || state.listenerEnabled) "Comenzar" else "Continuar de todos modos",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionStepCard(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(Sizes.avatarMd),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(Sizes.iconLg),
                    )
                }
            }
            Spacer(Modifier.size(Spacing.md))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            StatusPill(
                label = if (granted) "Activo" else "Pendiente",
                tone = if (granted) StatusTone.Positive else StatusTone.Warning,
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.md))
        if (granted) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Sizes.iconMd),
                )
                Spacer(Modifier.size(Spacing.sm))
                Text(
                    text = "Permiso concedido",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            FilledTonalButton(onClick = onAction, modifier = Modifier.fillMaxWidth()) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun FeatureHint(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Sizes.iconMd),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
