package id.aiinvest.hanaveli

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import id.aiinvest.hanaveli.data.local.AppDatabase
import id.aiinvest.hanaveli.data.repository.ValasRepository
import id.aiinvest.hanaveli.theme.ValasMonitorTheme
import id.aiinvest.hanaveli.ui.screens.MainScreen
import id.aiinvest.hanaveli.ui.viewmodel.MonitorViewModel
import id.aiinvest.hanaveli.ui.viewmodel.MonitorViewModelFactory
import id.aiinvest.hanaveli.ui.viewmodel.SettingsViewModel
import id.aiinvest.hanaveli.ui.viewmodel.SettingsViewModelFactory
import id.aiinvest.hanaveli.ui.viewmodel.TransactionViewModel
import id.aiinvest.hanaveli.ui.viewmodel.TransactionViewModelFactory
import java.util.Locale

object LocaleHelper {
    fun wrap(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = context.resources.configuration
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(this)
            )
            val themePreference = settingsViewModel.themePreference.collectAsState().value
            val isDark = when (themePreference) {
                "light" -> false
                "dark" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            androidx.compose.runtime.LaunchedEffect(isDark) {
                androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !isDark
            }

            val languagePreference = settingsViewModel.language.collectAsState().value
            val context = LocalContext.current
            val localizedContext = remember(languagePreference) {
                if (languagePreference == "system") context else LocaleHelper.wrap(context, languagePreference)
            }

            CompositionLocalProvider(LocalContext provides localizedContext) {
                val useDynamicColor = settingsViewModel.useDynamicColor.collectAsState().value
                val lightPrimary = settingsViewModel.lightPrimaryHex.collectAsState().value
                val lightSecondary = settingsViewModel.lightSecondaryHex.collectAsState().value
                val lightText = settingsViewModel.lightTextHex.collectAsState().value
                val darkPrimary = settingsViewModel.darkPrimaryHex.collectAsState().value
                val darkSecondary = settingsViewModel.darkSecondaryHex.collectAsState().value
                val darkText = settingsViewModel.darkTextHex.collectAsState().value

                ValasMonitorTheme(
                    darkTheme = isDark,
                    dynamicColor = useDynamicColor,
                    lightPrimaryHex = lightPrimary,
                    lightSecondaryHex = lightSecondary,
                    lightTextHex = lightText,
                    darkPrimaryHex = darkPrimary,
                    darkSecondaryHex = darkSecondary,
                    darkTextHex = darkText
                ) {
                    val database = AppDatabase.getDatabase(LocalContext.current)
                    val repository = ValasRepository(database.monitoredCurrencyDao(), database.transactionDao(), LocalContext.current)
                    
                    val monitorViewModel: MonitorViewModel = viewModel(
                        factory = MonitorViewModelFactory(repository, database.monitoredCurrencyDao())
                    )
                    
                    val transactionViewModel: TransactionViewModel = viewModel(
                        factory = TransactionViewModelFactory(database.transactionDao())
                    )

                    MainScreen(
                        monitorViewModel = monitorViewModel,
                        transactionViewModel = transactionViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }
}
