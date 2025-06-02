package com.example.pennykeeper.ui.navigation

sealed class NavigationDestination(val route: String) {
    object Home : NavigationDestination("home")
    object Statistics : NavigationDestination("statistics")
    object Settings : NavigationDestination("settings")
    object SetBudget : NavigationDestination("settings/budget")
    object ManageCategories : NavigationDestination("settings/categories")
    object ExpensePrediction : NavigationDestination("settings/prediction")
    object DisplayMode : NavigationDestination("settings/displaymode")
    object EditExpense : NavigationDestination("edit/{expenseId}") {
        fun createRoute(expenseId: Int) = "edit/$expenseId"
        const val expenseIdArg = "expenseId"
    }
    object AddExpense : NavigationDestination("add")
    object ChatAnalysis : NavigationDestination("settings/chat")
}