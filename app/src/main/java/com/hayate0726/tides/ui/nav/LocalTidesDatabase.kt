package com.hayate0726.tides.ui.nav

import androidx.compose.runtime.compositionLocalOf
import com.hayate0726.tides.data.TidesDatabase

/**
 * CompositionLocal carrying the currently-unlocked TidesDatabase down through
 * the Main route's subtree. Read by feature screens (Calendar / Log / Stats /
 * Settings) to construct their plain-Kotlin ViewModels.
 *
 * Provided by TidesNavHost when AppState becomes Unlocked or UnlockedDecoy.
 * Accessing it outside an unlocked context throws (intentional — surfaces
 * the bug rather than silently constructing a half-working ViewModel).
 */
val LocalTidesDatabase = compositionLocalOf<TidesDatabase> {
    error("LocalTidesDatabase accessed outside an unlocked composition")
}
