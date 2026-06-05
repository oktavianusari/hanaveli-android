package id.aiinvest.hanaveli.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import id.aiinvest.hanaveli.MainActivity
import id.aiinvest.hanaveli.R

class SahamHorizontalWidgetProvider : AppWidgetProvider() {
    companion object {
        const val ACTION_REFRESH = "id.aiinvest.hanaveli.widget.ACTION_REFRESH_SAHAM_HORIZONTAL"

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_saham_horizontal)
            
            val intent = Intent(context, SahamWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }

            // Note: Uses the same SahamWidgetService because the item layout is the same!
            views.setRemoteAdapter(R.id.widget_grid, intent)
            views.setEmptyView(R.id.widget_grid, R.id.widget_empty_view)

            val appIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, appIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_title, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_empty_view, pendingIntent)
            views.setPendingIntentTemplate(R.id.widget_grid, pendingIntent)

            val refreshIntent = Intent(context, SahamHorizontalWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)
            
            val sharedPreferences = context.getSharedPreferences("valas_settings", Context.MODE_PRIVATE)
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
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_grid)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = android.content.ComponentName(context, SahamHorizontalWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_saham_horizontal)
                views.setViewVisibility(R.id.widget_refresh_button, android.view.View.GONE)
                views.setViewVisibility(R.id.widget_progress_bar, android.view.View.VISIBLE)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }

            id.aiinvest.hanaveli.worker.SyncWorker.enqueueImmediate(context)
        } else if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = android.content.ComponentName(context, SahamHorizontalWidgetProvider::class.java)
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
