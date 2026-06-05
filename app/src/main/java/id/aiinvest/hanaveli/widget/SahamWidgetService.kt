package id.aiinvest.hanaveli.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.graphics.ColorUtils
import id.aiinvest.hanaveli.R
import id.aiinvest.hanaveli.data.local.AppDatabase
import id.aiinvest.hanaveli.data.local.entity.MonitoredStock
import kotlinx.coroutines.runBlocking
import java.text.NumberFormat
import java.util.Locale

class SahamWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return SahamWidgetFactory(this.applicationContext)
    }
}

class SahamWidgetFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private var stocks: List<MonitoredStock> = emptyList()
    // Map untuk menyimpan Gain (Profit/Loss Nominal) per saham berdasarkan riwayat
    private val totalGainMap = mutableMapOf<String, Double>()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        val database = AppDatabase.getDatabase(context)
        runBlocking {
            stocks = database.stockDao().getAllStocksSync()
            val transactions = database.sahamTransactionDao().getAllTransactionsSync()
            
            // Hitung Average Cost dan Gain per saham
            totalGainMap.clear()
            for (stock in stocks) {
                var totalLot = 0
                var totalCost = 0.0
                
                // Urutkan transaksi saham tertentu
                val symbolTrans = transactions.filter { it.symbol == stock.symbol }
                for (t in symbolTrans) {
                    if (t.type == "BUY") {
                        totalLot += t.lot
                        totalCost += (t.price * t.lot * 100)
                    } else if (t.type == "SELL") {
                        if (totalLot > 0) {
                            val avgPrice = totalCost / (totalLot * 100)
                            totalCost -= (avgPrice * t.lot * 100)
                        }
                        totalLot -= t.lot
                        if (totalLot <= 0) {
                            totalLot = 0
                            totalCost = 0.0
                        }
                    }
                }
                
                if (totalLot > 0) {
                    val currentValue = stock.price * totalLot * 100
                    val gain = currentValue - totalCost
                    totalGainMap[stock.symbol] = gain
                } else {
                    totalGainMap[stock.symbol] = 0.0
                }
            }
        }
    }

    override fun onDestroy() {
        stocks = emptyList()
        totalGainMap.clear()
    }

    override fun getCount(): Int = stocks.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position >= count) return RemoteViews(context.packageName, R.layout.widget_saham_item)

        val stock = stocks[position]
        val rv = RemoteViews(context.packageName, R.layout.widget_saham_item)

        val formatIdr = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        val gain = totalGainMap[stock.symbol] ?: 0.0

        // Set Teks persis seperti struktur Valas
        rv.setTextViewText(R.id.item_currency, stock.symbol)
        
        val sharedPreferences = context.getSharedPreferences("valas_settings", Context.MODE_PRIVATE)
        val isSensitiveVisible = sharedPreferences.getBoolean("widget_is_sensitive_visible", false)
        if (isSensitiveVisible) {
            rv.setTextViewText(R.id.item_rate, String.format("%,.2f", stock.price))
            val percentText = if (stock.changePercent >= 0) String.format("+%.2f%%", stock.changePercent) else String.format("%.2f%%", stock.changePercent)
            rv.setTextViewText(R.id.item_percent, percentText)
            val gainText = if (gain >= 0) "+ Rp${String.format("%,.0f", gain).replace(',', '.')}" else "- Rp${String.format("%,.0f", -gain).replace(',', '.')}"
            rv.setTextViewText(R.id.item_gain, gainText)
        } else {
            rv.setTextViewText(R.id.item_rate, "----")
            rv.setTextViewText(R.id.item_percent, "----%")
            rv.setTextViewText(R.id.item_gain, "Rp ----")
        }

        // SMART ADAPTIVE READABILITY
        val appTheme = sharedPreferences.getString("app_theme", "system") ?: "system"
        val isOverride = sharedPreferences.getBoolean("override_widget_color", false)

        var finalBgColor = Color.parseColor("#151517")
        val isDark = if (isOverride) {
            val hex = sharedPreferences.getString("widget_bg_hex", "#000000") ?: "#000000"
            val parsedColor = try { Color.parseColor(hex) } catch(e: Exception) { Color.BLACK }
            ColorUtils.calculateLuminance(parsedColor) < 0.5
        } else {
            val darkTheme = when (appTheme) {
                "dark" -> true
                "light" -> false
                else -> {
                    val currentNightMode = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                    currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
                }
            }
            darkTheme
        }

        if (isDark) {
            rv.setTextColor(R.id.item_currency, Color.WHITE)
            rv.setTextColor(R.id.item_rate, Color.parseColor("#CCCCCC"))
            rv.setTextColor(R.id.item_percent, if (stock.changePercent >= 0) Color.parseColor("#81C784") else Color.parseColor("#E57373"))
        } else {
            rv.setTextColor(R.id.item_currency, Color.BLACK)
            rv.setTextColor(R.id.item_rate, Color.parseColor("#555555"))
            rv.setTextColor(R.id.item_percent, if (stock.changePercent >= 0) Color.parseColor("#388E3C") else Color.parseColor("#D32F2F"))
        }
        
        // Pilled Gain logic
        if (gain >= 0) {
            rv.setInt(R.id.item_gain, "setBackgroundResource", R.drawable.pill_bg_green)
            rv.setTextColor(R.id.item_gain, Color.WHITE)
        } else {
            rv.setInt(R.id.item_gain, "setBackgroundResource", R.drawable.pill_bg_red)
            rv.setTextColor(R.id.item_gain, Color.WHITE)
        }

        val fillInIntent = Intent()
        rv.setOnClickFillInIntent(R.id.widget_list_item_root, fillInIntent)
        return rv
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
}
