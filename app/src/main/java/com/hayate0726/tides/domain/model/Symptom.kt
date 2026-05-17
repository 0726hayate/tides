package com.hayate0726.tides.domain.model

enum class SymptomCategory {
    PAIN, MOOD, ENERGY, BODY, DIGESTIVE, SLEEP, DISCHARGE, SEX, OTHER,
}

enum class Symptom(
    val category: SymptomCategory,
    val isFreeText: Boolean = false,
) {
    // Pain
    CRAMPS(SymptomCategory.PAIN),
    HEADACHE(SymptomCategory.PAIN),
    MIGRAINE(SymptomCategory.PAIN),
    BACK_PAIN(SymptomCategory.PAIN),
    BREAST_TENDERNESS(SymptomCategory.PAIN),
    JOINT_PAIN(SymptomCategory.PAIN),
    ABDOMINAL_PAIN(SymptomCategory.PAIN),

    // Mood
    ANXIOUS(SymptomCategory.MOOD),
    IRRITABLE(SymptomCategory.MOOD),
    SAD(SymptomCategory.MOOD),
    HAPPY(SymptomCategory.MOOD),
    SENSITIVE(SymptomCategory.MOOD),
    CALM(SymptomCategory.MOOD),

    // Energy
    TIRED(SymptomCategory.ENERGY),
    ENERGETIC(SymptomCategory.ENERGY),
    RESTLESS(SymptomCategory.ENERGY),
    FOGGY(SymptomCategory.ENERGY),

    // Body
    BLOATING(SymptomCategory.BODY),
    ACNE(SymptomCategory.BODY),
    HOT_FLASHES(SymptomCategory.BODY),
    CHILLS(SymptomCategory.BODY),
    DIZZINESS(SymptomCategory.BODY),
    NAUSEA(SymptomCategory.BODY),
    VOMITING(SymptomCategory.BODY),

    // Digestive
    CONSTIPATION(SymptomCategory.DIGESTIVE),
    DIARRHEA(SymptomCategory.DIGESTIVE),
    GAS(SymptomCategory.DIGESTIVE),
    INCREASED_APPETITE(SymptomCategory.DIGESTIVE),
    DECREASED_APPETITE(SymptomCategory.DIGESTIVE),
    CRAVINGS(SymptomCategory.DIGESTIVE),

    // Sleep
    INSOMNIA(SymptomCategory.SLEEP),
    OVERSLEEPING(SymptomCategory.SLEEP),
    VIVID_DREAMS(SymptomCategory.SLEEP),
    NIGHT_SWEATS(SymptomCategory.SLEEP),

    // Discharge
    DRY(SymptomCategory.DISCHARGE),
    STICKY(SymptomCategory.DISCHARGE),
    CREAMY(SymptomCategory.DISCHARGE),
    WATERY(SymptomCategory.DISCHARGE),
    EGG_WHITE(SymptomCategory.DISCHARGE),
    BROWN(SymptomCategory.DISCHARGE),
    SPOTTING(SymptomCategory.DISCHARGE),

    // Sex
    HIGH_LIBIDO(SymptomCategory.SEX),
    LOW_LIBIDO(SymptomCategory.SEX),
    PAINFUL_SEX(SymptomCategory.SEX),

    // Other (note) — free-text escape hatch
    OTHER(SymptomCategory.OTHER, isFreeText = true);
}
