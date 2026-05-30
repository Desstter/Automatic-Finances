package com.example.automaticfinances.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.automaticfinances.navigation.Routes

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
            route = Routes.INCOME_MANAGEMENT,
            icon = Icons.Filled.AccountBalanceWallet,
            label = "Ingresos"
        ),
        BottomNavItem(
            route = Routes.FINANCIAL_DASHBOARD,
            icon = Icons.Filled.Analytics,
            label = "Reportes"
        ),
        BottomNavItem(
            route = Routes.CATEGORY_MANAGEMENT,
            icon = Icons.Filled.Category,
            label = "Categorías"
        )
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
        bottomBar = {
            val isDark = isSystemInDarkTheme()
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets.navigationBars,
                modifier = Modifier.border(
                    width = Dp.Hairline,
                    color = if (isDark) MaterialTheme.colorScheme.outlineVariant else Color.Transparent,
                    shape = RectangleShape
                )
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
    ) { paddingValues ->
        content(paddingValues)
    }
}