package com.hayate0726.tides.ui.settings

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppearanceRepository @Inject constructor(
    @ApplicationContext ctx: Context,
) {
    private val sp: SharedPreferences =
        ctx.getSharedPreferences("tides_appearance", Context.MODE_PRIVATE)

    private val _useDynamicColor = MutableStateFlow(sp.getBoolean(KEY, false))
    val useDynamicColor: StateFlow<Boolean> = _useDynamicColor.asStateFlow()

    fun setUseDynamicColor(v: Boolean) {
        sp.edit().putBoolean(KEY, v).apply()
        _useDynamicColor.value = v
    }

    companion object { private const val KEY = "use_dynamic_color" }
}
