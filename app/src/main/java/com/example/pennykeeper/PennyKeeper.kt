import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.example.pennykeeper.data.repository.CategoryRepository
import com.example.pennykeeper.data.repository.ExpenseRepository
import com.example.pennykeeper.data.repository.SettingsRepository
import com.example.pennykeeper.data.repository.ThemeRepository
import com.example.pennykeeper.ui.navigation.Navigation
import com.example.pennykeeper.ui.theme.PennykeeperTheme
import com.example.pennykeeper.ui.settings.SettingsViewModel

@Composable
fun PennyKeeper(
    expenseRepository: ExpenseRepository,
    settingsRepository: SettingsRepository,
    categoryRepository: CategoryRepository,
    themeRepository: ThemeRepository,
    settingsViewModel: SettingsViewModel
) {
    // Observe the dark mode state from SettingsViewModel
    val isDarkMode = settingsViewModel.isDarkMode.collectAsState().value

    // Pass the dark mode state to PennykeeperTheme
    PennykeeperTheme(darkTheme = isDarkMode) {
        Navigation(
            expenseRepository = expenseRepository,
            settingsRepository = settingsRepository,
            categoryRepository = categoryRepository,
            themeRepository = themeRepository,
            settingsViewModel = settingsViewModel
        )
    }
}