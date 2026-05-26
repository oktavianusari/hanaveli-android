package id.aiinvest.hanaveli.data.repository

import android.util.Log
import id.aiinvest.hanaveli.data.local.dao.MonitoredCurrencyDao
import id.aiinvest.hanaveli.data.local.dao.TransactionDao
import id.aiinvest.hanaveli.data.local.entity.MonitoredCurrency
import id.aiinvest.hanaveli.data.local.entity.Transaction
import id.aiinvest.hanaveli.data.remote.BcaScraper
import id.aiinvest.hanaveli.domain.model.CurrencyData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import id.aiinvest.hanaveli.data.remote.ScrapedRate
import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ValasRepository(
    private val monitoredCurrencyDao: MonitoredCurrencyDao,
    private val transactionDao: TransactionDao,
    private val context: Context
) {

    fun getMonitoredCurrenciesWithData(): Flow<List<CurrencyData>> {
        return combine(
            monitoredCurrencyDao.getAllMonitoredCurrencies(),
            transactionDao.getAllTransactions()
        ) { currencies, allTransactions ->
            currencies.map { currency ->
                val transactions = allTransactions.filter { it.currency == currency.currency }.sortedBy { it.timestamp }
                calculateCurrencyData(currency, transactions)
            }.sortedBy { it.displayOrder }
        }
    }

    private fun calculateCurrencyData(currency: MonitoredCurrency, transactions: List<Transaction>): CurrencyData {
        var totalAmount = 0.0
        var totalCost = 0.0
        var realizedGain = 0.0

        for (t in transactions) {
            if (t.type == "BUY") {
                totalAmount += t.amount
                totalCost += t.amount * t.rate
            } else if (t.type == "SELL") {
                val averageCost = if (totalAmount > 0) totalCost / totalAmount else 0.0
                realizedGain += (t.rate - averageCost) * t.amount
                totalAmount -= t.amount
                totalCost -= t.amount * averageCost
            }
        }

        val currentRate = currency.lastRate
        val unrealizedGain = if (currentRate > 0) (totalAmount * currentRate) - totalCost else 0.0
        val totalGain = realizedGain + unrealizedGain
        val averageBuyPrice = if (totalAmount > 0) totalCost / totalAmount else 0.0

        return CurrencyData(
            currencyCode = currency.currency,
            displayOrder = currency.displayOrder,
            currentRate = currentRate,
            baseRateToday = currency.baseRateToday,
            balance = totalAmount,
            averageBuyPrice = averageBuyPrice,
            totalCost = totalCost,
            realizedGain = realizedGain,
            unrealizedGain = unrealizedGain,
            totalGain = totalGain
        )
    }

    suspend fun syncRates(rateType: String) {
        withContext(Dispatchers.IO) {
            try {
                val bcaDeferred = async { BcaScraper.scrapeRates() }
                val pegadaianDeferred = async { id.aiinvest.hanaveli.data.remote.PegadaianScraper.scrapeRates() }
                
                val bcaResult = bcaDeferred.await()
                val pegadaianResult = pegadaianDeferred.await()

                val scrapedRates = mutableMapOf<String, ScrapedRate>()
                scrapedRates.putAll(bcaResult.rates)
                scrapedRates.putAll(pegadaianResult.rates)
                
                val bcaLastUpdated = bcaResult.lastUpdated
                val pegadaianLastUpdated = pegadaianResult.lastUpdated
                
                val prefs = context.getSharedPreferences("valas_settings", Context.MODE_PRIVATE)
                val todayStr = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                val lastSyncDay = prefs.getString("last_sync_day", "")
                val isNewDay = todayStr != lastSyncDay
                
                // save new sync timestamp and day
                val editor = prefs.edit()
                    .putString("last_sync_day", todayStr)
                    .putLong("last_sync_timestamp", System.currentTimeMillis())
                
                if (bcaLastUpdated.isNotEmpty()) {
                    editor.putString("bca_last_updated", bcaLastUpdated)
                }
                if (pegadaianLastUpdated.isNotEmpty()) {
                    editor.putString("pegadaian_last_updated", pegadaianLastUpdated)
                }
                editor.apply()
                
                val rateDirection = prefs.getString("rate_direction", "jual") ?: "jual"
                
                val localCurrencies = monitoredCurrencyDao.getAllMonitoredCurrenciesSync()
                val updatedCurrencies = localCurrencies.map { currency ->
                    val rateInfo = scrapedRates[currency.currency]
                    if (rateInfo != null) {
                        val currentPrefDirection = if (currency.currency == "EMAS") prefs.getString("emas_rate_direction", "jual") ?: "jual" else rateDirection
                        val isJual = currentPrefDirection.lowercase() == "jual"
                        val currentRate = when (rateType.lowercase()) {
                            "e-rate" -> if (isJual) rateInfo.eRateSell else rateInfo.eRateBuy
                            "bank notes", "bank-notes" -> if (isJual) rateInfo.bankNotesSell else rateInfo.bankNotesBuy
                            "tt counter", "tt-counter" -> if (isJual) rateInfo.ttCounterSell else rateInfo.ttCounterBuy
                            else -> if (isJual) rateInfo.eRateSell else rateInfo.eRateBuy
                        }
                        
                        // If currentRate is 0.0 (e.g., parsing failed), fallback to lastRate
                        val finalRate = if (currentRate > 0) currentRate else currency.lastRate
                        
                        // If baseRateToday is 0 or new day, initialize it
                        val baseRate = if (currency.baseRateToday == 0.0 || isNewDay) finalRate else currency.baseRateToday
                        
                        currency.copy(
                            lastRate = finalRate,
                            baseRateToday = baseRate
                        )
                    } else {
                        currency
                    }
                }
                monitoredCurrencyDao.updateMonitoredCurrencies(updatedCurrencies)

                val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    android.content.ComponentName(context, id.aiinvest.hanaveli.widget.ValasWidgetProvider::class.java)
                )
                val updateIntent = android.content.Intent(context, id.aiinvest.hanaveli.widget.ValasWidgetProvider::class.java).apply {
                    action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(updateIntent)
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, id.aiinvest.hanaveli.R.id.widget_list)
            } catch (e: Exception) {
                Log.e("ValasRepository", "Error syncing rates", e)
            }
        }
    }
}
