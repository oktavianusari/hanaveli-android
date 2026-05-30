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

class ValasWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return ValasWidgetRemoteViewsFactory(this.applicationContext)
    }
}

class ValasWidgetRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var currencies: List<CurrencyData> = emptyList()
    private lateinit var repository: ValasRepository

    override fun onCreate() {
        val database = AppDatabase.getDatabase(context)
        repository = ValasRepository(database.monitoredCurrencyDao(), database.transactionDao(), context)
    }

    override fun onDataSetChanged() {
        // Fetch data synchronously for the widget
        runBlocking {
            currencies = repository.getMonitoredCurrenciesWithData().first()
        }
    }

    override fun onDestroy() {
        currencies = emptyList()
    }

    override fun getCount(): Int = currencies.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position >= currencies.size) return RemoteViews(context.packageName, R.layout.widget_list_item)
        
        val currency = currencies[position]
        val emoji = getFlagEmojiForCurrency(currency.currencyCode)
        
        val views = RemoteViews(context.packageName, R.layout.widget_list_item)
        views.setTextViewText(R.id.item_currency, "$emoji ${currency.currencyCode}")
        views.setTextViewText(R.id.item_rate, String.format("IDR %,.2f", currency.currentRate))

        val prefs = context.getSharedPreferences("valas_settings", Context.MODE_PRIVATE)
        val appTheme = prefs.getString("app_theme", "system") ?: "system"
        val isDark = when (appTheme) {
            "light" -> false
            "dark" -> true
            else -> {
                val currentNightMode = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        }
        val textColor = androidx.core.content.ContextCompat.getColor(context, if (isDark) R.color.widget_text_dark else R.color.widget_text_light)
        views.setTextColor(R.id.item_currency, textColor)
        views.setTextColor(R.id.item_rate, textColor)
        
        val gainPercent = if (currency.totalCost > 0) (currency.totalGain / currency.totalCost) * 100 else 0.0
        val gainText = if (currency.totalGain >= 0) "+Rp${String.format("%,.0f", currency.totalGain).replace(',', '.')} (+${String.format("%.1f", gainPercent).replace('.', ',')}%)" else "-Rp${String.format("%,.0f", -currency.totalGain).replace(',', '.')} (${String.format("%.1f", -gainPercent).replace('.', ',')}%)"
        views.setTextViewText(R.id.item_gain, gainText)
        
        val gainBgResource = if (currency.totalGain >= 0) R.drawable.bg_pill_gain else R.drawable.bg_pill_loss
        views.setInt(R.id.item_gain, "setBackgroundResource", gainBgResource)

        val percentChange = if (currency.baseRateToday > 0) ((currency.currentRate - currency.baseRateToday) / currency.baseRateToday) * 100 else 0.0
        val percentText = if (percentChange >= 0) String.format("+%.2f%%", percentChange) else String.format("%.2f%%", percentChange)
        val percentTextColor = if (percentChange >= 0) android.graphics.Color.parseColor("#388E3C") else android.graphics.Color.parseColor("#D32F2F")
        
        views.setTextViewText(R.id.item_percent, percentText)
        views.setTextColor(R.id.item_percent, percentTextColor)

        val fillInIntent = Intent().apply {
            putExtra("currency", currency.currencyCode)
        }
        views.setOnClickFillInIntent(R.id.widget_list_item_root, fillInIntent)
        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
}
