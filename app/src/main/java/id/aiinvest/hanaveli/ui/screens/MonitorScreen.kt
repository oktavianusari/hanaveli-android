package id.aiinvest.hanaveli.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.aiinvest.hanaveli.R
import id.aiinvest.hanaveli.theme.SoftGreen
import id.aiinvest.hanaveli.theme.SoftRed
import id.aiinvest.hanaveli.ui.utils.getFlagEmojiForCurrency
import id.aiinvest.hanaveli.ui.viewmodel.MonitorViewModel
import id.aiinvest.hanaveli.ui.viewmodel.SettingsViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.foundation.combinedClickable

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MonitorScreen(
    monitorViewModel: MonitorViewModel,
    settingsViewModel: SettingsViewModel,
    onSettingsClick: () -> Unit
) {
    val currencies by monitorViewModel.monitoredCurrencies.collectAsState()
    val bcaLastUpdated by settingsViewModel.bcaLastUpdated.collectAsState()
    val isLoading by monitorViewModel.isLoading.collectAsState()
    val rateType by settingsViewModel.rateType.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var menuExpandedFor by remember { mutableStateOf<String?>(null) }

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val mutableList = currencies.toMutableList()
        val item = mutableList.removeAt(from.index)
        mutableList.add(to.index, item)
        monitorViewModel.updateDisplayOrder(mutableList)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    val formattedRate = when(rateType.lowercase()) {
                        "e-rate" -> "e-Rate"
                        "tt counter" -> "TT Counter"
                        "bank notes" -> "Bank Notes"
                        else -> rateType
                    }
                    Text(stringResource(R.string.monitor_title_dynamic, formattedRate)) 
                },
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),
                actions = {
                    IconButton(
                        onClick = { monitorViewModel.forceRefreshRates(rateType) },
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.width(24.dp).padding(2.dp))
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                        }
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_currency))
            }
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (bcaLastUpdated.isNotEmpty()) {
                Text(
                    text = bcaLastUpdated,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)
                )
            } else {
                Spacer(modifier = Modifier.padding(bottom = 16.dp))
            }
            
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 80.dp)
            ) {
                items(currencies, key = { it.currencyCode }) { c ->
                    ReorderableItem(reorderableState, key = c.currencyCode) { isDragging ->
                        val elevation = if (isDragging) 8.dp else 2.dp
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .shadow(elevation, RoundedCornerShape(8.dp)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onLongClick = { menuExpandedFor = c.currencyCode },
                                            onClick = {}
                                        )
                                        .padding(horizontal = 12.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Menu,
                                        contentDescription = "Reorder",
                                        modifier = Modifier
                                            .draggableHandle()
                                            .padding(end = 12.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    val emoji = getFlagEmojiForCurrency(c.currencyCode)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "$emoji ${c.currencyCode} IDR ${String.format("%,.2f", c.currentRate)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        val gainPercent = if (c.totalCost > 0) (c.totalGain / c.totalCost) * 100 else 0.0
                                        val gainText = if (c.totalGain >= 0) "+ Rp${String.format("%,.0f", c.totalGain).replace(',', '.')} (+${String.format("%.1f", gainPercent).replace('.', ',')}%)" else "- Rp${String.format("%,.0f", -c.totalGain).replace(',', '.')} (${String.format("%.1f", -gainPercent).replace('.', ',')}%)"
                                        val gainColor = if (c.totalGain >= 0) Color(0xFF388E3C) else Color(0xFFD32F2F)
                                        Text(
                                            text = "G: $gainText",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (c.totalGain != 0.0) gainColor else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    val percentChange = if (c.baseRateToday > 0) ((c.currentRate - c.baseRateToday) / c.baseRateToday) * 100 else 0.0
                                    val percentText = if (percentChange >= 0) String.format("+%.2f%%", percentChange) else String.format("%.2f%%", percentChange)
                                    val percentColor = if (percentChange >= 0) Color(0xFF388E3C) else Color(0xFFD32F2F)
                                    Text(
                                        text = percentText,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = percentColor
                                    )
                                }
                                
                                DropdownMenu(
                                    expanded = menuExpandedFor == c.currencyCode,
                                    onDismissRequest = { menuExpandedFor = null }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Move Up") },
                                        onClick = {
                                            val idx = currencies.indexOfFirst { it.currencyCode == c.currencyCode }
                                            if (idx > 0) {
                                                val mutableList = currencies.toMutableList()
                                                val item = mutableList.removeAt(idx)
                                                mutableList.add(idx - 1, item)
                                                monitorViewModel.updateDisplayOrder(mutableList)
                                            }
                                            menuExpandedFor = null
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Move Down") },
                                        onClick = {
                                            val idx = currencies.indexOfFirst { it.currencyCode == c.currencyCode }
                                            if (idx < currencies.size - 1) {
                                                val mutableList = currencies.toMutableList()
                                                val item = mutableList.removeAt(idx)
                                                mutableList.add(idx + 1, item)
                                                monitorViewModel.updateDisplayOrder(mutableList)
                                            }
                                            menuExpandedFor = null
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            monitorViewModel.removeCurrency(c.currencyCode)
                                            menuExpandedFor = null
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        val availableCurrencies = listOf("USD", "SGD", "HKD", "CHF", "GBP", "AUD", "JPY", "DKK", "CAD", "EUR", "SAR", "NZD", "CNY", "SEK", "THB", "RUB", "KRW", "MYR")
            .filter { code -> currencies.none { it.currencyCode == code } }

        var expanded by remember { mutableStateOf(false) }
        var selectedCurrency by remember { mutableStateOf(availableCurrencies.firstOrNull() ?: "") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(stringResource(R.string.add_monitor_title)) },
            text = {
                if (availableCurrencies.isEmpty()) {
                    Text(stringResource(R.string.all_monitored))
                } else {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedCurrency,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.select_currency)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            availableCurrencies.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text("${getFlagEmojiForCurrency(option)} $option") },
                                    onClick = {
                                        selectedCurrency = option
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (availableCurrencies.isNotEmpty()) {
                    Button(
                        onClick = {
                            if (selectedCurrency.isNotEmpty()) {
                                monitorViewModel.addCurrency(selectedCurrency)
                            }
                            showAddDialog = false
                        }
                    ) {
                        Text(stringResource(R.string.add))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
