package id.aiinvest.hanaveli.ui.viewmodel

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.content.SharedPreferences
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class SettingsViewModel(private val context: Context) : ViewModel() {
    private val prefs = context.getSharedPreferences("valas_settings", Context.MODE_PRIVATE)

    private val _rateType = MutableStateFlow(prefs.getString("rate_type", "e-rate") ?: "e-rate")
    val rateType: StateFlow<String> = _rateType.asStateFlow()

    private val _rateDirection = MutableStateFlow(prefs.getString("rate_direction", "jual") ?: "jual")
    val rateDirection: StateFlow<String> = _rateDirection.asStateFlow()

    private val _emasRateDirection = MutableStateFlow(prefs.getString("emas_rate_direction", "jual") ?: "jual")
    val emasRateDirection: StateFlow<String> = _emasRateDirection.asStateFlow()

    private val _syncInterval = MutableStateFlow(prefs.getInt("sync_interval", 60))
    val syncInterval: StateFlow<Int> = _syncInterval.asStateFlow()
    private val _themePreference = MutableStateFlow(prefs.getString("theme_preference", "system") ?: "system")
    val themePreference: StateFlow<String> = _themePreference.asStateFlow()

    private val _useDynamicColor = MutableStateFlow(prefs.getBoolean("use_dynamic_color", true))
    val useDynamicColor: StateFlow<Boolean> = _useDynamicColor.asStateFlow()

    private val _language = MutableStateFlow(prefs.getString("language", "system") ?: "system")
    val language: StateFlow<String> = _language.asStateFlow()

    // Light Theme Custom Colors
    private val _lightPrimaryHex = MutableStateFlow(prefs.getString("light_primary_hex", "#386A20") ?: "#386A20")
    val lightPrimaryHex: StateFlow<String> = _lightPrimaryHex.asStateFlow()
    private val _lightSecondaryHex = MutableStateFlow(prefs.getString("light_secondary_hex", "#D7E8CD") ?: "#D7E8CD")
    val lightSecondaryHex: StateFlow<String> = _lightSecondaryHex.asStateFlow()
    private val _lightTextHex = MutableStateFlow(prefs.getString("light_text_hex", "#1A1C18") ?: "#1A1C18")
    val lightTextHex: StateFlow<String> = _lightTextHex.asStateFlow()

    // Dark Theme Custom Colors
    private val _darkPrimaryHex = MutableStateFlow(prefs.getString("dark_primary_hex", "#9CD67D") ?: "#9CD67D")
    val darkPrimaryHex: StateFlow<String> = _darkPrimaryHex.asStateFlow()
    private val _darkSecondaryHex = MutableStateFlow(prefs.getString("dark_secondary_hex", "#BBCBB1") ?: "#BBCBB1")
    val darkSecondaryHex: StateFlow<String> = _darkSecondaryHex.asStateFlow()
    private val _darkTextHex = MutableStateFlow(prefs.getString("dark_text_hex", "#E3E3DC") ?: "#E3E3DC")
    val darkTextHex: StateFlow<String> = _darkTextHex.asStateFlow()

    private val _bcaLastUpdated = MutableStateFlow(prefs.getString("bca_last_updated", "") ?: "")
    val bcaLastUpdated: StateFlow<String> = _bcaLastUpdated.asStateFlow()

    private val _pegadaianLastUpdated = MutableStateFlow(prefs.getString("pegadaian_last_updated", "") ?: "")
    val pegadaianLastUpdated: StateFlow<String> = _pegadaianLastUpdated.asStateFlow()

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == "bca_last_updated") {
            _bcaLastUpdated.value = sharedPreferences.getString(key, "") ?: ""
        }
        if (key == "pegadaian_last_updated") {
            _pegadaianLastUpdated.value = sharedPreferences.getString(key, "") ?: ""
        }
    }

    private val _overrideWidgetColor = MutableStateFlow(prefs.getBoolean("override_widget_color", false))
    val overrideWidgetColor: StateFlow<Boolean> = _overrideWidgetColor.asStateFlow()

    private val _widgetBgHex = MutableStateFlow(prefs.getString("widget_bg_hex", "#000000") ?: "#000000")
    val widgetBgHex: StateFlow<String> = _widgetBgHex.asStateFlow()

    private val _widgetOpacity = MutableStateFlow(prefs.getFloat("widget_opacity", 0.5f))
    val widgetOpacity: StateFlow<Float> = _widgetOpacity.asStateFlow()

    private val _enableWidgetShowHide = MutableStateFlow(prefs.getBoolean("enable_widget_show_hide", true))
    val enableWidgetShowHide: StateFlow<Boolean> = _enableWidgetShowHide.asStateFlow()

    private val _autoRateBca = MutableStateFlow(prefs.getBoolean("auto_rate_bca", false))
    val autoRateBca: StateFlow<Boolean> = _autoRateBca.asStateFlow()

    fun updateWidgetSettings(override: Boolean, bgHex: String, opacity: Float) {
        prefs.edit {
            putBoolean("override_widget_color", override)
            putString("widget_bg_hex", bgHex)
            putFloat("widget_opacity", opacity)
        }
        _overrideWidgetColor.value = override
        _widgetBgHex.value = bgHex
        _widgetOpacity.value = opacity

        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
        val intent1 = android.content.Intent(context, id.aiinvest.hanaveli.widget.ValasWidgetProvider::class.java).apply {
            action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = appWidgetManager.getAppWidgetIds(android.content.ComponentName(context, id.aiinvest.hanaveli.widget.ValasWidgetProvider::class.java))
            putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        context.sendBroadcast(intent1)

        val intent2 = android.content.Intent(context, id.aiinvest.hanaveli.widget.BigValasWidgetProvider::class.java).apply {
            action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = appWidgetManager.getAppWidgetIds(android.content.ComponentName(context, id.aiinvest.hanaveli.widget.BigValasWidgetProvider::class.java))
            putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        context.sendBroadcast(intent2)

        val intent3 = android.content.Intent(context, id.aiinvest.hanaveli.widget.BigHorizontalWidgetProvider::class.java).apply {
            action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = appWidgetManager.getAppWidgetIds(android.content.ComponentName(context, id.aiinvest.hanaveli.widget.BigHorizontalWidgetProvider::class.java))
            putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        context.sendBroadcast(intent3)
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    fun updateRateType(type: String) {
        prefs.edit { putString("rate_type", type) }
        _rateType.value = type
        
        val refreshIntent = android.content.Intent(context, id.aiinvest.hanaveli.widget.ValasWidgetProvider::class.java).apply {
            action = id.aiinvest.hanaveli.widget.ValasWidgetProvider.ACTION_REFRESH
        }
        context.sendBroadcast(refreshIntent)
    }

    fun updateRateDirection(direction: String) {
        prefs.edit { putString("rate_direction", direction) }
        _rateDirection.value = direction
        
        val refreshIntent = android.content.Intent(context, id.aiinvest.hanaveli.widget.ValasWidgetProvider::class.java).apply {
            action = id.aiinvest.hanaveli.widget.ValasWidgetProvider.ACTION_REFRESH
        }
        context.sendBroadcast(refreshIntent)
    }

    fun updateEmasRateDirection(direction: String) {
        prefs.edit { putString("emas_rate_direction", direction) }
        _emasRateDirection.value = direction
        
        val refreshIntent = android.content.Intent(context, id.aiinvest.hanaveli.widget.ValasWidgetProvider::class.java).apply {
            action = id.aiinvest.hanaveli.widget.ValasWidgetProvider.ACTION_REFRESH
        }
        context.sendBroadcast(refreshIntent)
    }

    fun updateEnableWidgetShowHide(enabled: Boolean) {
        prefs.edit { putBoolean("enable_widget_show_hide", enabled) }
        _enableWidgetShowHide.value = enabled
        
        // Refresh Widgets to apply visibility changes
        val valasIntent = android.content.Intent(context, id.aiinvest.hanaveli.widget.ValasWidgetProvider::class.java).apply {
            action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(android.content.ComponentName(context, id.aiinvest.hanaveli.widget.ValasWidgetProvider::class.java))
            putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        context.sendBroadcast(valasIntent)
        
        val sahamIntent = android.content.Intent(context, id.aiinvest.hanaveli.widget.SahamWidgetProvider::class.java).apply {
            action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(android.content.ComponentName(context, id.aiinvest.hanaveli.widget.SahamWidgetProvider::class.java))
            putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        context.sendBroadcast(sahamIntent)
    }

    fun updateAutoRateBca(enabled: Boolean) {
        prefs.edit { putBoolean("auto_rate_bca", enabled) }
        _autoRateBca.value = enabled
        
        // Refresh widget immediately to apply rate changes if any
        val refreshIntent = android.content.Intent(context, id.aiinvest.hanaveli.widget.ValasWidgetProvider::class.java).apply {
            action = id.aiinvest.hanaveli.widget.ValasWidgetProvider.ACTION_REFRESH
        }
        context.sendBroadcast(refreshIntent)
    }

    fun updateTheme(theme: String) {
        prefs.edit().putString("theme_preference", theme).apply()
        _themePreference.value = theme
    }

    fun updateUseDynamicColor(value: Boolean) {
        prefs.edit().putBoolean("use_dynamic_color", value).apply()
        _useDynamicColor.value = value
    }

    fun updateSyncInterval(interval: Int) {
        prefs.edit { putInt("sync_interval", interval) }
        _syncInterval.value = interval
        id.aiinvest.hanaveli.worker.SyncWorker.enqueue(context, interval.toLong())
    }

    fun updateLanguage(lang: String) {
        prefs.edit { putString("language", lang) }
        _language.value = lang
    }

    fun updateLightColors(primary: String, secondary: String, text: String) {
        prefs.edit {
            putString("light_primary_hex", primary)
            putString("light_secondary_hex", secondary)
            putString("light_text_hex", text)
        }
        _lightPrimaryHex.value = primary
        _lightSecondaryHex.value = secondary
        _lightTextHex.value = text
    }

    fun updateDarkColors(primary: String, secondary: String, text: String) {
        prefs.edit {
            putString("dark_primary_hex", primary)
            putString("dark_secondary_hex", secondary)
            putString("dark_text_hex", text)
        }
        _darkPrimaryHex.value = primary
        _darkSecondaryHex.value = secondary
        _darkTextHex.value = text
    }

    private val _isSensitiveDataVisible = MutableStateFlow(false)
    val isSensitiveDataVisible: StateFlow<Boolean> = _isSensitiveDataVisible.asStateFlow()
    
    private var visibilityJob: Job? = null

    fun toggleSensitiveDataVisibility() {
        val newState = !_isSensitiveDataVisible.value
        _isSensitiveDataVisible.value = newState
        
        visibilityJob?.cancel()
        
        if (newState) {
            visibilityJob = viewModelScope.launch {
                delay(60000)
                _isSensitiveDataVisible.value = false
            }
        }
    }
}

class SettingsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
