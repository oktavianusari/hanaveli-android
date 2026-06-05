package id.aiinvest.hanaveli.ui.screens

import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.Menu
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.aiinvest.hanaveli.R
import id.aiinvest.hanaveli.ui.utils.getFlagEmojiForCurrency
import id.aiinvest.hanaveli.ui.viewmodel.SahamViewModel
import id.aiinvest.hanaveli.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SahamScreen(
    viewModel: SahamViewModel,
    settingsViewModel: SettingsViewModel,
    onSettingsClick: () -> Unit
) {
    val stocks by viewModel.monitoredStocks.collectAsState()
    val isSensitiveDataVisible by settingsViewModel.isSensitiveDataVisible.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var menuExpandedFor by remember { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val infiniteTransition = rememberInfiniteTransition()
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "refresh_rotation"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saham Hari Ini") },
                windowInsets = WindowInsets(0.dp),
                actions = {
                    IconButton(onClick = { settingsViewModel.toggleSensitiveDataVisibility() }) {
                        Icon(if (isSensitiveDataVisible) Icons.Filled.Clear else Icons.Filled.Lock, contentDescription = "Toggle Visibility")
                    }
                    IconButton(
                        onClick = { 
                            isRefreshing = true
                            viewModel.refreshStocks {
                                isRefreshing = false
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", modifier = Modifier.rotate(if (isRefreshing) angle else 0f))
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Tambah Saham")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            val lastUpdateMillis = stocks.maxOfOrNull { it.lastUpdated } ?: 0L
            if (lastUpdateMillis > 0) {
                val dateStr = java.text.SimpleDateFormat("dd MMMM yyyy HH:mm", java.util.Locale("id", "ID")).format(java.util.Date(lastUpdateMillis))
                Text(
                    text = "Update terakhir: $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)
                )
            } else {
                Spacer(modifier = Modifier.padding(bottom = 16.dp))
            }

            if (stocks.isEmpty()) {
                Text(
                    text = "Belum ada saham yang dimonitor.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            val lazyListState = rememberLazyListState()
            val state = rememberReorderableLazyListState(lazyListState) { from, to ->
                viewModel.reorderStocks(from.index, to.index)
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(stocks, key = { it.symbol }) { s ->
                    ReorderableItem(state, key = s.symbol) { isDragging ->
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
                                        onLongClick = { menuExpandedFor = s.symbol },
                                        onClick = {}
                                    )
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
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
                                
                                val emoji = if (s.symbol.endsWith(".JK", ignoreCase = true)) "🇮🇩" else "🇺🇸"
                                Column(modifier = Modifier.weight(1f)) {
                                    val priceText = if (!isSensitiveDataVisible) "----" else String.format("%,.2f", s.price)
                                    Text(
                                        text = "$emoji ${s.symbol} - ${s.currency} $priceText",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = s.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                val percentColor = if (s.changePercent >= 0) Color(0xFF388E3C) else Color(0xFFD32F2F)
                                val percentText = if (!isSensitiveDataVisible) "----%" else if (s.changePercent >= 0) String.format("+%.2f%%", s.changePercent) else String.format("%.2f%%", s.changePercent)
                                
                                Text(
                                    text = percentText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = percentColor
                                )
                            }
                            
                            DropdownMenu(
                                expanded = menuExpandedFor == s.symbol,
                                onDismissRequest = { menuExpandedFor = null }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        viewModel.deleteStock(s.symbol)
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
        var symbolInput by remember { mutableStateOf("") }
        var isSearching by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSearching) showAddDialog = false },
            title = { Text("Tambah Saham / Emiten") },
            text = {
                Column {
                    Text("Ketik kode saham resmi Yahoo Finance.\nContoh: BBCA.JK, GOOG, AAPL, NVDA")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = symbolInput,
                        onValueChange = { symbolInput = it.uppercase() },
                        label = { Text("Kode Emiten") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (symbolInput.isNotEmpty()) {
                            isSearching = true
                            viewModel.addStock(symbolInput) { success ->
                                isSearching = false
                                if (success) {
                                    showAddDialog = false
                                } else {
                                    Toast.makeText(context, "Saham $symbolInput tidak ditemukan atau gagal jaringan", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    enabled = !isSearching
                ) {
                    Text(if (isSearching) "Mencari..." else stringResource(R.string.add))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }, enabled = !isSearching) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
