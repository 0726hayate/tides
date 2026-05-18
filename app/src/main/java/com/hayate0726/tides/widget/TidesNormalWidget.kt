package com.hayate0726.tides.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.hayate0726.tides.MainActivity
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class TidesNormalWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetSummary.read(context)
        provideContent { Content(snapshot) }
    }

    @Composable
    private fun Content(snapshot: WidgetSummary.Snapshot?) {
        val bg = ColorProvider(day = BG_LIGHT, night = BG_DARK)
        val fg = ColorProvider(day = FG_LIGHT, night = FG_DARK)
        val muted = ColorProvider(day = MUTED_LIGHT, night = MUTED_DARK)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(bg)
                .cornerRadius(20.dp)
                .clickable(actionStartActivity<MainActivity>())
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start,
        ) {
            if (snapshot == null || snapshot.cycleDay <= 0) {
                Text(
                    "Tides",
                    style = TextStyle(color = fg, fontSize = 16.sp, fontWeight = FontWeight.Medium),
                )
                Text(
                    "Tap to open",
                    style = TextStyle(color = muted, fontSize = 11.sp),
                )
                return@Column
            }
            Text(
                "Day ${snapshot.cycleDay}",
                style = TextStyle(color = fg, fontSize = 22.sp, fontWeight = FontWeight.Bold),
            )
            Spacer(GlanceModifier.size(4.dp))
            val today = LocalDate.now()
            val predictedDays = snapshot.predictedPeriodStartEpochDay?.let {
                ChronoUnit.DAYS.between(today, LocalDate.ofEpochDay(it))
            }
            if (predictedDays != null) {
                Text(
                    when {
                        predictedDays > 0 -> "Period in $predictedDays d"
                        predictedDays == 0L -> "Period today"
                        else -> "Period ${-predictedDays} d late"
                    },
                    style = TextStyle(color = muted, fontSize = 11.sp),
                )
            }
            if (snapshot.showOvulation && snapshot.ovulationDateEpochDay != null) {
                val days = ChronoUnit.DAYS.between(
                    today, LocalDate.ofEpochDay(snapshot.ovulationDateEpochDay),
                )
                Text(
                    when {
                        days > 0 -> "Fertile in $days d"
                        days == 0L -> "Fertile today"
                        else -> "Fertile ${-days} d ago"
                    },
                    style = TextStyle(color = muted, fontSize = 11.sp),
                )
            }
        }
    }
}

class TidesNormalWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TidesNormalWidget()
}

private val BG_LIGHT = Color(0xFFFFF8F1)
private val BG_DARK = Color(0xFF1A1A1A)
private val FG_LIGHT = Color(0xFF1A1A1A)
private val FG_DARK = Color(0xFFFFF8F1)
private val MUTED_LIGHT = Color(0xFF6E6358)
private val MUTED_DARK = Color(0xFFB6AEA4)
