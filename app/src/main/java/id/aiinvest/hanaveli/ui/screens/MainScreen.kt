package id.aiinvest.hanaveli.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import id.aiinvest.hanaveli.R
import id.aiinvest.hanaveli.ui.viewmodel.MonitorViewModel
import id.aiinvest.hanaveli.ui.viewmodel.SettingsViewModel
import id.aiinvest.hanaveli.ui.viewmodel.TransactionViewModel
import id.aiinvest.hanaveli.ui.viewmodel.SahamViewModel
import id.aiinvest.hanaveli.ui.viewmodel.RiwayatSahamViewModel

@Composable
fun MainScreen(
    monitorViewModel: MonitorViewModel,
    sahamViewModel: SahamViewModel,
    transactionViewModel: TransactionViewModel,
    riwayatSahamViewModel: RiwayatSahamViewModel,
    settingsViewModel: SettingsViewModel
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    val tabs = listOf(stringResource(R.string.monitor_title), stringResource(R.string.saham_title), "Riwayat Valas", "Riwayat Saham", "Total Valas")
    val icons = listOf(Icons.Filled.Home, Icons.Filled.Star, Icons.AutoMirrored.Filled.List, Icons.AutoMirrored.Filled.List, Icons.Filled.ShoppingCart)

    if (showSettings) {
        SettingsScreen(viewModel = settingsViewModel, transactionViewModel = transactionViewModel, onBackClick = { showSettings = false })
    } else {
        Scaffold(
            bottomBar = {
                androidx.compose.material3.NavigationBar {
                    tabs.forEachIndexed { index, title ->
                        NavigationBarItem(
                            icon = { Icon(icons[index], contentDescription = title) },
                            label = { Text(title, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 2) },
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (selectedTabIndex) {
                    0 -> MonitorScreen(monitorViewModel, settingsViewModel, onSettingsClick = { showSettings = true })
                    1 -> SahamScreen(viewModel = sahamViewModel, settingsViewModel = settingsViewModel, onSettingsClick = { showSettings = true })
                    2 -> HistoryScreen(transactionViewModel)
                    3 -> RiwayatSahamScreen(riwayatSahamViewModel, sahamViewModel)
                    4 -> TotalScreen(monitorViewModel, settingsViewModel)
                }
            }
        }
    }
}
