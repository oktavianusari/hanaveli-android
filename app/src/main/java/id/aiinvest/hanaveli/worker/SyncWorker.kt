package id.aiinvest.hanaveli.worker

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import id.aiinvest.hanaveli.data.local.AppDatabase
import id.aiinvest.hanaveli.data.repository.ValasRepository
import id.aiinvest.hanaveli.widget.ValasWidgetProvider
import id.aiinvest.hanaveli.widget.BigValasWidgetProvider
import id.aiinvest.hanaveli.widget.BigHorizontalWidgetProvider
import id.aiinvest.hanaveli.R

class SyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getDatabase(context)
            val repository = ValasRepository(database.monitoredCurrencyDao(), database.transactionDao(), context)
            
            val prefs = context.getSharedPreferences("valas_settings", Context.MODE_PRIVATE)
            val rateType = prefs.getString("rate_type", "e-rate") ?: "e-rate"
            
            repository.syncRates(rateType)
            
            // Update widget after successful sync
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, ValasWidgetProvider::class.java)
            )
            
            val updateIntent = android.content.Intent(context, ValasWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            }
            context.sendBroadcast(updateIntent)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list)

            // Update Big Screen Widget
            val bigAppWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, BigValasWidgetProvider::class.java)
            )
            val bigUpdateIntent = android.content.Intent(context, BigValasWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, bigAppWidgetIds)
            }
            context.sendBroadcast(bigUpdateIntent)
            appWidgetManager.notifyAppWidgetViewDataChanged(bigAppWidgetIds, R.id.widget_grid)
            
            // Update Big Screen Horizontal Widget
            val bigHorizontalAppWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, BigHorizontalWidgetProvider::class.java)
            )
            val bigHorizontalUpdateIntent = android.content.Intent(context, BigHorizontalWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, bigHorizontalAppWidgetIds)
            }
            context.sendBroadcast(bigHorizontalUpdateIntent)
            appWidgetManager.notifyAppWidgetViewDataChanged(bigHorizontalAppWidgetIds, R.id.widget_grid)
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        fun enqueue(context: Context, intervalMinutes: Long) {
            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()

            val workRequest = androidx.work.PeriodicWorkRequestBuilder<SyncWorker>(
                intervalMinutes,
                java.util.concurrent.TimeUnit.MINUTES
            )
            .setConstraints(constraints)
            .build()

            androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "ValasSyncWork",
                androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }
    }
}
