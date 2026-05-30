package id.aiinvest.hanaveli.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import id.aiinvest.hanaveli.MainActivity
import id.aiinvest.hanaveli.R

class ValasWidgetProvider : AppWidgetProvider() {
    companion object {
        const val ACTION_REFRESH = "id.aiinvest.hanaveli.widget.ACTION_REFRESH"

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_valas_monitor)
            
            // Set up the intent that starts the RemoteViewsService, which will
            // provide the views for this collection.
            val intent = Intent(context, ValasWidgetService::class.java).apply {
                // Add the app widget ID to the intent extras.
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }

            views.setRemoteAdapter(R.id.widget_list, intent)
            views.setEmptyView(R.id.widget_list, R.id.widget_empty_view)

            // Intent to open MainActivity on title click
            val appIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, appIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_title, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_empty_view, pendingIntent)
            views.setPendingIntentTemplate(R.id.widget_list, pendingIntent)

            val refreshIntent = Intent(context, ValasWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)
            
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

            if (isDark) {
                views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_bg_rounded_dark)
                views.setTextColor(R.id.widget_title, androidx.core.content.ContextCompat.getColor(context, R.color.widget_text_dark))
                views.setTextColor(R.id.widget_timestamp, androidx.core.content.ContextCompat.getColor(context, R.color.widget_text_dark))
            } else {
                views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_bg_rounded_light)
                views.setTextColor(R.id.widget_title, androidx.core.content.ContextCompat.getColor(context, R.color.widget_text_light))
                views.setTextColor(R.id.widget_timestamp, androidx.core.content.ContextCompat.getColor(context, R.color.widget_text_light))
            }

            val titleStr = "Portofolio Anda Hari Ini"
            views.setTextViewText(R.id.widget_title, titleStr)
            
            val bcaLastUpdated = prefs.getString("bca_last_updated", "") ?: ""
            val pegadaianLastUpdated = prefs.getString("pegadaian_last_updated", "") ?: ""
            
            if (bcaLastUpdated.isNotEmpty() || pegadaianLastUpdated.isNotEmpty()) {
                val rawBca = bcaLastUpdated.replace(Regex("(?i)^bca\\s*:?\\s*"), "").trim()
                val bcaStr = if (rawBca.isNotEmpty()) "BCA: $rawBca" else "BCA: -"
                val rawIndogold = pegadaianLastUpdated.replace(Regex("(?i)^indogold\\s*:?\\s*"), "").trim()
                val indogoldStr = if (rawIndogold.isNotEmpty()) "Indogold: $rawIndogold" else "Indogold: -"
                val combinedText = "$bcaStr\n$indogoldStr"
                views.setTextViewText(R.id.widget_timestamp, combinedText)
            } else {
                val lastSync = prefs.getLong("last_sync_timestamp", 0L)
                if (lastSync > 0) {
                    val dateStr = java.text.SimpleDateFormat("dd MMM HH:mm", java.util.Locale.getDefault()).format(java.util.Date(lastSync))
                    val formatStr = context.getString(R.string.last_refresh)
                    views.setTextViewText(R.id.widget_timestamp, String.format(formatStr, dateStr))
                } else {
                    views.setTextViewText(R.id.widget_timestamp, "")
                }
            }

            views.setViewVisibility(R.id.widget_refresh_button, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.widget_progress_bar, android.view.View.GONE)

            appWidgetManager.updateAppWidget(appWidgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = android.content.ComponentName(context, ValasWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_valas_monitor)
                views.setViewVisibility(R.id.widget_refresh_button, android.view.View.GONE)
                views.setViewVisibility(R.id.widget_progress_bar, android.view.View.VISIBLE)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }

            val syncRequest = OneTimeWorkRequestBuilder<id.aiinvest.hanaveli.worker.SyncWorker>().build()
            WorkManager.getInstance(context).enqueue(syncRequest)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
}
