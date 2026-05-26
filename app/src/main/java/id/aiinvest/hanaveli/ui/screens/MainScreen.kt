package id.aiinvest.hanaveli.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import id.aiinvest.hanaveli.ui.viewmodel.MonitorViewModel
import id.aiinvest.hanaveli.ui.viewmodel.SettingsViewModel
import id.aiinvest.hanaveli.ui.viewmodel.TransactionViewModel

@Composable
fun MainScreen(
    monitorViewModel: MonitorViewModel,
    transactionViewModel: TransactionViewModel,
    settingsViewModel: SettingsViewModel
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    val tabs = listOf("Monitor", "Riwayat", "Total")
    val icons = listOf(Icons.Filled.Home, Icons.Filled.List, Icons.Filled.ShoppingCart)

    if (showSettings) {
        SettingsScreen(viewModel = settingsViewModel, transactionViewModel = transactionViewModel, onBackClick = { showSettings = false })
    } else {
        Scaffold(
            bottomBar = {
                androidx.compose.material3.NavigationBar {
                    tabs.forEachIndexed { index, title ->
                        NavigationBarItem(
                            icon = { Icon(icons[index], contentDescription = title) },
                            label = { Text(title) },
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
                    1 -> HistoryScreen(transactionViewModel)
                    2 -> TotalScreen(monitorViewModel)
                }
            }
        }
    }
}
