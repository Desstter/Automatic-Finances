package com.example.automaticfinances.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.automaticfinances.ui.HomeScreen
import com.example.automaticfinances.ui.HomeViewModel
import com.example.automaticfinances.ui.categories.CategoryManagementScreen
import com.example.automaticfinances.ui.transaction.TransactionDetailScreen
import com.example.automaticfinances.ui.transaction.AddTransactionScreen
import com.example.automaticfinances.ui.transaction.TransactionHistoryScreen
import com.example.automaticfinances.ui.insights.FinancialDashboardScreen
import com.example.automaticfinances.ui.insights.FinancialDashboardViewModel
import com.example.automaticfinances.data.repo.BudgetRepository
import com.example.automaticfinances.data.repo.TransactionRepository
import com.example.automaticfinances.data.db.AppDatabase
import androidx.lifecycle.viewmodel.compose.viewModel

// Definición de rutas
object Routes {
    const val HOME = "home"
    const val TRANSACTION_DETAIL = "transaction_detail/{transactionId}"
    const val CATEGORY_MANAGEMENT = "category_management"
    const val ADD_TRANSACTION = "add_transaction"
    const val TRANSACTION_HISTORY = "transaction_history"
    const val FINANCIAL_DASHBOARD = "financial_dashboard"
    const val BUDGET_MANAGEMENT = "budget_management"
    
    fun transactionDetail(transactionId: String) = "transaction_detail/$transactionId"
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    onOpenNotifAccess: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) { backStackEntry ->
            val homeViewModel: HomeViewModel = viewModel()
            
            // Refrescar datos cuando se vuelve a Home desde otra pantalla
            LaunchedEffect(backStackEntry) {
                homeViewModel.forceRefresh()
            }
            
            HomeScreen(
                stateFlow = homeViewModel.state,
                onOpenNotifAccess = onOpenNotifAccess,
                onTransactionClick = { transactionId ->
                    navController.navigate(Routes.transactionDetail(transactionId))
                },
                onManageCategoriesClick = {
                    navController.navigate(Routes.CATEGORY_MANAGEMENT)
                },
                onAddTransactionClick = {
                    navController.navigate(Routes.ADD_TRANSACTION)
                },
                onViewHistoryClick = {
                    navController.navigate(Routes.TRANSACTION_HISTORY)
                },
                onViewInsightsClick = {
                    navController.navigate(Routes.FINANCIAL_DASHBOARD)
                },
                onRefresh = {
                    homeViewModel.refreshData()
                },
                onSearchQueryChange = { query ->
                    homeViewModel.updateSearchQuery(query)
                },
                onToggleFilters = {
                    homeViewModel.toggleFilters()
                },
                onClearFilters = {
                    homeViewModel.clearAllFilters()
                },
                onDateFilterChange = { startDate, endDate ->
                    homeViewModel.setDateFilter(startDate, endDate)
                },
                onAmountFilterChange = { minAmount, maxAmount ->
                    homeViewModel.setAmountFilter(minAmount, maxAmount)
                },
                onCategoryFilterChange = { categoryId ->
                    homeViewModel.filterByCategory(categoryId)
                }
            )
        }
        
        composable(Routes.TRANSACTION_DETAIL) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId") ?: ""
            TransactionDetailScreen(
                transactionId = transactionId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Routes.CATEGORY_MANAGEMENT) {
            CategoryManagementScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.ADD_TRANSACTION) {
            AddTransactionScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.TRANSACTION_HISTORY) {
            TransactionHistoryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Routes.FINANCIAL_DASHBOARD) {
            val budgetRepository = BudgetRepository(
                budgetDao = AppDatabase.get().budgetDao(),
                transactionDao = AppDatabase.get().transactionDao(),
                categoryDao = AppDatabase.get().categoryDao()
            )
            val transactionRepository = TransactionRepository()
            
            val dashboardViewModel: FinancialDashboardViewModel = viewModel {
                FinancialDashboardViewModel(budgetRepository, transactionRepository)
            }
            
            FinancialDashboardScreen(
                viewModel = dashboardViewModel,
                onNavigateToBudgetManagement = {
                    navController.navigate(Routes.BUDGET_MANAGEMENT)
                },
                onNavigateToGoals = {
                    // Navigate to goals screen (placeholder for 20/80 rule)
                    navController.popBackStack()
                },
                onNavigateToReports = {
                    // Navigate to reports screen (placeholder for 20/80 rule)
                    navController.popBackStack()
                }
            )
        }
        
        composable(Routes.BUDGET_MANAGEMENT) {
            // Placeholder for BudgetManagementScreen (20/80 rule - focus on core dashboard first)
            // For now, navigate back to show we're working with 80% of value
            LaunchedEffect(Unit) {
                navController.popBackStack()
            }
        }
    }
}