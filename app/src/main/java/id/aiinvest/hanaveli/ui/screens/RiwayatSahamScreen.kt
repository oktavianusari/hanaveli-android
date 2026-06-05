package id.aiinvest.hanaveli.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import id.aiinvest.hanaveli.R
import id.aiinvest.hanaveli.data.local.entity.SahamTransaction
import id.aiinvest.hanaveli.ui.viewmodel.RiwayatSahamViewModel
import id.aiinvest.hanaveli.ui.viewmodel.SahamViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiwayatSahamScreen(viewModel: RiwayatSahamViewModel, sahamViewModel: SahamViewModel) {
    val context = LocalContext.current
    val transactions by viewModel.transactions.collectAsState()
    val stocks by sahamViewModel.monitoredStocks.collectAsState()
    
    var showDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<SahamTransaction?>(null) }
    var sortOption by remember { mutableStateOf("date_desc") }
    
    val sortedTransactions = remember(transactions, sortOption) {
        when (sortOption) {
            "date_asc" -> transactions.sortedBy { it.timestamp }
            "date_desc" -> transactions.sortedByDescending { it.timestamp }
            "portfolio_asc" -> transactions.sortedBy { it.symbol }
            "portfolio_desc" -> transactions.sortedByDescending { it.symbol }
            else -> transactions.sortedByDescending { it.timestamp }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Saham") },
                windowInsets = WindowInsets(0.dp),
                actions = {
                    var sortMenuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { sortMenuExpanded = true }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Sort")
                    }
                    DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                        DropdownMenuItem(text = { Text("Terbaru (Default)") }, onClick = { sortOption = "date_desc"; sortMenuExpanded = false })
                        DropdownMenuItem(text = { Text("Terlama") }, onClick = { sortOption = "date_asc"; sortMenuExpanded = false })
                        DropdownMenuItem(text = { Text("Portofolio (A-Z)") }, onClick = { sortOption = "portfolio_asc"; sortMenuExpanded = false })
                        DropdownMenuItem(text = { Text("Portofolio (Z-A)") }, onClick = { sortOption = "portfolio_desc"; sortMenuExpanded = false })
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                editingTransaction = null
                showDialog = true 
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Tambah Transaksi Saham")
            }
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            if (transactions.isEmpty()) {
                Text("Belum ada riwayat transaksi saham.", modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp))
            } else {
                LazyColumn {
                    items(sortedTransactions) { t ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${t.symbol} - ${t.type}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (t.type == "BUY") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                                    )
                                    Text("Volume: ${t.lot} Lot (${t.lot * 100} Lembar)")
                                    Text("Harga Match: ${String.format("%,.2f", t.price)}")
                                    val dateStr = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(t.timestamp))
                                    Text("Waktu: $dateStr", style = MaterialTheme.typography.bodySmall)
                                }
                                
                                IconButton(onClick = {
                                    editingTransaction = t
                                    showDialog = true
                                }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = { viewModel.deleteTransaction(t) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        var symbol by remember { mutableStateOf(editingTransaction?.symbol ?: "") }
        var type by remember { mutableStateOf(editingTransaction?.type ?: "BUY") }
        var lotStr by remember { mutableStateOf(editingTransaction?.lot?.toString() ?: "") }
        var priceStr by remember { mutableStateOf(if (editingTransaction != null) editingTransaction!!.price.toString() else "") }
        
        var typeExpanded by remember { mutableStateOf(false) }
        var symbolExpanded by remember { mutableStateOf(false) }
        
        var timestamp by remember { mutableStateOf(editingTransaction?.timestamp ?: System.currentTimeMillis()) }
        var showDatePicker by remember { mutableStateOf(false) }
        
        val types = listOf("BUY", "SELL")
        val monitoredSymbols = stocks.map { it.symbol }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = timestamp)
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { timestamp = it }
                        showDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editingTransaction == null) "Tambah Transaksi Saham" else "Edit Transaksi Saham") },
            text = {
                Column {
                    // Symbol Dropdown
                    ExposedDropdownMenuBox(
                        expanded = symbolExpanded,
                        onExpandedChange = { symbolExpanded = it },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = symbol,
                            onValueChange = { symbol = it },
                            label = { Text("Kode Emiten (mis: BBCA.JK)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = symbolExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        if (monitoredSymbols.isNotEmpty()) {
                            ExposedDropdownMenu(
                                expanded = symbolExpanded,
                                onDismissRequest = { symbolExpanded = false }
                            ) {
                                monitoredSymbols.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            symbol = option
                                            symbolExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    // Type Dropdown
                    ExposedDropdownMenuBox(
                        expanded = typeExpanded,
                        onExpandedChange = { typeExpanded = it },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = type,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Jenis Transaksi") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = typeExpanded,
                            onDismissRequest = { typeExpanded = false }
                        ) {
                            types.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(if (option == "BUY") "Beli (BUY)" else "Jual (SELL)") },
                                    onClick = {
                                        type = option
                                        typeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = lotStr,
                        onValueChange = { lotStr = it },
                        label = { Text("Volume (Lot)") },
                        placeholder = { Text("1 Lot = 100 lembar") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Harga Match (Per Lembar)") },
                        placeholder = { Text("mis. 10000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    
                    val lot = lotStr.toIntOrNull() ?: 0
                    val price = priceStr.toDoubleOrNull() ?: 0.0
                    val totalValue = lot * 100 * price
                    if (lot > 0 && price > 0) {
                        Text(
                            text = "Total Transaksi: ${String.format("%,.2f", totalValue)}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Date selector row
                    OutlinedTextField(
                        value = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp)),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tanggal Transaksi") },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Pilih Tanggal")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val lot = lotStr.toIntOrNull() ?: 0
                        val price = priceStr.toDoubleOrNull() ?: 0.0
                        
                        if (symbol.isEmpty() || lotStr.isEmpty() || priceStr.isEmpty()) {
                            Toast.makeText(context, "Semua kolom wajib diisi", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        if (lot > 0 && price > 0) {
                            if (editingTransaction != null) {
                                viewModel.updateTransaction(
                                    editingTransaction!!.copy(
                                        symbol = symbol.uppercase(),
                                        type = type,
                                        lot = lot,
                                        price = price,
                                        timestamp = timestamp
                                    )
                                )
                            } else {
                                viewModel.insertTransaction(
                                    symbol = symbol.uppercase(),
                                    type = type,
                                    lot = lot,
                                    price = price,
                                    timestamp = timestamp
                                )
                            }
                            showDialog = false
                        }
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
