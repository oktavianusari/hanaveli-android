package id.aiinvest.hanaveli.worker

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import id.aiinvest.hanaveli.R
import id.aiinvest.hanaveli.widget.*

class WidgetHideWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = context.getSharedPreferences("valas_settings", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("widget_is_sensitive_visible", false).apply()

        val appWidgetManager = AppWidgetManager.getInstance(context)

        // Update all widget providers
        val providers = listOf(
            ValasWidgetProvider::class.java,
            BigValasWidgetProvider::class.java,
            BigHorizontalWidgetProvider::class.java,
            SahamWidgetProvider::class.java,
            SahamHorizontalWidgetProvider::class.java
        )

        providers.forEach { providerClass ->
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, providerClass))
            val intent = android.content.Intent(context, providerClass).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
            
            // Also notify list views to re-render data with Rp ----
            // We use R.id.widget_list and R.id.widget_grid interchangeably based on the layout,
            // but notifyAppWidgetViewDataChanged just sends a broadcast so it's safe to call for both
            appWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
            appWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.widget_grid)
        }

        return Result.success()
    }
}
