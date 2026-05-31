package id.aiinvest.hanaveli.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import id.aiinvest.hanaveli.data.local.dao.TransactionDao
import id.aiinvest.hanaveli.data.local.entity.Transaction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TransactionViewModel(
    private val transactionDao: TransactionDao
) : ViewModel() {

    val transactions: StateFlow<List<Transaction>> = transactionDao.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun insertTransaction(currency: String, type: String, rate: Double, amount: Double, timestamp: Long) {
        viewModelScope.launch {
            transactionDao.insertTransaction(
                Transaction(
                    currency = currency,
                    type = type,
                    rate = rate,
                    amount = amount,
                    timestamp = timestamp
                )
            )
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionDao.deleteTransaction(transaction)
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionDao.updateTransaction(transaction)
        }
    }

    fun exportTransactions(uri: android.net.Uri, context: android.content.Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val transactions = transactionDao.getAllTransactionsSync()
                val jsonArray = org.json.JSONArray()
                transactions.forEach { t ->
                    val obj = org.json.JSONObject().apply {
                        put("id", t.id)
                        put("currency", t.currency)
                        put("type", t.type)
                        put("rate", t.rate)
                        put("amount", t.amount)
                        put("timestamp", t.timestamp)
                    }
                    jsonArray.put(obj)
                }
                
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    java.io.BufferedWriter(java.io.OutputStreamWriter(outputStream, Charsets.UTF_8)).use { writer ->
                        writer.write(jsonArray.toString(4))
                        writer.flush()
                    }
                }
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Export berhasil disimpan", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Gagal export: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun importTransactions(uri: android.net.Uri, context: android.content.Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val jsonStr = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().use { it.readText() }
                } ?: return@launch
                
                val jsonArray = org.json.JSONArray(jsonStr)
                val existingTransactions = transactionDao.getAllTransactionsSync()
                
                var importedCount = 0
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val t = Transaction(
                        id = if (obj.has("id")) obj.getLong("id") else 0L,
                        currency = obj.getString("currency"),
                        type = obj.getString("type"),
                        rate = obj.getDouble("rate"),
                        amount = obj.getDouble("amount"),
                        timestamp = obj.getLong("timestamp")
                    )
                    
                    // Filter duplicate by ID
                    val isDuplicate = existingTransactions.any { it.id == t.id }
                    
                    if (!isDuplicate) {
                        transactionDao.insertTransaction(t)
                        importedCount++
                    }
                }
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Import berhasil! $importedCount data baru disinkronisasi.", android.widget.Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Gagal import: format tidak valid", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

class TransactionViewModelFactory(
    private val transactionDao: TransactionDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TransactionViewModel(transactionDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
