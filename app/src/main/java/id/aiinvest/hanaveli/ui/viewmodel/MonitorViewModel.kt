package id.aiinvest.hanaveli.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import id.aiinvest.hanaveli.data.local.dao.MonitoredCurrencyDao
import id.aiinvest.hanaveli.data.local.entity.MonitoredCurrency
import id.aiinvest.hanaveli.data.repository.ValasRepository
import id.aiinvest.hanaveli.domain.model.CurrencyData
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MonitorViewModel(
    private val repository: ValasRepository,
    private val currencyDao: MonitoredCurrencyDao
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val monitoredCurrencies: StateFlow<List<CurrencyData>> = repository.getMonitoredCurrenciesWithData()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun updateDisplayOrder(currencies: List<CurrencyData>) {
        viewModelScope.launch {
            val updated = currencies.mapIndexed { index, data ->
                MonitoredCurrency(
                    currency = data.currencyCode,
                    displayOrder = index,
                    baseRateToday = data.baseRateToday,
                    lastRate = data.currentRate
                )
            }
            currencyDao.updateMonitoredCurrencies(updated)
        }
    }

    fun syncRates(rateType: String) {
        viewModelScope.launch {
            repository.syncRates(rateType)
        }
    }

    fun forceRefreshRates(rateType: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.syncRates(rateType)
            _isLoading.value = false
        }
    }

    fun removeCurrency(currencyCode: String) {
        viewModelScope.launch {
            currencyDao.deleteMonitoredCurrency(MonitoredCurrency(currencyCode, 0))
        }
    }

    fun addCurrency(currencyCode: String) {
        viewModelScope.launch {
            val currentMax = currencyDao.getMaxDisplayOrder() ?: -1
            currencyDao.insertMonitoredCurrency(
                MonitoredCurrency(
                    currency = currencyCode,
                    displayOrder = currentMax + 1
                )
            )
        }
    }
}

class MonitorViewModelFactory(
    private val repository: ValasRepository,
    private val currencyDao: MonitoredCurrencyDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MonitorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MonitorViewModel(repository, currencyDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
