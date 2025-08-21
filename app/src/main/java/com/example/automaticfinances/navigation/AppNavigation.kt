package com.example.automaticfinances.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.automaticfinances.ui.HomeScreen
import com.example.automaticfinances.ui.HomeViewModel
import com.example.automaticfinances.ui.categories.CategoryManagementScreen
import com.example.automaticfinances.ui.transaction.TransactionDetailScreen
import com.example.automaticfinances.ui.transaction.AddTransactionScreen
import com.example.automaticfinances.ui.transaction.TransactionHistoryScreen
import com.example.automaticfinances.ui.insights.FinancialDashboardScreen
import com.example.automaticfinances.ui.insights.FinancialDashboardViewModel
import com.example.automaticfinances.ui.budget.BudgetManagementScreen
import com.example.automaticfinances.ui.goals.GoalsScreen
import com.example.automaticfinances.ui.reports.ReportsScreen
import com.example.automaticfinances.ui.income.IncomeScreen
import com.example.automaticfinances.ui.income.AddIncomeScreen
import com.example.automaticfinances.ui.openingbalance.OpeningBalanceSetupScreen
import com.example.automaticfinances.ui.openingbalance.OpeningBalanceManagementScreen
import com.example.automaticfinances.data.repo.BudgetRepository
import com.example.automaticfinances.data.repo.TransactionRepository
import com.example.automaticfinances.data.repo.CategoryRepository
import com.example.automaticfinances.data.db.AppDatabase
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.automaticfinances.ui.components.BottomNavigationWrapper

// Definición de rutas
object Routes {
    const val HOME = "home"
    const val TRANSACTION_DETAIL = "transaction_detail/{transactionId}"
    const val CATEGORY_MANAGEMENT = "category_management"
    const val ADD_TRANSACTION = "add_transaction"
    const val TRANSACTION_HISTORY = "transaction_history"
    const val FINANCIAL_DASHBOARD = "financial_dashboard"
    const val BUDGET_MANAGEMENT = "budget_management"
    const val GOALS_MANAGEMENT = "goals_management"
    const val REPORTS = "reports"
    const val BUDGET_DETAIL = "budget_detail/{budgetId}"
    const val INCOME_MANAGEMENT = "income_management"
    const val ADD_INCOME = "add_income"
    const val OPENING_BALANCE_SETUP = "opening_balance_setup"
    const val OPENING_BALANCE_MANAGEMENT = "opening_balance_management"
    
    fun transactionDetail(transactionId: String) = "transaction_detail/$transactionId"
    fun budgetDetail(budgetId: String) = "budget_detail/$budgetId"
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    onOpenNotifAccess: () -> Unit
) {
    BottomNavigationWrapper(navController = navController) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(paddingValues)
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
                onViewIncomesClick = {
                    navController.navigate(Routes.INCOME_MANAGEMENT)
                },
                onViewBalancesClick = {
                    navController.navigate(Routes.OPENING_BALANCE_MANAGEMENT)
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
            val categoryRepository = CategoryRepository()
            
            val dashboardViewModel: FinancialDashboardViewModel = viewModel {
                FinancialDashboardViewModel(budgetRepository, transactionRepository, categoryRepository)
            }
            
            FinancialDashboardScreen(
                viewModel = dashboardViewModel,
                onNavigateToBudgetManagement = {
                    navController.navigate(Routes.BUDGET_MANAGEMENT)
                },
                onNavigateToGoals = {
                    navController.navigate(Routes.GOALS_MANAGEMENT)
                },
                onNavigateToReports = {
                    navController.navigate(Routes.REPORTS)
                },
                onNavigateToBudgetDetail = { budgetId ->
                    navController.navigate(Routes.budgetDetail(budgetId.toString()))
                }
            )
        }
        
        composable(Routes.BUDGET_MANAGEMENT) {
            BudgetManagementScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Routes.GOALS_MANAGEMENT) {
            GoalsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Routes.REPORTS) {
            ReportsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Routes.BUDGET_DETAIL) { backStackEntry ->
            val budgetId = backStackEntry.arguments?.getString("budgetId") ?: ""
            // For now, navigate to budget management screen
            // In a full implementation, this would show budget detail with edit capability
            BudgetManagementScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Routes.INCOME_MANAGEMENT) {
            IncomeScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onAddIncomeClick = {
                    navController.navigate(Routes.ADD_INCOME)
                },
                onIncomeClick = { incomeId ->
                    navController.navigate(Routes.transactionDetail(incomeId))
                }
            )
        }
        
        composable(Routes.ADD_INCOME) {
            AddIncomeScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Routes.OPENING_BALANCE_SETUP) {
            OpeningBalanceSetupScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSetupComplete = {
                    navController.popBackStack()
                },
                isFirstTime = false
            )
        }
        
        composable(Routes.OPENING_BALANCE_MANAGEMENT) {
            OpeningBalanceManagementScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSetup = {
                    navController.navigate(Routes.OPENING_BALANCE_SETUP)
                }
            )
        }
        }
    }
}