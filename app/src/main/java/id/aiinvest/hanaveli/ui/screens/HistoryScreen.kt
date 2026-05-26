package id.aiinvest.hanaveli.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import id.aiinvest.hanaveli.R
import id.aiinvest.hanaveli.data.local.entity.Transaction
import id.aiinvest.hanaveli.ui.utils.getFlagEmojiForCurrency
import id.aiinvest.hanaveli.ui.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: TransactionViewModel) {
    val context = LocalContext.current
    val transactions by viewModel.transactions.collectAsState()
    
    var showDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var sortOption by remember { mutableStateOf("date_desc") }
    
    val sortedTransactions = remember(transactions, sortOption) {
        when (sortOption) {
            "date_asc" -> transactions.sortedBy { it.timestamp }
            "date_desc" -> transactions.sortedByDescending { it.timestamp }
            "portfolio_asc" -> transactions.sortedBy { it.currency }
            "portfolio_desc" -> transactions.sortedByDescending { it.currency }
            else -> transactions.sortedByDescending { it.timestamp }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),
                actions = {
                    var sortMenuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { sortMenuExpanded = true }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Sort")
                    }
                    androidx.compose.material3.DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                        androidx.compose.material3.DropdownMenuItem(text = { Text("Terbaru (Default)") }, onClick = { sortOption = "date_desc"; sortMenuExpanded = false })
                        androidx.compose.material3.DropdownMenuItem(text = { Text("Terlama") }, onClick = { sortOption = "date_asc"; sortMenuExpanded = false })
                        androidx.compose.material3.DropdownMenuItem(text = { Text("Portofolio (A-Z)") }, onClick = { sortOption = "portfolio_asc"; sortMenuExpanded = false })
                        androidx.compose.material3.DropdownMenuItem(text = { Text("Portofolio (Z-A)") }, onClick = { sortOption = "portfolio_desc"; sortMenuExpanded = false })
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                editingTransaction = null
                showDialog = true 
            }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_transaction))
            }
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            if (transactions.isEmpty()) {
                Text(stringResource(R.string.no_history), modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp))
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
                                val emoji = getFlagEmojiForCurrency(t.currency)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "$emoji ${t.currency} - ${t.type}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (t.type == "BUY") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                                    )
                                    Text("${stringResource(R.string.nominal)} ${String.format("%,.2f", t.amount)}")
                                    val displayRate = if (t.currency == "EMAS") t.rate / 100 else t.rate
                                    Text("${stringResource(R.string.rate)} ${String.format("%,.2f", displayRate)}")
                                    val dateStr = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(t.timestamp))
                                    Text("${stringResource(R.string.time)} $dateStr", style = MaterialTheme.typography.bodySmall)
                                }
                                
                                IconButton(onClick = {
                                    editingTransaction = t
                                    showDialog = true
                                }) {
                                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit))
                                }
                                IconButton(onClick = { viewModel.deleteTransaction(t) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        var currency by remember { mutableStateOf(editingTransaction?.currency ?: "") }
        var type by remember { mutableStateOf(editingTransaction?.type ?: "BUY") }
        var amountStr by remember { mutableStateOf(editingTransaction?.amount?.toString() ?: "") }
        var rateStr by remember { 
            mutableStateOf(
                if (editingTransaction != null) {
                    if (editingTransaction!!.currency == "EMAS") {
                        String.format(Locale.US, "%.0f", editingTransaction!!.rate / 100)
                    } else {
                        editingTransaction!!.rate.toString()
                    }
                } else ""
            ) 
        }
        
        var typeExpanded by remember { mutableStateOf(false) }
        var currencyExpanded by remember { mutableStateOf(false) }
        
        var timestamp by remember { mutableStateOf(editingTransaction?.timestamp ?: System.currentTimeMillis()) }
        var showDatePicker by remember { mutableStateOf(false) }
        
        val currencies = listOf("USD", "EUR", "GBP", "AUD", "CAD", "CHF", "CNY", "HKD", "JPY", "MYR", "NZD", "SAR", "SEK", "SGD", "THB", "EMAS")
        val types = listOf("BUY", "SELL")

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
                    TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editingTransaction == null) stringResource(R.string.add_transaction) else stringResource(R.string.edit_transaction)) },
            text = {
                Column {
                    // Currency Dropdown
                    ExposedDropdownMenuBox(
                        expanded = currencyExpanded,
                        onExpandedChange = { currencyExpanded = it },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = currency,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.currency)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = currencyExpanded,
                            onDismissRequest = { currencyExpanded = false }
                        ) {
                            currencies.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text("${getFlagEmojiForCurrency(option)} $option") },
                                    onClick = {
                                        currency = option
                                        currencyExpanded = false
                                    }
                                )
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
                            label = { Text(stringResource(R.string.transaction_type)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = typeExpanded,
                            onDismissRequest = { typeExpanded = false }
                        ) {
                            types.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(if (option == "BUY") stringResource(R.string.buy_type) else stringResource(R.string.sell_type)) },
                                    onClick = {
                                        type = option
                                        typeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text(if (currency == "EMAS") "Jumlah (Gram)" else stringResource(R.string.nominal_valas)) },
                        placeholder = { Text("mis. 100") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    
                    OutlinedTextField(
                        value = rateStr,
                        onValueChange = { rateStr = it },
                        label = { Text(stringResource(R.string.exchange_rate)) },
                        placeholder = { Text("mis. 15000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    val rt = rateStr.toDoubleOrNull() ?: 0.0
                    val totalValue = if (currency == "EMAS") amt * rt * 100 else amt * rt
                    if (amt > 0 && rt > 0) {
                        Text(
                            text = stringResource(R.string.total_rupiah, String.format("%,.2f", totalValue)),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Date selector row
                    OutlinedTextField(
                        value = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp)),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.transaction_date)) },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.select_date))
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountStr.toDoubleOrNull() ?: 0.0
                        val rt = rateStr.toDoubleOrNull() ?: 0.0
                        
                        if (amountStr.isEmpty() || rateStr.isEmpty()) {
                            Toast.makeText(context, "Semua kolom wajib diisi", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        if (currency.isNotEmpty() && amt > 0 && rt > 0) {
                            val rtToSave = if (currency == "EMAS") rt * 100 else rt
                            if (editingTransaction != null) {
                                viewModel.updateTransaction(
                                    editingTransaction!!.copy(
                                        currency = currency,
                                        type = type,
                                        amount = amt,
                                        rate = rtToSave,
                                        timestamp = timestamp
                                    )
                                )
                            } else {
                                viewModel.insertTransaction(
                                    currency = currency,
                                    type = type,
                                    amount = amt,
                                    rate = rtToSave,
                                    timestamp = timestamp
                                )
                            }
                            showDialog = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
