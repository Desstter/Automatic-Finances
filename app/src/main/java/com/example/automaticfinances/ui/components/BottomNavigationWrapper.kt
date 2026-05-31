package com.example.automaticfinances.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.automaticfinances.navigation.Routes
import com.example.automaticfinances.ui.theme.MotionTokens

data class BottomNavItem(
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
)

@Composable
fun BottomNavigationWrapper(
    navController: NavController,
    content: @Composable (PaddingValues) -> Unit
) {
    val bottomNavItems = listOf(
        BottomNavItem(
            route = Routes.HOME,
            icon = Icons.Filled.Home,
            label = "Inicio"
        ),
        BottomNavItem(
            route = Routes.TRANSACTION_HISTORY,
            icon = Icons.AutoMirrored.Filled.ReceiptLong,
            label = "Movimientos"
        ),
        BottomNavItem(
            route = Routes.FINANCIAL_DASHBOARD,
            icon = Icons.Filled.Analytics,
            label = "Análisis"
        ),
        BottomNavItem(
            route = Routes.SETTINGS,
            icon = Icons.Filled.Settings,
            label = "Ajustes"
        )
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // The bottom bar only belongs on the four top-level roots. Detail/modal screens
    // (add, detail, budget, etc.) get the full screen so the bar doesn't read as a
    // dead-end control there.
    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
        bottomBar = {
            // Expressive: lean on a tonal container for separation instead of a manual
            // hairline border. surfaceContainer reads as a distinct layer in light and dark.
            AnimatedVisibility(
                visible = showBottomBar,
                enter = fadeIn(MotionTokens.expressiveEffectsDefault()) +
                    expandVertically(MotionTokens.expressiveSpatialDefault()),
                exit = fadeOut(MotionTokens.expressiveEffectsDefault()) +
                    shrinkVertically(MotionTokens.expressiveSpatialDefault()),
            ) {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                windowInsets = WindowInsets.navigationBars
            ) {
                bottomNavItems.forEach { item ->
                    val isSelected = currentRoute == item.route
                    
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        selected = isSelected,
                        onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    // Pop up to the start destination to avoid building up a large stack
                                    popUpTo(Routes.HOME) {
                                        saveState = true
                                    }
                                    // Avoid multiple copies of the same destination when reselecting
                                    launchSingleTop = true
                                    // Restore state when reselecting a previously selected item
                                    restoreState = true
                                }
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
            }
        }
    ) { paddingValues ->
        content(paddingValues)
    }
}