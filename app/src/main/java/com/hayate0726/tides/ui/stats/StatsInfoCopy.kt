package com.hayate0726.tides.ui.stats

data class SheetCopy(
    val title: String,
    val body: String,
    val sources: List<String> = emptyList(),
)

object StatsInfoCopy {

    val cycleLength = SheetCopy(
        title = "Cycle length",
        body = "A cycle is measured from the first day of one period to the first day " +
            "of the next. Typical adult range is 21–35 days. A few days of variation is " +
            "normal; consistent shifts of 7+ days may be worth a check-in with a clinician.",
        sources = listOf(
            "FSRH 2023 — Normal Menstruation",
            "ACOG FAQ — Your Menstrual Cycle",
        ),
    )

    val periodLength = SheetCopy(
        title = "Period length",
        body = "The number of consecutive bleeding days in your period. Typical range is " +
            "2–7 days. Bleeding longer than 8 days, or that is heavier than usual for you, " +
            "may warrant a clinical check.",
        sources = listOf("NHS — Periods", "ACOG FAQ — Heavy Menstrual Bleeding"),
    )

    val symptomHeatmap = SheetCopy(
        title = "Symptom heatmap",
        body = "Darker cells show that a symptom was logged more often during that part of " +
            "your cycle. Patterns that repeat across cycles are more meaningful than " +
            "occasional single occurrences.",
    )

    val symptomFrequency = SheetCopy(
        title = "Symptom frequency",
        body = "Total days each symptom appeared in the range you're viewing. Useful for " +
            "spotting clusters or new patterns over time.",
    )

    val insightCard = SheetCopy(
        title = "About insights",
        body = "Insights are observations from your data, not medical advice. They're " +
            "generated locally on this device — nothing is uploaded.",
    )
}
