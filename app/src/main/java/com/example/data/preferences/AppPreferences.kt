package com.example.data.preferences

import android.content.Context
import android.text.format.DateFormat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.util.BanglaPeriodStyle
import com.example.util.FormatOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "writing_assistant_prefs")

data class AppSettings(
    val isMasterEnabled: Boolean = true,
    val buttonSizeDp: Int = 40,
    val buttonOpacity: Float = 0.85f,
    val buttonSide: String = "right", // "left", "right"
    val buttonPositionRatioY: Float = 0.50f,
    val defaultDateFormat: String = "dd/MM/yyyy",
    val defaultDayFormat: String = "EEEE",
    val defaultTimeFormat: String = "hh:mm a",
    val customDateFormat: String = "",
    val customDayFormat: String = "",
    val customTimeFormat: String = "",
    val isBangla: Boolean = false,
    val isSavedTextsCollapsed: Boolean = false,
    val menuMaxHeightPercent: Int = 60,
    val themeMode: String = "system", // "system", "light", "dark"
    val formattingStyle: String = "html", // "html" (Rich HTML), "unicode" (Unicode styled)
    val formatBoldPresets: Set<String> = emptySet(),
    val formatUnderlinePresets: Set<String> = emptySet(),
    val startOnBoot: Boolean = true
) {
    fun getDateFormatOptions() = FormatOptions(
        isBanglaText = isBangla,
        isBanglaDigits = isBangla
    )

    fun getDayFormatOptions() = FormatOptions(
        isBanglaText = isBangla,
        isBanglaDigits = isBangla
    )

    fun getTimeFormatOptions() = FormatOptions(
        isBanglaText = isBangla,
        isBanglaDigits = isBangla,
        banglaPeriodStyle = BanglaPeriodStyle.PERIOD
    )

    fun isAppAllowed(packageName: String): Boolean {
        return packageName.isNotEmpty() && packageName != "com.android.systemui"
    }

    fun isPresetBold(pattern: String): Boolean {
        return formatBoldPresets.contains(pattern)
    }

    fun isPresetUnderline(pattern: String): Boolean {
        return formatUnderlinePresets.contains(pattern)
    }
}

class AppPreferences(private val context: Context) {

    private val dataStore = context.applicationContext.dataStore
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _currentSnapshot = MutableStateFlow(
        AppSettings(
            defaultTimeFormat = if (DateFormat.is24HourFormat(context)) "HH:mm" else "hh:mm a"
        )
    )
    val settingsState = _currentSnapshot.asStateFlow()

    val currentSnapshot: AppSettings
        get() = _currentSnapshot.value

    companion object {
        @Volatile
        private var INSTANCE: AppPreferences? = null

        fun getInstance(context: Context): AppPreferences {
            return INSTANCE ?: synchronized(this) {
                val instance = AppPreferences(context)
                INSTANCE = instance
                instance
            }
        }

        private val KEY_MASTER_ENABLED = booleanPreferencesKey("master_enabled")
        private val KEY_BUTTON_SIZE = intPreferencesKey("button_size_dp")
        private val KEY_BUTTON_OPACITY = floatPreferencesKey("button_opacity")
        private val KEY_BUTTON_SIDE = stringPreferencesKey("button_side")
        private val KEY_BUTTON_RATIO_Y = floatPreferencesKey("button_ratio_y")
        private val KEY_DATE_FORMAT = stringPreferencesKey("date_format")
        private val KEY_DAY_FORMAT = stringPreferencesKey("day_format")
        private val KEY_TIME_FORMAT = stringPreferencesKey("time_format")
        private val KEY_CUSTOM_DATE = stringPreferencesKey("custom_date")
        private val KEY_CUSTOM_DAY = stringPreferencesKey("custom_day")
        private val KEY_CUSTOM_TIME = stringPreferencesKey("custom_time")
        private val KEY_IS_BANGLA = booleanPreferencesKey("is_bangla")
        private val KEY_SAVED_TEXTS_COLLAPSED = booleanPreferencesKey("saved_texts_collapsed")
        private val KEY_MENU_MAX_HEIGHT_PERCENT = intPreferencesKey("menu_max_height_percent")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_FORMATTING_STYLE = stringPreferencesKey("formatting_style")
        private val KEY_FORMAT_BOLD_PRESETS = stringSetPreferencesKey("format_bold_presets")
        private val KEY_FORMAT_UNDERLINE_PRESETS = stringSetPreferencesKey("format_underline_presets")
        private val KEY_START_ON_BOOT = booleanPreferencesKey("start_on_boot")
    }

    init {
        val initialDefaultTime = if (DateFormat.is24HourFormat(context)) "HH:mm" else "hh:mm a"
        scope.launch {
            dataStore.data.collect { prefs ->
                val settings = AppSettings(
                    isMasterEnabled = prefs[KEY_MASTER_ENABLED] ?: true,
                    buttonSizeDp = prefs[KEY_BUTTON_SIZE] ?: 40,
                    buttonOpacity = prefs[KEY_BUTTON_OPACITY] ?: 0.85f,
                    buttonSide = prefs[KEY_BUTTON_SIDE] ?: "right",
                    buttonPositionRatioY = prefs[KEY_BUTTON_RATIO_Y] ?: 0.50f,
                    defaultDateFormat = prefs[KEY_DATE_FORMAT] ?: "dd/MM/yyyy",
                    defaultDayFormat = prefs[KEY_DAY_FORMAT] ?: "EEEE",
                    defaultTimeFormat = prefs[KEY_TIME_FORMAT] ?: initialDefaultTime,
                    customDateFormat = prefs[KEY_CUSTOM_DATE] ?: "",
                    customDayFormat = prefs[KEY_CUSTOM_DAY] ?: "",
                    customTimeFormat = prefs[KEY_CUSTOM_TIME] ?: "",
                    isBangla = prefs[KEY_IS_BANGLA] ?: false,
                    isSavedTextsCollapsed = prefs[KEY_SAVED_TEXTS_COLLAPSED] ?: false,
                    menuMaxHeightPercent = prefs[KEY_MENU_MAX_HEIGHT_PERCENT] ?: 60,
                    themeMode = prefs[KEY_THEME_MODE] ?: "system",
                    formattingStyle = prefs[KEY_FORMATTING_STYLE] ?: "html",
                    formatBoldPresets = prefs[KEY_FORMAT_BOLD_PRESETS] ?: emptySet(),
                    formatUnderlinePresets = prefs[KEY_FORMAT_UNDERLINE_PRESETS] ?: emptySet(),
                    startOnBoot = prefs[KEY_START_ON_BOOT] ?: true
                )
                _currentSnapshot.value = settings
            }
        }
    }

    suspend fun setMasterEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_MASTER_ENABLED] = enabled }
    }

    suspend fun setButtonSize(sizeDp: Int) {
        dataStore.edit { it[KEY_BUTTON_SIZE] = sizeDp }
    }

    suspend fun setButtonOpacity(opacity: Float) {
        dataStore.edit { it[KEY_BUTTON_OPACITY] = opacity }
    }

    suspend fun setButtonSide(side: String) {
        dataStore.edit { it[KEY_BUTTON_SIDE] = side }
    }

    suspend fun setButtonPositionRatioY(ratio: Float) {
        dataStore.edit { it[KEY_BUTTON_RATIO_Y] = ratio }
    }

    suspend fun setDefaultDateFormat(format: String) {
        dataStore.edit { it[KEY_DATE_FORMAT] = format }
    }

    suspend fun setDefaultDayFormat(format: String) {
        dataStore.edit { it[KEY_DAY_FORMAT] = format }
    }

    suspend fun setDefaultTimeFormat(format: String) {
        dataStore.edit { it[KEY_TIME_FORMAT] = format }
    }

    suspend fun setCustomDateFormat(format: String) {
        dataStore.edit { it[KEY_CUSTOM_DATE] = format }
    }

    suspend fun setCustomDayFormat(format: String) {
        dataStore.edit { it[KEY_CUSTOM_DAY] = format }
    }

    suspend fun setCustomTimeFormat(format: String) {
        dataStore.edit { it[KEY_CUSTOM_TIME] = format }
    }

    suspend fun setIsBangla(isBangla: Boolean) {
        dataStore.edit { it[KEY_IS_BANGLA] = isBangla }
    }

    suspend fun setSavedTextsCollapsed(collapsed: Boolean) {
        dataStore.edit { it[KEY_SAVED_TEXTS_COLLAPSED] = collapsed }
    }

    suspend fun setMenuMaxHeightPercent(percent: Int) {
        dataStore.edit { it[KEY_MENU_MAX_HEIGHT_PERCENT] = percent }
    }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[KEY_THEME_MODE] = mode }
    }

    suspend fun setFormattingStyle(style: String) {
        dataStore.edit { it[KEY_FORMATTING_STYLE] = style }
    }

    suspend fun togglePresetBold(pattern: String) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_FORMAT_BOLD_PRESETS]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(pattern)) {
                current.remove(pattern)
            } else {
                current.add(pattern)
            }
            prefs[KEY_FORMAT_BOLD_PRESETS] = current
        }
    }

    suspend fun togglePresetUnderline(pattern: String) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_FORMAT_UNDERLINE_PRESETS]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(pattern)) {
                current.remove(pattern)
            } else {
                current.add(pattern)
            }
            prefs[KEY_FORMAT_UNDERLINE_PRESETS] = current
        }
    }

    suspend fun setStartOnBoot(enabled: Boolean) {
        dataStore.edit { it[KEY_START_ON_BOOT] = enabled }
    }
}
