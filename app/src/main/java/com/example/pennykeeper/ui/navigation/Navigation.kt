package com.example.pennykeeper.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pennykeeper.AppViewModelFactory
import com.example.pennykeeper.data.repository.CategoryRepository
import com.example.pennykeeper.data.repository.ExpenseRepository
import com.example.pennykeeper.data.repository.SettingsRepository
import com.example.pennykeeper.data.repository.ThemeRepository
import com.example.pennykeeper.ui.expense.EditExpenseScreen
import com.example.pennykeeper.ui.expense.EditExpenseViewModel
import com.example.pennykeeper.ui.home.AddScreen
import com.example.pennykeeper.ui.home.HomeScreen
import com.example.pennykeeper.ui.home.HomeViewModel
import com.example.pennykeeper.ui.settings.CategoryViewModel
import com.example.pennykeeper.ui.settings.ChatAnalysisScreen
import com.example.pennykeeper.ui.settings.DisplayModeScreen
import com.example.pennykeeper.ui.settings.ManageCategoriesScreen
import com.example.pennykeeper.ui.settings.PredictionScreen
import com.example.pennykeeper.ui.settings.SetBudgetScreen
import com.example.pennykeeper.ui.settings.SettingsScreen
import com.example.pennykeeper.ui.settings.SettingsViewModel
import com.example.pennykeeper.ui.stats.StatisticsScreen
import com.example.pennykeeper.ui.stats.StatisticsViewModel


@Composable
fun Navigation(
    expenseRepository: ExpenseRepository,
    settingsRepository: SettingsRepository,
    categoryRepository: CategoryRepository,
    themeRepository: ThemeRepository,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()
    val factory = AppViewModelFactory(expenseRepository,settingsRepository, categoryRepository, themeRepository )

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavigationDestination.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavigationDestination.Home.route) {
                val homeViewModel = viewModel<HomeViewModel>(factory = factory)
                HomeScreen(
                    homeViewModel = homeViewModel,
                    onNavigateToEdit = { expenseId ->
                        navController.navigate(NavigationDestination.EditExpense.createRoute(expenseId))
                    },
                    onNavigateToAdd = {
                        navController.navigate(NavigationDestination.AddExpense.route)
                    }
                )
            }

            composable(NavigationDestination.AddExpense.route) {
                val homeViewModel = viewModel<HomeViewModel>(factory = factory)
                AddScreen(
                    viewModel = homeViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = NavigationDestination.EditExpense.route,
                arguments = listOf(
                    navArgument(NavigationDestination.EditExpense.expenseIdArg) {
                        type = NavType.IntType
                    }
                )
            ) { backStackEntry ->
                val expenseId = backStackEntry.arguments?.getInt(NavigationDestination.EditExpense.expenseIdArg) ?: return@composable
                val editViewModel = viewModel<EditExpenseViewModel>(factory = factory)
                EditExpenseScreen(
                    viewModel = editViewModel,
                    expenseId = expenseId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(NavigationDestination.Statistics.route) {
                val statisticsViewModel = viewModel<StatisticsViewModel>(factory = factory)
                StatisticsScreen(statisticsViewModel)
            }

            composable(NavigationDestination.Settings.route) {
                val settingsViewModel = viewModel<SettingsViewModel>(factory = factory)
                SettingsScreen(
                    settingsViewModel = settingsViewModel,
                    onNavigateToBudget = { navController.navigate(NavigationDestination.SetBudget.route) },
                    onNavigateToCategories = { navController.navigate(NavigationDestination.ManageCategories.route) },
                    onNavigateToPrediction = { navController.navigate(NavigationDestination.ExpensePrediction.route) },
                    onNavigateToDisplay = { navController.navigate(NavigationDestination.DisplayMode.route) },
                    onNavigateToChatAnalysis = { navController.navigate(NavigationDestination.ChatAnalysis.route) }
                )
            }

            composable(NavigationDestination.SetBudget.route) {
                val settingsViewModel = viewModel<SettingsViewModel>(factory = factory)
                SetBudgetScreen(
                    settingsViewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(NavigationDestination.ManageCategories.route) {
                val categoryViewModel = viewModel<CategoryViewModel>(factory = factory)
                ManageCategoriesScreen(
                    viewModel = categoryViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(NavigationDestination.ExpensePrediction.route) {
                val settingsViewModel = viewModel<SettingsViewModel>(factory = factory)
                PredictionScreen(
                    settingsViewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(NavigationDestination.ChatAnalysis.route) {
                val settingsViewModel = viewModel<SettingsViewModel>(factory = factory)
                ChatAnalysisScreen(
                    settingsViewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(NavigationDestination.DisplayMode.route) {
                val settingsViewModel = viewModel<SettingsViewModel>(factory = factory)
                DisplayModeScreen(
                    settingsViewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}


@Composable
private fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        BottomNavItem(
            route = NavigationDestination.Home.route,
            title = "Home",
            icon = Icons.Default.Home
        ),
        BottomNavItem(
            route = NavigationDestination.Statistics.route,
            title = "Statistics",
            icon = Icons.AutoMirrored.Filled.List
        ),
        BottomNavItem(
            route = NavigationDestination.Settings.route,
            title = "Settings",
            icon = Icons.Default.Settings
        )
    )

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = null) },
                label = { Text(screen.title) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

private data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)