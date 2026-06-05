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
        const val ACTION_TOGGLE_SENSITIVE = "id.aiinvest.hanaveli.widget.ACTION_TOGGLE_SENSITIVE_VALAS"

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

            val toggleIntent = Intent(context, ValasWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_SENSITIVE
            }
            val togglePendingIntent = PendingIntent.getBroadcast(context, 0, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_toggle_sensitive, togglePendingIntent)
            
            val enableShowHide = prefs.getBoolean("enable_widget_show_hide", true)
            if (!enableShowHide) {
                views.setViewVisibility(R.id.widget_toggle_sensitive, android.view.View.GONE)
            } else {
                views.setViewVisibility(R.id.widget_toggle_sensitive, android.view.View.VISIBLE)
                val isSensitive = prefs.getBoolean("widget_is_sensitive_visible", false)
                views.setImageViewResource(R.id.widget_toggle_sensitive, if (isSensitive) R.drawable.ic_eye_open else R.drawable.ic_eye_closed)
            }

            val appTheme = prefs.getString("app_theme", "system") ?: "system"
            
            val isOverride = prefs.getBoolean("override_widget_color", false)

            var finalBgColor = android.graphics.Color.parseColor("#151517")
            val isDark = if (isOverride) {
                val hex = prefs.getString("widget_bg_hex", "#000000") ?: "#000000"
                val opacity = prefs.getFloat("widget_opacity", 0.5f)
                val parsedColor = try { android.graphics.Color.parseColor(hex) } catch(e: Exception) { android.graphics.Color.BLACK }
                val alphaInt = (opacity * 255).toInt()
                finalBgColor = (parsedColor and 0x00FFFFFF) or (alphaInt shl 24)
                androidx.core.graphics.ColorUtils.calculateLuminance(finalBgColor) < 0.5
            } else {
                val darkTheme = when (appTheme) {
                    "dark" -> true
                    "light" -> false
                    else -> {
                        val currentNightMode = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                        currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
                    }
                }
                finalBgColor = if (darkTheme) android.graphics.Color.parseColor("#151517") else android.graphics.Color.parseColor("#FFFFFF")
                darkTheme
            }

            views.setInt(R.id.widget_bg_shape, "setColorFilter", finalBgColor)

            if (isDark) {
                views.setTextColor(R.id.widget_title, androidx.core.content.ContextCompat.getColor(context, R.color.widget_text_dark))
                views.setTextColor(R.id.widget_timestamp, androidx.core.content.ContextCompat.getColor(context, R.color.widget_text_dark))
            } else {
                views.setTextColor(R.id.widget_title, androidx.core.content.ContextCompat.getColor(context, R.color.widget_text_light))
                views.setTextColor(R.id.widget_timestamp, androidx.core.content.ContextCompat.getColor(context, R.color.widget_text_light))
            }

            val titleStr = "Valas Hari Ini"
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

            id.aiinvest.hanaveli.worker.SyncWorker.enqueueImmediate(context)
        } else if (intent.action == ACTION_TOGGLE_SENSITIVE) {
            val prefs = context.getSharedPreferences("valas_settings", Context.MODE_PRIVATE)
            val isVisible = prefs.getBoolean("widget_is_sensitive_visible", false)
            prefs.edit().putBoolean("widget_is_sensitive_visible", !isVisible).apply()
            
            if (!isVisible) {
                val workRequest = androidx.work.OneTimeWorkRequestBuilder<id.aiinvest.hanaveli.worker.WidgetHideWorker>()
                    .setInitialDelay(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
                    "WidgetHideWork",
                    androidx.work.ExistingWorkPolicy.REPLACE,
                    workRequest
                )
            } else {
                androidx.work.WorkManager.getInstance(context).cancelUniqueWork("WidgetHideWork")
            }
            
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = android.content.ComponentName(context, ValasWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            onUpdate(context, appWidgetManager, appWidgetIds)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
}
