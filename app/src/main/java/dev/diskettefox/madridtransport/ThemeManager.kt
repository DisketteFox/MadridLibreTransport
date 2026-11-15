package dev.diskettefox.madridtransport

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

object ThemeManager {
    private const val PREFS_NAME = "ThemePrefs"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_OLED_MODE = "oled_mode"

    private var initialized = false

    var themeMode by mutableStateOf(ThemeMode.SYSTEM)
        private set

    var isOledMode by mutableStateOf(false)
        private set

    fun ensureInitialized(context: Context) {
        if (!initialized) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val modeStr = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
            themeMode = try { ThemeMode.valueOf(modeStr) } catch (_: Exception) { ThemeMode.SYSTEM }
            isOledMode = prefs.getBoolean(KEY_OLED_MODE, false)
            initialized = true
        }
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        ensureInitialized(context)
        themeMode = mode
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_THEME_MODE, mode.name) }
    }

    fun setOledMode(context: Context, enabled: Boolean) {
        ensureInitialized(context)
        isOledMode = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_OLED_MODE, enabled) }
    }
}
