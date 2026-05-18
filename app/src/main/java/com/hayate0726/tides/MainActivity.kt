package com.hayate0726.tides

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hayate0726.tides.ui.nav.TidesNavHost
import com.hayate0726.tides.ui.settings.AppearanceRepository
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
            TidesTheme(useDynamicColor = useDynamic) {
                TidesNavHost()
            }
        }
    }
}
