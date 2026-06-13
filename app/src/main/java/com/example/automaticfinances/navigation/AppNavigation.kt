package com.example.automaticfinances.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import android.net.Uri
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.automaticfinances.ui.theme.MotionTokens
import com.example.automaticfinances.ui.HomeScreen
import com.example.automaticfinances.ui.HomeViewModel
import com.example.automaticfinances.ui.categories.CategoryManagementScreen
import com.example.automaticfinances.ui.categories.CategoryRulesScreen
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
import com.example.automaticfinances.ui.transfer.TransferScreen
import com.example.automaticfinances.ui.openingbalance.OpeningBalanceSetupScreen
import com.example.automaticfinances.ui.openingbalance.OpeningBalanceManagementScreen
import com.example.automaticfinances.ui.settings.SettingsScreen
import com.example.automaticfinances.ui.unparsed.UnparsedSmsScreen
import com.example.automaticfinances.ui.review.ReviewScreen
import com.example.automaticfinances.ui.suggestions.CategorySuggestionScreen
import com.example.automaticfinances.ui.components.BottomNavigationWrapper
import com.example.automaticfinances.utils.UnparsedTransactionHints

// Optional, nullable prefill arguments shared by the manual gasto/ingreso flows. All default to ""
// so plain navigation (no prefill) keeps matching the same destinations.
private fun prefillArgs() = listOf(
    navArgument(Routes.ARG_PREFILL_AMOUNT) { type = NavType.StringType; nullable = true; defaultValue = "" },
    navArgument(Routes.ARG_PREFILL_DESC) { type = NavType.StringType; nullable = true; defaultValue = "" },
    navArgument(Routes.ARG_UNPARSED_ID) { type = NavType.StringType; nullable = true; defaultValue = "" },
)

// Definición de rutas
object Routes {
    const val HOME = "home"
    const val TRANSACTION_DETAIL = "transaction_detail/{transactionId}"
    const val CATEGORY_MANAGEMENT = "category_management"
    const val CATEGORY_RULES = "category_rules"
    const val ADD_TRANSACTION = "add_transaction"
    const val TRANSACTION_HISTORY = "transaction_history"
    const val FINANCIAL_DASHBOARD = "financial_dashboard"
    const val BUDGET_MANAGEMENT = "budget_management"
    const val GOALS_MANAGEMENT = "goals_management"
    const val REPORTS = "reports"
    const val BUDGET_DETAIL = "budget_detail/{budgetId}"
    const val INCOME_MANAGEMENT = "income_management"
    const val ADD_INCOME = "add_income"
    const val TRANSFER = "transfer"
    const val OPENING_BALANCE_SETUP = "opening_balance_setup"
    const val OPENING_BALANCE_MANAGEMENT = "opening_balance_management"
    const val SETTINGS = "settings"
    const val UNPARSED_SMS = "unparsed_sms"
    const val REVIEW_QUEUE = "review_queue"
    const val CATEGORY_SUGGESTIONS = "category_suggestions"

    // Optional prefill args, shared by the manual gasto/ingreso flows so a message rescued from
    // "Mensajes no reconocidos" can open the form half-filled. All optional (default ""), so plain
    // navigate(ADD_TRANSACTION)/navigate(ADD_INCOME) keeps working unchanged.
    const val ARG_PREFILL_AMOUNT = "amount"
    const val ARG_PREFILL_DESC = "desc"
    const val ARG_UNPARSED_ID = "unparsedId"
    private const val PREFILL_QUERY =
        "?$ARG_PREFILL_AMOUNT={$ARG_PREFILL_AMOUNT}&$ARG_PREFILL_DESC={$ARG_PREFILL_DESC}&$ARG_UNPARSED_ID={$ARG_UNPARSED_ID}"
    const val ADD_TRANSACTION_ROUTE = ADD_TRANSACTION + PREFILL_QUERY
    const val ADD_INCOME_ROUTE = ADD_INCOME + PREFILL_QUERY

    fun transactionDetail(transactionId: String) = "transaction_detail/$transactionId"
    fun budgetDetail(budgetId: String) = "budget_detail/$budgetId"

    fun addTransaction(amount: String? = null, desc: String? = null, unparsedId: String? = null) =
        "$ADD_TRANSACTION?$ARG_PREFILL_AMOUNT=${Uri.encode(amount.orEmpty())}" +
            "&$ARG_PREFILL_DESC=${Uri.encode(desc.orEmpty())}" +
            "&$ARG_UNPARSED_ID=${Uri.encode(unparsedId.orEmpty())}"

    fun addIncome(amount: String? = null, desc: String? = null, unparsedId: String? = null) =
        "$ADD_INCOME?$ARG_PREFILL_AMOUNT=${Uri.encode(amount.orEmpty())}" +
            "&$ARG_PREFILL_DESC=${Uri.encode(desc.orEmpty())}" +
            "&$ARG_UNPARSED_ID=${Uri.encode(unparsedId.orEmpty())}"
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    themeViewModel: com.example.automaticfinances.ui.theme.ThemeViewModel,
    onOpenNotifAccess: () -> Unit,
    onVoiceEntry: () -> Unit = {}
) {
    BottomNavigationWrapper(navController = navController) { paddingValues ->
        val slideDuration = MotionTokens.DurationEnter
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(paddingValues),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it / 5 },
                    animationSpec = tween(slideDuration, easing = MotionTokens.EmphasizedDecelerate)
                ) + fadeIn(tween(slideDuration))
            },
            exitTransition = {
                fadeOut(tween(MotionTokens.DurationShort)) +
                    slideOutHorizontally(targetOffsetX = { -it / 10 }, animationSpec = tween(slideDuration))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it / 5 },
                    animationSpec = tween(slideDuration, easing = MotionTokens.EmphasizedDecelerate)
                ) + fadeIn(tween(slideDuration))
            },
            popExitTransition = {
                fadeOut(tween(MotionTokens.DurationShort)) +
                    slideOutHorizontally(targetOffsetX = { it / 5 }, animationSpec = tween(slideDuration))
            }
        ) {
        composable(Routes.HOME) { backStackEntry ->
            val homeViewModel: HomeViewModel = hiltViewModel()
            
            // Al volver a Home, solo re-leemos los saldos (las transacciones ya se actualizan en
            // vivo vía el flow de Room); evita una recarga completa en cada navegación.
            LaunchedEffect(backStackEntry) {
                homeViewModel.refreshBalances()
            }
            
            HomeScreen(
                stateFlow = homeViewModel.state,
                onOpenNotifAccess = onOpenNotifAccess,
                onTransactionClick = { transactionId ->
                    navController.navigate(Routes.transactionDetail(transactionId))
                },
                onAddTransactionClick = {
                    navController.navigate(Routes.ADD_TRANSACTION)
                },
                onAddVoiceClick = onVoiceEntry,
                onAddIncomeClick = {
                    navController.navigate(Routes.ADD_INCOME)
                },
                onAddTransferClick = {
                    navController.navigate(Routes.TRANSFER)
                },
                onViewHistoryClick = {
                    // Select the "Movimientos" tab (single source of truth) instead of
                    // pushing a second copy of the history screen on top of Home. This keeps
                    // the back stack predictable: system-back from history returns to Home.
                    navController.navigate(Routes.TRANSACTION_HISTORY) {
                        popUpTo(Routes.HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onBankBalanceClick = {
                    navController.navigate(Routes.OPENING_BALANCE_MANAGEMENT)
                },
                onCashBalanceClick = {
                    navController.navigate(Routes.OPENING_BALANCE_MANAGEMENT)
                },
                onRefresh = {
                    homeViewModel.refreshData()
                },
                onSearchQueryChange = { query ->
                    homeViewModel.updateSearchQuery(query)
                },
                onReviewClick = {
                    navController.navigate(Routes.REVIEW_QUEUE)
                },
                onViewSuggestionsClick = {
                    navController.navigate(Routes.CATEGORY_SUGGESTIONS)
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

        composable(Routes.CATEGORY_RULES) {
            CategoryRulesScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Routes.ADD_TRANSACTION_ROUTE,
            arguments = prefillArgs(),
        ) {
            AddTransactionScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.TRANSACTION_HISTORY) {
            // Reached as a bottom-nav root → no back arrow; the nav bar is the way out.
            TransactionHistoryScreen(
                onNavigateBack = null,
                onTransactionClick = { transactionId ->
                    navController.navigate(Routes.transactionDetail(transactionId))
                }
            )
        }
        
        composable(Routes.FINANCIAL_DASHBOARD) {
            val dashboardViewModel: FinancialDashboardViewModel = hiltViewModel()

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
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS) {
                        popUpTo(Routes.HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
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
        
        composable(
            route = Routes.ADD_INCOME_ROUTE,
            arguments = prefillArgs(),
        ) {
            AddIncomeScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Routes.TRANSFER) {
            TransferScreen(
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

        composable(Routes.SETTINGS) {
            SettingsScreen(
                themeViewModel = themeViewModel,
                onOpenNotifAccess = onOpenNotifAccess,
                onNavigateToCategories = {
                    navController.navigate(Routes.CATEGORY_MANAGEMENT)
                },
                onNavigateToCategoryRules = {
                    navController.navigate(Routes.CATEGORY_RULES)
                },
                onNavigateToIncomes = {
                    navController.navigate(Routes.INCOME_MANAGEMENT)
                },
                onNavigateToBalances = {
                    navController.navigate(Routes.OPENING_BALANCE_MANAGEMENT)
                },
                onNavigateToUnparsed = {
                    navController.navigate(Routes.UNPARSED_SMS)
                },
                onNavigateToReview = {
                    navController.navigate(Routes.REVIEW_QUEUE)
                }
            )
        }

        composable(Routes.UNPARSED_SMS) {
            UnparsedSmsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onRegister = { sms ->
                    // Pre-fill the matching manual flow (ingreso vs gasto) with the extracted amount
                    // and a description seed; pass the message id so the form deletes it on save.
                    val amount = UnparsedTransactionHints.extractAmount(sms.text)
                    val desc = UnparsedTransactionHints.suggestedDescription(sms.text)
                    val route = if (UnparsedTransactionHints.looksLikeIncome(sms.text)) {
                        Routes.addIncome(amount, desc, sms.id)
                    } else {
                        Routes.addTransaction(amount, desc, sms.id)
                    }
                    navController.navigate(route)
                },
            )
        }

        composable(Routes.REVIEW_QUEUE) {
            ReviewScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.CATEGORY_SUGGESTIONS) {
            CategorySuggestionScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        }
    }
}