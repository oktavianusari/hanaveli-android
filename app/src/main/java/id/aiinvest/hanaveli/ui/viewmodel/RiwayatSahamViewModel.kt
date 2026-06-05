package id.aiinvest.hanaveli.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import id.aiinvest.hanaveli.data.local.dao.SahamTransactionDao
import id.aiinvest.hanaveli.data.local.entity.SahamTransaction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RiwayatSahamViewModel(private val dao: SahamTransactionDao) : ViewModel() {
    val transactions: StateFlow<List<SahamTransaction>> = dao.getAllTransactions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun insertTransaction(symbol: String, type: String, lot: Int, price: Double, timestamp: Long) {
        viewModelScope.launch {
            dao.insertTransaction(
                SahamTransaction(
                    symbol = symbol,
                    type = type,
                    lot = lot,
                    price = price,
                    timestamp = timestamp
                )
            )
        }
    }

    fun updateTransaction(transaction: SahamTransaction) {
        viewModelScope.launch {
            dao.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: SahamTransaction) {
        viewModelScope.launch {
            dao.deleteTransaction(transaction)
        }
    }
}

class RiwayatSahamViewModelFactory(private val dao: SahamTransactionDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RiwayatSahamViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RiwayatSahamViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
