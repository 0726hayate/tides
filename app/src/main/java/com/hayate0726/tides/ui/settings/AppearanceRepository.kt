package com.hayate0726.tides.ui.settings

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-wide appearance preferences. SharedPreferences-backed so the values
 * survive process kill and are available before the encrypted DB unlocks
 * (the theme has to render before the user can enter their PIN).
 */
@Singleton
class AppearanceRepository @Inject constructor(
    @ApplicationContext ctx: Context,
) {
    private val sp: SharedPreferences =
        ctx.getSharedPreferences("tides_appearance", Context.MODE_PRIVATE)

    private val _useDynamicColor = MutableStateFlow(sp.getBoolean(KEY_DYNAMIC, false))
    val useDynamicColor: StateFlow<Boolean> = _useDynamicColor.asStateFlow()

    private val _themeMode = MutableStateFlow(readThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setUseDynamicColor(v: Boolean) {
        sp.edit().putBoolean(KEY_DYNAMIC, v).apply()
        _useDynamicColor.value = v
    }

    fun setThemeMode(m: ThemeMode) {
        sp.edit().putString(KEY_THEME_MODE, m.name).apply()
        _themeMode.value = m
    }

    private fun readThemeMode(): ThemeMode {
        val raw = sp.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        return runCatching { ThemeMode.valueOf(raw) }.getOrDefault(ThemeMode.SYSTEM)
    }

    companion object {
        private const val KEY_DYNAMIC = "use_dynamic_color"
        private const val KEY_THEME_MODE = "theme_mode"
    }
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }
