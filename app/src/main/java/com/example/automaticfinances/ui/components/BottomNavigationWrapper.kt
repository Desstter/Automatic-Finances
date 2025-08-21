package com.example.automaticfinances.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.automaticfinances.navigation.Routes

data class BottomNavItem(
    val route: String,
    val icon: String,
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
            icon = "🏠",
            label = "Inicio"
        ),
        BottomNavItem(
            route = Routes.INCOME_MANAGEMENT,
            icon = "💰",
            label = "Ingresos"
        ),
        BottomNavItem(
            route = Routes.FINANCIAL_DASHBOARD,
            icon = "📊",
            label = "Reportes"
        ),
        BottomNavItem(
            route = Routes.CATEGORY_MANAGEMENT,
            icon = "🏷️",
            label = "Categorías"
        )
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                bottomNavItems.forEach { item ->
                    val isSelected = currentRoute == item.route
                    
                    NavigationBarItem(
                        icon = {
                            Text(
                                text = item.icon,
                                style = MaterialTheme.typography.titleMedium
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