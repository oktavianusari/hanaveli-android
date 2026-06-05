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

class SahamWidgetProvider : AppWidgetProvider() {
    companion object {
        const val ACTION_REFRESH = "id.aiinvest.hanaveli.widget.ACTION_REFRESH_SAHAM"
        const val ACTION_TOGGLE_SENSITIVE = "id.aiinvest.hanaveli.widget.ACTION_TOGGLE_SENSITIVE_SAHAM"

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_saham)
            
            val intent = Intent(context, SahamWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }

            views.setRemoteAdapter(R.id.widget_list, intent)
            views.setEmptyView(R.id.widget_list, R.id.widget_empty_view)

            val appIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, appIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_title, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_empty_view, pendingIntent)
            views.setPendingIntentTemplate(R.id.widget_list, pendingIntent)

            val refreshIntent = Intent(context, SahamWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)
            
            val sharedPreferences = context.getSharedPreferences("valas_settings", Context.MODE_PRIVATE)

            val toggleIntent = Intent(context, SahamWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_SENSITIVE
            }
            val togglePendingIntent = PendingIntent.getBroadcast(context, 0, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_toggle_sensitive, togglePendingIntent)
            
            val isSensitive = sharedPreferences.getBoolean("widget_is_sensitive_visible", false)
            views.setImageViewResource(R.id.widget_toggle_sensitive, if (isSensitive) R.drawable.ic_eye_open else R.drawable.ic_eye_closed)

            val appTheme = sharedPreferences.getString("app_theme", "system") ?: "system"
            val isOverride = sharedPreferences.getBoolean("override_widget_color", false)

            var finalBgColor = android.graphics.Color.parseColor("#151517")
            val isDark = if (isOverride) {
                val hex = sharedPreferences.getString("widget_bg_hex", "#000000") ?: "#000000"
                val opacity = sharedPreferences.getFloat("widget_opacity", 0.5f)
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
                views.setTextColor(R.id.widget_empty_view, android.graphics.Color.WHITE)
            } else {
                views.setTextColor(R.id.widget_title, androidx.core.content.ContextCompat.getColor(context, R.color.widget_text_light))
                views.setTextColor(R.id.widget_timestamp, androidx.core.content.ContextCompat.getColor(context, R.color.widget_text_light))
                views.setTextColor(R.id.widget_empty_view, android.graphics.Color.BLACK)
            }
            
            val sahamLastUpdated = sharedPreferences.getString("saham_last_updated", "") ?: ""
            if (sahamLastUpdated.isNotEmpty()) {
                views.setTextViewText(R.id.widget_timestamp, "Yahoo Finance: $sahamLastUpdated")
            } else {
                views.setTextViewText(R.id.widget_timestamp, "Menunggu Sinkronisasi...")
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
            val componentName = android.content.ComponentName(context, SahamWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_saham)
                views.setViewVisibility(R.id.widget_refresh_button, android.view.View.GONE)
                views.setViewVisibility(R.id.widget_progress_bar, android.view.View.VISIBLE)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }

            // Gunakan OneTimeWorkRequest untuk immediate fetch
            id.aiinvest.hanaveli.worker.SyncWorker.enqueueImmediate(context)
        } else if (intent.action == ACTION_TOGGLE_SENSITIVE) {
            val prefs = context.getSharedPreferences("valas_settings", Context.MODE_PRIVATE)
            val isVisible = prefs.getBoolean("widget_is_sensitive_visible", false)
            prefs.edit().putBoolean("widget_is_sensitive_visible", !isVisible).apply()
            
            if (!isVisible) {
                val workRequest = androidx.work.OneTimeWorkRequestBuilder<id.aiinvest.hanaveli.worker.WidgetHideWorker>()
                    .setInitialDelay(1, java.util.concurrent.TimeUnit.MINUTES)
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
            val componentName = android.content.ComponentName(context, SahamWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            onUpdate(context, appWidgetManager, appWidgetIds)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list)
        } else if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = android.content.ComponentName(context, SahamWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
}
