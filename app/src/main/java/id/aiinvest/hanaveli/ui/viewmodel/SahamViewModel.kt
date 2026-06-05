package id.aiinvest.hanaveli.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import id.aiinvest.hanaveli.data.local.entity.MonitoredStock
import id.aiinvest.hanaveli.data.repository.StockRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SahamViewModel(
    private val repository: StockRepository
) : ViewModel() {

    private val _monitoredStocks = MutableStateFlow<List<MonitoredStock>>(emptyList())
    val monitoredStocks: StateFlow<List<MonitoredStock>> = _monitoredStocks.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllStocks().collect { stocks ->
                _monitoredStocks.value = stocks
            }
        }
    }

    fun reorderStocks(fromIndex: Int, toIndex: Int) {
        val currentList = _monitoredStocks.value.toMutableList()
        if (fromIndex !in currentList.indices || toIndex !in currentList.indices) return
        
        val item = currentList.removeAt(fromIndex)
        currentList.add(toIndex, item)
        
        val updatedList = currentList.mapIndexed { index, stock -> 
            stock.copy(displayOrder = index) 
        }
        _monitoredStocks.value = updatedList
        
        viewModelScope.launch {
            repository.updateStocks(updatedList)
        }
    }

    fun addStock(symbol: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.addStock(symbol)
            onResult(success)
        }
    }

    fun deleteStock(symbol: String) {
        viewModelScope.launch {
            repository.deleteStock(symbol)
        }
    }

    fun refreshStocks(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.syncAllStocks()
            onComplete?.invoke()
        }
    }
}

class SahamViewModelFactory(
    private val repository: StockRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SahamViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SahamViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
