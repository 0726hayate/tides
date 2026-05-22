package com.hayate0726.tides

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hayate0726.tides.ui.nav.TidesNavHost
import com.hayate0726.tides.ui.settings.AppearanceRepository
import com.hayate0726.tides.ui.settings.ThemeMode
import com.hayate0726.tides.ui.theme.TidesTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var appearanceRepository: AppearanceRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val useDynamic by appearanceRepository.useDynamicColor.collectAsStateWithLifecycle()
            val themeMode by appearanceRepository.themeMode.collectAsStateWithLifecycle()
            val app: AppViewModel = hiltViewModel()
            val appState by app.state.collectAsStateWithLifecycle()
            val systemDark = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            // First-impression rule: onboarding always renders the cream
            // palette, regardless of phone setting, so users on system dark
            // mode don't see the OLED-monochrome dark theme before they've
            // had a chance to learn the app's visual identity. Once the user
            // is past onboarding, we respect their explicit theme choice.
            val darkTheme = if (appState is AppState.Onboarding) false else systemDark
            TidesTheme(darkTheme = darkTheme, useDynamicColor = useDynamic) {
                TidesNavHost()
            }
        }
    }
}
