package com.hayate0726.tides.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.hayate0726.tides.MainActivity

/**
 * Discreet variant per spec §5.13: shows the current cycle-day number on a
 * neutral background, with no labels. Never displays the words "period,"
 * "cycle," or a predicted date. Tapping the widget opens the app.
 *
 * The "Normal" variant (cycle day + predicted date) is intentionally deferred
 * — landing it requires writing the prediction into [WidgetSummary] and a
 * Settings toggle to pick variants. The Discreet widget alone gets users
 * something useful at the home screen without expanding the unencrypted
 * summary file's surface area.
 */
class TidesDiscreetWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetSummary.read(context)
        provideContent { Content(snapshot) }
    }

    @Composable
    private fun Content(snapshot: WidgetSummary.Snapshot?) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(day = BackgroundLight, night = BackgroundDark))
                .cornerRadius(20.dp)
                .clickable(actionStartActivity<MainActivity>())
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            val foreground = ColorProvider(day = OnBackgroundLight, night = OnBackgroundDark)
            if (snapshot == null || snapshot.cycleDay <= 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Tides",
                        style = TextStyle(
                            color = foreground,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        snapshot.cycleDay.toString(),
                        style = TextStyle(
                            color = foreground,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
        }
    }
}

class TidesDiscreetWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TidesDiscreetWidget()
}

private val BackgroundLight = Color(0xFFFFF8F1)
private val BackgroundDark = Color(0xFF1A1A1A)
private val OnBackgroundLight = Color(0xFF1A1A1A)
private val OnBackgroundDark = Color(0xFFFFF8F1)
