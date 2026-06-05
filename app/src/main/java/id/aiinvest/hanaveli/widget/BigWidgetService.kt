package id.aiinvest.hanaveli.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import id.aiinvest.hanaveli.R
import id.aiinvest.hanaveli.data.local.AppDatabase
import id.aiinvest.hanaveli.data.repository.ValasRepository
import id.aiinvest.hanaveli.domain.model.CurrencyData
import id.aiinvest.hanaveli.ui.utils.getFlagEmojiForCurrency
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class BigWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return BigWidgetRemoteViewsFactory(this.applicationContext)
    }
}

class BigWidgetRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var currencies: List<CurrencyData> = emptyList()
    private lateinit var repository: ValasRepository

    override fun onCreate() {
        val database = AppDatabase.getDatabase(context)
        repository = ValasRepository(database.monitoredCurrencyDao(), database.transactionDao(), context)
    }

    override fun onDataSetChanged() {
        runBlocking {
            currencies = repository.getMonitoredCurrenciesWithData().first()
        }
    }

    override fun onDestroy() {
        currencies = emptyList()
    }

    override fun getCount(): Int = currencies.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position >= currencies.size) return RemoteViews(context.packageName, R.layout.widget_big_item)
        
        val currency = currencies[position]
        val emoji = getFlagEmojiForCurrency(currency.currencyCode)
        
        val views = RemoteViews(context.packageName, R.layout.widget_big_item)
        
        // Asumsi data
        val gainPercent = if (currency.totalCost > 0) (currency.totalGain / currency.totalCost) * 100 else 0.0
        val percentChange = if (currency.baseRateToday > 0) ((currency.currentRate - currency.baseRateToday) / currency.baseRateToday) * 100 else 0.0
        
        val isGreen = currency.totalGain >= 0
        
        // Background kotak
        views.setInt(R.id.widget_big_item_root, "setBackgroundResource", if (isGreen) R.drawable.widget_item_bg_green else R.drawable.widget_item_bg_red)
        
        // Bendera emoji
        views.setTextViewText(R.id.item_flag, emoji)
        
        val prefs = context.getSharedPreferences("valas_settings", Context.MODE_PRIVATE)
        val isSensitiveVisible = prefs.getBoolean("widget_is_sensitive_visible", false)
        
        if (isSensitiveVisible) {
            views.setTextViewText(R.id.item_rate, "${currency.currencyCode}/IDR ${String.format("%,.0f", currency.currentRate).replace(',', '.')}")
            val percentText = if (percentChange >= 0) String.format("+ %.2f%%", percentChange).replace('.', ',') else String.format("- %.2f%%", -percentChange).replace('.', ',')
            views.setTextViewText(R.id.item_percent, percentText)
            val gainText = if (isGreen) "+ Rp${String.format("%,.0f", currency.totalGain).replace(',', '.')} (${String.format("%.1f", gainPercent).replace('.', ',')}%)" else "- Rp${String.format("%,.0f", -currency.totalGain).replace(',', '.')} (${String.format("%.1f", -gainPercent).replace('.', ',')}%)"
            views.setTextViewText(R.id.item_gain, gainText)
        } else {
            views.setTextViewText(R.id.item_rate, "${currency.currencyCode}/IDR ----")
            views.setTextViewText(R.id.item_percent, "----%")
            views.setTextViewText(R.id.item_gain, "Rp ----")
        }
        val appTheme = prefs.getString("app_theme", "system") ?: "system"
        val isOverride = prefs.getBoolean("override_widget_color", false)
        val isDark = if (isOverride) {
            val hex = prefs.getString("widget_bg_hex", "#000000") ?: "#000000"
            val opacity = prefs.getFloat("widget_opacity", 0.5f)
            val parsedColor = try { android.graphics.Color.parseColor(hex) } catch(e: Exception) { android.graphics.Color.BLACK }
            val alphaInt = (opacity * 255).toInt()
            val finalBgColor = (parsedColor and 0x00FFFFFF) or (alphaInt shl 24)
            androidx.core.graphics.ColorUtils.calculateLuminance(finalBgColor) < 0.5
        } else {
            when (appTheme) {
                "light" -> false
                "dark" -> true
                else -> {
                    val currentNightMode = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                    currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
                }
            }
        }
        val textColor = androidx.core.content.ContextCompat.getColor(context, if (isDark) R.color.widget_text_dark else R.color.widget_text_light)
        views.setTextColor(R.id.item_rate, textColor)
        
        // Gain Pill Logic Only
        views.setInt(R.id.item_gain, "setBackgroundResource", if (isGreen) R.drawable.pill_bg_green else R.drawable.pill_bg_red)

        val fillInIntent = Intent().apply {
            putExtra("currency", currency.currencyCode)
        }
        views.setOnClickFillInIntent(R.id.widget_big_item_root, fillInIntent)
        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
}
