package com.hayate0726.tides

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.hayate0726.tides.ui.nav.TidesNavHost
import com.hayate0726.tides.ui.theme.TidesTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TidesTheme {
                TidesNavHost()
            }
        }
    }
}
