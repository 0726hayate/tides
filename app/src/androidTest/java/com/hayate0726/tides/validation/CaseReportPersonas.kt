package com.hayate0726.tides.validation

import com.hayate0726.tides.data.entity.BirthControlEntity
import com.hayate0726.tides.data.entity.CycleEntryEntity
import com.hayate0726.tides.data.entity.SymptomEntryEntity
import com.hayate0726.tides.domain.model.BirthControlMethod
import com.hayate0726.tides.domain.model.FlowIntensity
import com.hayate0726.tides.domain.model.Goal
import com.hayate0726.tides.domain.model.Symptom
import java.time.LocalDate

/**
 * Hand-transcribed clinical case histories. Each Persona's data comes from
 * a publicly accessible case report or ACOG Practice Bulletin example.
 * Sources are cited above each definition.
 *
 * Dates are normalized so that the most recent recorded event in each
 * history falls within the 12 months preceding 2026-05-01 — this places
 * the histories in a consistent timeframe for the test harness.
 */
object CaseReportPersonas {

    private val today: LocalDate = LocalDate.of(2026, 5, 1)

    /**
     * Build a multi-day bleeding episode anchored at [start], with the given
     * length and flow intensity. Helper for transcribing case histories where
     * an "episode from day X for N days" pattern recurs.
     */
    private fun episode(
        start: LocalDate,
        days: Int,
        flow: FlowIntensity,
    ): List<CycleEntryEntity> =
        (0 until days).map { d ->
            CycleEntryEntity(
                date = start.plusDays(d.toLong()),
                flowIntensity = flow,
                painSeverity = null,
                notes = null,
            )
        }

    /**
     * SOURCE: Solanki A. "Polycystic Ovarian Syndrome: An Autobiographical
     * Case Report of an Often Overlooked Disorder." Cureus 2022;14(1):e21068.
     * DOI: 10.7759/cureus.21068. PMC: PMC8835484.
     *
     * Patient: female physician, late 20s, PCOS undiagnosed until age 28
     * despite irregular menses dating from menarche. Cycles 45-90+ days with
     * occasional 4-6 month gaps; bleeding occasionally prolonged (>7 days)
     * and heavy.
     *
     * NOTE: The research doc gives only descriptive ranges ("45-90+ days")
     * rather than a dated sequence, so the six bleeding episodes below are
     * reconstructed from those ranges using cycle lengths drawn from
     * {45, 60, 50, 75, 55, 90}. The most recent bleed is normalized to
     * 45 days before 2026-05-01.
     */
    private val casePcosLateDx: Persona = Persona(
        id = "CASE_PCOS_LATE_DX",
        spec = null,
        cycleEntries = listOf(
            // Episode 1: today - 330d, 5 days, MEDIUM
            episode(today.minusDays(330), 5, FlowIntensity.MEDIUM),
            // Episode 2: cycle len 45 -> today - 285d, 6 days, HEAVY (prolonged)
            episode(today.minusDays(285), 6, FlowIntensity.HEAVY),
            // Episode 3: cycle len 60 -> today - 225d, 5 days, MEDIUM
            episode(today.minusDays(225), 5, FlowIntensity.MEDIUM),
            // Episode 4: cycle len 50 -> today - 175d, 8 days, HEAVY (prolonged)
            episode(today.minusDays(175), 8, FlowIntensity.HEAVY),
            // Episode 5: cycle len 75 -> today - 100d, 6 days, MEDIUM
            episode(today.minusDays(100), 6, FlowIntensity.MEDIUM),
            // Episode 6: cycle len 55 -> today - 45d, 5 days, LIGHT
            episode(today.minusDays(45), 5, FlowIntensity.LIGHT),
        ).flatten(),
        symptomEntries = listOf(
            SymptomEntryEntity(date = today.minusDays(280), symptom = Symptom.ACNE, severity = 2, otherText = null),
            SymptomEntryEntity(date = today.minusDays(200), symptom = Symptom.ACNE, severity = 2, otherText = null),
            SymptomEntryEntity(date = today.minusDays(120), symptom = Symptom.ACNE, severity = 2, otherText = null),
            SymptomEntryEntity(date = today.minusDays(120), symptom = Symptom.TIRED, severity = 1, otherText = null),
            SymptomEntryEntity(date = today.minusDays(60), symptom = Symptom.TIRED, severity = 1, otherText = null),
            SymptomEntryEntity(date = today.minusDays(45), symptom = Symptom.IRRITABLE, severity = 1, otherText = null),
        ),
        goals = setOf(Goal.TRYING_TO_CONCEIVE),
        birthControl = null,
    )

    /**
     * SOURCE: Harlow SD et al. STRAW+10 framework. "Executive summary of the
     * Stages of Reproductive Aging Workshop + 10." Fertil Steril
     * 2012;97:843. DOI: 10.1016/j.fertnstert.2012.01.128. Substituted from
     * ACOG Practice Bulletin 141 illustrative perimenopause transition (per
     * research doc; no openly accessible single-patient case report was
     * available covering the full transition pattern).
     *
     * Patient: age 48, G2P2, BMI 26, perimenopausal. Baseline cycles 28
     * days. Over 24 months, cycle lengths trend: 28 -> 26 -> 24 -> 22
     * (Stage -3a shortening) -> 35 -> 21 -> 40 -> 18 (Stage -2 ≥7-day
     * variability) -> 65 -> 25 -> 90 (Stage -1 skipped cycles ≥60d).
     * Bleeding occasionally heavy 8+ days during anovulatory cycles.
     * Most recent bleed normalized to today - 45d.
     */
    private val casePerimenopauseTransition: Persona = Persona(
        id = "CASE_PERIMENOPAUSE_TRANSITION",
        spec = null,
        cycleEntries = listOf(
            // Stage -3a shortening: 28d baseline
            episode(today.minusDays(439), 5, FlowIntensity.MEDIUM),
            // gap 28 -> -411
            episode(today.minusDays(411), 5, FlowIntensity.MEDIUM),
            // gap 26 -> -385
            episode(today.minusDays(385), 4, FlowIntensity.MEDIUM),
            // gap 24 -> -361
            episode(today.minusDays(361), 4, FlowIntensity.MEDIUM),
            // Stage -2 variability begins. gap 22 -> -339
            episode(today.minusDays(339), 5, FlowIntensity.MEDIUM),
            // gap 35 -> -304
            episode(today.minusDays(304), 4, FlowIntensity.LIGHT),
            // gap 21 -> -283
            episode(today.minusDays(283), 6, FlowIntensity.HEAVY),
            // gap 40 -> -243
            episode(today.minusDays(243), 5, FlowIntensity.MEDIUM),
            // Stage -1: skipped cycles. gap 18 -> -225
            episode(today.minusDays(225), 8, FlowIntensity.HEAVY),
            // gap 65 -> -160
            episode(today.minusDays(160), 5, FlowIntensity.MEDIUM),
            // gap 25 -> -135
            episode(today.minusDays(135), 8, FlowIntensity.HEAVY),
            // gap 90 -> -45 (most recent)
            episode(today.minusDays(45), 6, FlowIntensity.MEDIUM),
        ).flatten(),
        symptomEntries = listOf(
            SymptomEntryEntity(date = today.minusDays(360), symptom = Symptom.HOT_FLASHES, severity = 1, otherText = null),
            SymptomEntryEntity(date = today.minusDays(300), symptom = Symptom.HOT_FLASHES, severity = 2, otherText = null),
            SymptomEntryEntity(date = today.minusDays(240), symptom = Symptom.NIGHT_SWEATS, severity = 2, otherText = null),
            SymptomEntryEntity(date = today.minusDays(180), symptom = Symptom.INSOMNIA, severity = 2, otherText = null),
            SymptomEntryEntity(date = today.minusDays(120), symptom = Symptom.HOT_FLASHES, severity = 2, otherText = null),
            SymptomEntryEntity(date = today.minusDays(90), symptom = Symptom.IRRITABLE, severity = 1, otherText = null),
            SymptomEntryEntity(date = today.minusDays(60), symptom = Symptom.NIGHT_SWEATS, severity = 2, otherText = null),
            SymptomEntryEntity(date = today.minusDays(30), symptom = Symptom.HOT_FLASHES, severity = 2, otherText = null),
        ),
        goals = setOf(Goal.TRACK_PERIOD),
        birthControl = null,
    )

    /**
     * SOURCE: Campbell OM, Gray RH. "Characteristics and determinants of
     * postpartum ovarian function in women in the United States." Am J
     * Obstet Gynecol 1993;169:55. DOI: 10.1016/0002-9378(93)90131-2.
     * Supplemented with WHO Multinational Study of Breast-feeding and
     * Lactational Amenorrhea, Fertil Steril 1998;70:461.
     * DOI: 10.1016/s0015-0282(98)00191-5.
     *
     * Patient: age 31, recent G1P1, exclusively breastfeeding. Reference
     * delivery date 2025-01-15. Lochial bleeding through day 35
     * (2025-02-19), then complete amenorrhea during exclusive breastfeeding.
     * Solids introduced month 6 (2025-07-15). First spotting day 245
     * (2025-09-17, 1 day — not counted as menses per LAM criteria). First
     * true menses day 285 (2025-10-27, 6 days). Second menses 38 days later
     * (2025-12-04, 7 days). Subsequent cycles normalized to ~30 days.
     *
     * Dates from the research doc are kept as-is; the most recent bleed in
     * the constructed timeline (2026-04-03) is within 12 months of
     * 2026-05-01.
     */
    private val casePostpartumLactational: Persona = Persona(
        id = "CASE_POSTPARTUM_LACTATIONAL",
        spec = null,
        cycleEntries = listOf(
            // Lochial bleeding 2025-01-15 through 2025-02-19 — represent as a
            // continuous decreasing-intensity tail across the first 35 days.
            // Use a sampled subset rather than 35 daily rows; the harness
            // treats consecutive entries as a single bleeding episode.
            episode(LocalDate.of(2025, 1, 15), 7, FlowIntensity.HEAVY),
            episode(LocalDate.of(2025, 1, 22), 7, FlowIntensity.MEDIUM),
            episode(LocalDate.of(2025, 1, 29), 7, FlowIntensity.LIGHT),
            episode(LocalDate.of(2025, 2, 5), 7, FlowIntensity.LIGHT),
            episode(LocalDate.of(2025, 2, 12), 8, FlowIntensity.SPOTTING),
            // First spotting at day 245 (2025-09-17), 1 day, not menses
            episode(LocalDate.of(2025, 9, 17), 1, FlowIntensity.SPOTTING),
            // First true menses at day 285 (2025-10-27), 6 days
            episode(LocalDate.of(2025, 10, 27), 6, FlowIntensity.HEAVY),
            // Second menses 38 days later (2025-12-04), 7 days
            episode(LocalDate.of(2025, 12, 4), 7, FlowIntensity.MEDIUM),
            // Subsequent ~30-day cycles
            episode(LocalDate.of(2026, 1, 3), 6, FlowIntensity.MEDIUM),
            episode(LocalDate.of(2026, 2, 2), 5, FlowIntensity.MEDIUM),
            episode(LocalDate.of(2026, 3, 4), 5, FlowIntensity.MEDIUM),
            episode(LocalDate.of(2026, 4, 3), 5, FlowIntensity.MEDIUM),
        ).flatten(),
        symptomEntries = listOf(
            SymptomEntryEntity(date = LocalDate.of(2025, 6, 1), symptom = Symptom.BREAST_TENDERNESS, severity = 1, otherText = null),
            SymptomEntryEntity(date = LocalDate.of(2025, 9, 1), symptom = Symptom.BREAST_TENDERNESS, severity = 1, otherText = null),
            SymptomEntryEntity(date = LocalDate.of(2025, 10, 27), symptom = Symptom.CRAMPS, severity = 2, otherText = null),
            SymptomEntryEntity(date = LocalDate.of(2025, 12, 4), symptom = Symptom.CRAMPS, severity = 1, otherText = null),
        ),
        goals = setOf(Goal.TRACK_PERIOD),
        birthControl = null,
    )

    /**
     * SOURCE: Mansour D et al. "Fertility after discontinuation of
     * contraception: a comprehensive review of the literature."
     * Contraception 2011;84:465. DOI: 10.1016/j.contraception.2011.04.002.
     * Supplemented with Yland JJ et al. "Pregravid contraceptive use and
     * fecundability." BMJ 2020;371:m3966. DOI: 10.1136/bmj.m3966.
     *
     * Patient: age 29, G0, prior COCP use 6 years continuous, discontinued
     * to plan pregnancy. Last withdrawal bleed during placebo week. First
     * post-pill bleed at day 35 (3 days, mild). Second cycle 45 days,
     * anovulatory. Third cycle 38 days, ovulatory. Fourth/fifth cycles
     * 30 and 29 days. Subsequent cycles 27-32 days range.
     *
     * Most recent bleed normalized to today - 45d.
     */
    private val casePillDiscontinuation: Persona = Persona(
        id = "CASE_PILL_DISCONTINUATION",
        spec = null,
        cycleEntries = listOf(
            // B0 withdrawal bleed (placebo week, ~3 days)
            episode(today.minusDays(338), 3, FlowIntensity.LIGHT),
            // B1 first post-pill: 35 days later, mild, 3 days
            episode(today.minusDays(303), 3, FlowIntensity.LIGHT),
            // B2: +45d (anovulatory), 5 days
            episode(today.minusDays(258), 5, FlowIntensity.MEDIUM),
            // B3: +38d (ovulatory, luteal 11d), 5 days
            episode(today.minusDays(220), 5, FlowIntensity.MEDIUM),
            // B4: +30d, 5 days
            episode(today.minusDays(190), 5, FlowIntensity.MEDIUM),
            // B5: +29d, 5 days
            episode(today.minusDays(161), 5, FlowIntensity.MEDIUM),
            // B6-B9: 27-32 day range, 5 days each
            episode(today.minusDays(133), 5, FlowIntensity.MEDIUM),
            episode(today.minusDays(104), 5, FlowIntensity.MEDIUM),
            episode(today.minusDays(75), 5, FlowIntensity.MEDIUM),
            // Most recent at day -45
            episode(today.minusDays(45), 5, FlowIntensity.MEDIUM),
        ).flatten(),
        symptomEntries = listOf(
            SymptomEntryEntity(date = today.minusDays(290), symptom = Symptom.ACNE, severity = 1, otherText = null),
            SymptomEntryEntity(date = today.minusDays(260), symptom = Symptom.ACNE, severity = 1, otherText = null),
            SymptomEntryEntity(date = today.minusDays(245), symptom = Symptom.BLOATING, severity = 1, otherText = null),
            SymptomEntryEntity(date = today.minusDays(210), symptom = Symptom.EGG_WHITE, severity = 1, otherText = null),
            SymptomEntryEntity(date = today.minusDays(180), symptom = Symptom.EGG_WHITE, severity = 1, otherText = null),
            SymptomEntryEntity(date = today.minusDays(150), symptom = Symptom.ABDOMINAL_PAIN, severity = 1, otherText = null),
            SymptomEntryEntity(date = today.minusDays(120), symptom = Symptom.EGG_WHITE, severity = 1, otherText = null),
            SymptomEntryEntity(date = today.minusDays(90), symptom = Symptom.EGG_WHITE, severity = 1, otherText = null),
        ),
        goals = setOf(Goal.TRYING_TO_CONCEIVE),
        birthControl = null,
    )

    /**
     * SOURCE: ACOG Practice Bulletin 136 "Management of Abnormal Uterine
     * Bleeding Associated With Ovulatory Dysfunction" 2013, reaffirmed 2019,
     * illustrative AUB-O presentation. Substituted from a single-patient
     * case report because no openly accessible non-PCOS chronic anovulation
     * case was located (per research doc).
     *
     * Patient: age 34, BMI 31, no PCOS criteria, normal thyroid/prolactin.
     * Cycles 20-50 days, occasional prolonged 12-day bleeding episodes.
     * Mid-cycle BBT failed to confirm ovulation in 3 of 4 documented cycles.
     *
     * NOTE: The research doc provides only the descriptive 20-50 day range,
     * not a dated sequence. The four bleeding episodes below are
     * reconstructed from that range using cycle lengths drawn from
     * {25, 35, 50, 30}. The most recent bleed is normalized to 45 days
     * before 2026-05-01.
     */
    private val caseAnovulatory: Persona = Persona(
        id = "CASE_ANOVULATORY",
        spec = null,
        cycleEntries = listOf(
            // B1: start (cycle of 25 leads into it from prior unobserved bleed)
            episode(today.minusDays(160), 5, FlowIntensity.MEDIUM),
            // B2: +35d, 12-day prolonged bleeding episode
            episode(today.minusDays(125), 12, FlowIntensity.HEAVY),
            // B3: +50d, 5 days
            episode(today.minusDays(75), 5, FlowIntensity.MEDIUM),
            // B4: +30d -> most recent at -45d, 6 days
            episode(today.minusDays(45), 6, FlowIntensity.MEDIUM),
        ).flatten(),
        symptomEntries = listOf(
            SymptomEntryEntity(date = today.minusDays(160), symptom = Symptom.TIRED, severity = 1, otherText = null),
            SymptomEntryEntity(date = today.minusDays(120), symptom = Symptom.TIRED, severity = 2, otherText = null),
            SymptomEntryEntity(date = today.minusDays(75), symptom = Symptom.TIRED, severity = 1, otherText = null),
            SymptomEntryEntity(date = today.minusDays(45), symptom = Symptom.TIRED, severity = 1, otherText = null),
        ),
        goals = setOf(Goal.TRACK_PERIOD),
        birthControl = null,
    )

    /**
     * SOURCE: Madden T et al. "Treatment of unacceptable bleeding in
     * long-term users of 52-mg levonorgestrel intrauterine device: a
     * prospective observational study." Contraception 2024.
     * PMC: PMC12008142.
     *
     * Patient: age 35, Mirena 52-mg LNG-IUS in place 14 months. Near-daily
     * spotting in first 3 months post-insertion (~50 bleed/spotting days in
     * 90-day window). Months 4-9: gradual reduction to 1-2 spotting episodes
     * per month. Months 10-14: re-emergence of irregular spotting,
     * ~12 bleed/spotting days per 90-day window.
     *
     * IUD insertion at today - 14 months ≈ today - 420 days. Bleeding
     * pattern transcribed as representative sparse entries reflecting each
     * phase.
     */
    private val caseIudBreakthrough: Persona = Persona(
        id = "CASE_IUD_BREAKTHROUGH",
        spec = null,
        cycleEntries = listOf(
            // Months 1-3 post-insertion: frequent spotting (days -420 .. -330)
            episode(today.minusDays(418), 3, FlowIntensity.SPOTTING),
            episode(today.minusDays(410), 4, FlowIntensity.LIGHT),
            episode(today.minusDays(400), 3, FlowIntensity.SPOTTING),
            episode(today.minusDays(390), 5, FlowIntensity.SPOTTING),
            episode(today.minusDays(378), 2, FlowIntensity.LIGHT),
            episode(today.minusDays(370), 4, FlowIntensity.SPOTTING),
            episode(today.minusDays(360), 3, FlowIntensity.SPOTTING),
            episode(today.minusDays(350), 2, FlowIntensity.SPOTTING),
            episode(today.minusDays(340), 3, FlowIntensity.SPOTTING),
            // Months 4-9: 1-2 episodes/month (days -330 .. -150)
            episode(today.minusDays(320), 2, FlowIntensity.SPOTTING),
            episode(today.minusDays(295), 2, FlowIntensity.SPOTTING),
            episode(today.minusDays(270), 2, FlowIntensity.SPOTTING),
            episode(today.minusDays(245), 1, FlowIntensity.SPOTTING),
            episode(today.minusDays(220), 2, FlowIntensity.SPOTTING),
            episode(today.minusDays(195), 1, FlowIntensity.SPOTTING),
            episode(today.minusDays(170), 2, FlowIntensity.SPOTTING),
            // Months 10-14: re-emergence (days -150 .. 0), ~12 days/90d
            episode(today.minusDays(140), 3, FlowIntensity.SPOTTING),
            episode(today.minusDays(115), 2, FlowIntensity.SPOTTING),
            episode(today.minusDays(90), 3, FlowIntensity.LIGHT),
            episode(today.minusDays(65), 2, FlowIntensity.SPOTTING),
            episode(today.minusDays(40), 2, FlowIntensity.SPOTTING),
            episode(today.minusDays(15), 2, FlowIntensity.SPOTTING),
        ).flatten(),
        symptomEntries = listOf(
            SymptomEntryEntity(date = today.minusDays(410), symptom = Symptom.CRAMPS, severity = 1, otherText = null),
            SymptomEntryEntity(date = today.minusDays(390), symptom = Symptom.CRAMPS, severity = 1, otherText = null),
            SymptomEntryEntity(date = today.minusDays(140), symptom = Symptom.CRAMPS, severity = 1, otherText = null),
            SymptomEntryEntity(date = today.minusDays(90), symptom = Symptom.CRAMPS, severity = 1, otherText = null),
            SymptomEntryEntity(date = today.minusDays(40), symptom = Symptom.CRAMPS, severity = 1, otherText = null),
        ),
        goals = setOf(Goal.TRACK_PERIOD),
        birthControl = BirthControlEntity(
            method = BirthControlMethod.HORMONAL_IUD,
            startDate = today.minusDays(420),
            endDate = null,
        ),
    )

    /**
     * SOURCE: Jensen JT et al. "Contraceptive efficacy and safety of the
     * 52-mg levonorgestrel intrauterine system for up to 8 years: findings
     * from the Mirena Extension Trial." Am J Obstet Gynecol 2022.
     * DOI: 10.1016/j.ajog.2022.09.007.
     *
     * Patient: age 38, Mirena in place 5 years 4 months. Adjustment-phase
     * spotting tapered off; last documented bleeding episode month 13
     * post-insertion. Complete amenorrhea sustained from month 13 to
     * present (>4 years continuous). No spotting, no withdrawal bleeds.
     *
     * Per the task spec, this persona has no cycle entries and no symptom
     * entries — only the active hormonal-IUD birth control row. The IUD
     * start date is placed >18 months before 2026-05-01 (here 5 years 4
     * months = ~1947 days) to match the source case.
     */
    private val caseIudAmenorrhea: Persona = Persona(
        id = "CASE_IUD_AMENORRHEA",
        spec = null,
        cycleEntries = emptyList(),
        symptomEntries = emptyList(),
        goals = setOf(Goal.TRACK_PERIOD),
        birthControl = BirthControlEntity(
            method = BirthControlMethod.HORMONAL_IUD,
            startDate = today.minusMonths(64),  // ~5 years 4 months
            endDate = null,
        ),
    )

    /**
     * SOURCE: Cheung VYT, Goodman SR. "Retained Intrauterine Device (IUD):
     * Triple Case Report and Review of the Literature." Case Reports in
     * Obstetrics and Gynecology 2018;2018:9362962.
     * DOI: 10.1155/2018/9362962. Supplemented with ACOG Committee Opinion
     * 672 partial-expulsion management illustrative pattern.
     *
     * Patient: age 27, Mirena placed 18 months prior, previously achieved
     * oligomenorrhea (~1 bleed every 3 months). Months 6-17 of insertion:
     * established near-amenorrhea. Month 18: new-onset crampy heavier
     * bleeding episode lasting 9 days, then irregular spotting and
     * intermittent heavier episodes (3 episodes over the next 2 months).
     * Ultrasound revealed partial IUD displacement.
     *
     * IUD placed at today - 540 days. The 9-day return-of-bleeding episode
     * is placed at today - 90 days, with three follow-up episodes within
     * the subsequent 2 months. Most recent bleed within 12 months of
     * 2026-05-01.
     */
    private val caseIudExpulsionReturn: Persona = Persona(
        id = "CASE_IUD_EXPULSION_RETURN",
        spec = null,
        cycleEntries = listOf(
            // Months 1-5 adjustment-phase spotting (very sparse)
            episode(today.minusDays(530), 2, FlowIntensity.SPOTTING),
            episode(today.minusDays(490), 2, FlowIntensity.SPOTTING),
            episode(today.minusDays(450), 1, FlowIntensity.SPOTTING),
            // Months 6-17 near-amenorrhea: ~1 bleed every 3 months, very light
            episode(today.minusDays(360), 2, FlowIntensity.LIGHT),
            episode(today.minusDays(270), 2, FlowIntensity.LIGHT),
            episode(today.minusDays(180), 1, FlowIntensity.SPOTTING),
            // Month 18 — return of crampy heavier bleeding, 9 days
            episode(today.minusDays(90), 9, FlowIntensity.HEAVY),
            // Following 2 months: 3 irregular episodes
            episode(today.minusDays(60), 3, FlowIntensity.LIGHT),
            episode(today.minusDays(40), 5, FlowIntensity.MEDIUM),
            episode(today.minusDays(15), 3, FlowIntensity.LIGHT),
        ).flatten(),
        symptomEntries = listOf(
            SymptomEntryEntity(date = today.minusDays(90), symptom = Symptom.CRAMPS, severity = 2, otherText = null),
            SymptomEntryEntity(date = today.minusDays(60), symptom = Symptom.CRAMPS, severity = 1, otherText = null),
            SymptomEntryEntity(date = today.minusDays(40), symptom = Symptom.CRAMPS, severity = 2, otherText = null),
            SymptomEntryEntity(date = today.minusDays(15), symptom = Symptom.CRAMPS, severity = 1, otherText = null),
        ),
        goals = setOf(Goal.AVOID_PREGNANCY),
        birthControl = BirthControlEntity(
            method = BirthControlMethod.HORMONAL_IUD,
            startDate = today.minusDays(540),
            endDate = null,
        ),
    )

    val all: List<Persona> = listOf(
        casePcosLateDx,
        casePerimenopauseTransition,
        casePostpartumLactational,
        casePillDiscontinuation,
        caseAnovulatory,
        caseIudBreakthrough,
        caseIudAmenorrhea,
        caseIudExpulsionReturn,
    )
}
