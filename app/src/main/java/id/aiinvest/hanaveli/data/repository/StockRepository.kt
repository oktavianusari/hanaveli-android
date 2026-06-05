package id.aiinvest.hanaveli.data.repository

import id.aiinvest.hanaveli.data.local.dao.StockDao
import id.aiinvest.hanaveli.data.local.entity.MonitoredStock
import id.aiinvest.hanaveli.data.remote.YahooFinanceScraper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import android.content.Context

class StockRepository(private val stockDao: StockDao, private val context: Context? = null) {

    fun getAllStocks(): Flow<List<MonitoredStock>> = stockDao.getAllStocks()

    suspend fun addStock(symbol: String): Boolean = withContext(Dispatchers.IO) {
        val upperSymbol = symbol.uppercase()
        // Try to fetch from Yahoo Finance to verify if it exists
        val fetchedStock = YahooFinanceScraper.scrapeStock(upperSymbol)
        if (fetchedStock != null) {
            val maxOrder = stockDao.getMaxDisplayOrder() ?: 0
            val newStock = fetchedStock.copy(displayOrder = maxOrder + 1)
            stockDao.insertStock(newStock)
            return@withContext true
        }
        return@withContext false
    }

    suspend fun deleteStock(symbol: String) = withContext(Dispatchers.IO) {
        stockDao.deleteStockBySymbol(symbol.uppercase())
    }

    suspend fun syncAllStocks() = withContext(Dispatchers.IO) {
        val currentStocks = stockDao.getAllStocksSync()
        if (currentStocks.isEmpty()) return@withContext

        val symbols = currentStocks.map { it.symbol }
        val updatedStocksRaw = YahooFinanceScraper.scrapeStocks(symbols)
        
        if (updatedStocksRaw.isNotEmpty()) {
            val updatedStocks = updatedStocksRaw.map { newStock ->
                val oldOrder = currentStocks.find { it.symbol == newStock.symbol }?.displayOrder ?: 0
                newStock.copy(displayOrder = oldOrder)
            }
            stockDao.insertAll(updatedStocks)
            
            context?.let { ctx ->
                val prefs = ctx.getSharedPreferences("valas_settings", Context.MODE_PRIVATE)
                val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale("id", "ID"))
                val dateStr = dateFormat.format(java.util.Date())
                prefs.edit().putString("saham_last_updated", dateStr).apply()
            }
        }
    }

    suspend fun updateStocks(stocks: List<MonitoredStock>) = withContext(Dispatchers.IO) {
        stockDao.updateStocks(stocks)
        
        context?.let { ctx ->
            val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(ctx)
            
            val sahamAppWidgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(ctx, id.aiinvest.hanaveli.widget.SahamWidgetProvider::class.java)
            )
            val updateIntent1 = android.content.Intent(ctx, id.aiinvest.hanaveli.widget.SahamWidgetProvider::class.java).apply {
                action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, sahamAppWidgetIds)
            }
            ctx.sendBroadcast(updateIntent1)
            appWidgetManager.notifyAppWidgetViewDataChanged(sahamAppWidgetIds, id.aiinvest.hanaveli.R.id.widget_list)

            val sahamHorizontalAppWidgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(ctx, id.aiinvest.hanaveli.widget.SahamHorizontalWidgetProvider::class.java)
            )
            val updateIntent2 = android.content.Intent(ctx, id.aiinvest.hanaveli.widget.SahamHorizontalWidgetProvider::class.java).apply {
                action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, sahamHorizontalAppWidgetIds)
            }
            ctx.sendBroadcast(updateIntent2)
            appWidgetManager.notifyAppWidgetViewDataChanged(sahamHorizontalAppWidgetIds, id.aiinvest.hanaveli.R.id.widget_grid)
        }
    }
}
