package com.hayate0726.tides.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SymptomTest {

    @Test
    fun `curated taxonomy contains every spec symptom`() {
        val expected = setOf(
            // Pain
            Symptom.CRAMPS, Symptom.HEADACHE, Symptom.MIGRAINE, Symptom.BACK_PAIN,
            Symptom.BREAST_TENDERNESS, Symptom.JOINT_PAIN, Symptom.ABDOMINAL_PAIN,
            // Mood
            Symptom.ANXIOUS, Symptom.IRRITABLE, Symptom.SAD, Symptom.HAPPY,
            Symptom.SENSITIVE, Symptom.CALM,
            // Energy
            Symptom.TIRED, Symptom.ENERGETIC, Symptom.RESTLESS, Symptom.FOGGY,
            // Body
            Symptom.BLOATING, Symptom.ACNE, Symptom.HOT_FLASHES, Symptom.CHILLS,
            Symptom.DIZZINESS, Symptom.NAUSEA, Symptom.VOMITING,
            // Digestive
            Symptom.CONSTIPATION, Symptom.DIARRHEA, Symptom.GAS,
            Symptom.INCREASED_APPETITE, Symptom.DECREASED_APPETITE, Symptom.CRAVINGS,
            // Sleep
            Symptom.INSOMNIA, Symptom.OVERSLEEPING, Symptom.VIVID_DREAMS, Symptom.NIGHT_SWEATS,
            // Discharge
            Symptom.DRY, Symptom.STICKY, Symptom.CREAMY, Symptom.WATERY,
            Symptom.EGG_WHITE, Symptom.BROWN, Symptom.SPOTTING,
            // Sex
            Symptom.HIGH_LIBIDO, Symptom.LOW_LIBIDO, Symptom.PAINFUL_SEX,
            // Other (note)
            Symptom.OTHER,
        )
        assertEquals(expected, Symptom.entries.toSet())
    }

    @Test
    fun `every SymptomCategory has at least one Symptom`() {
        val populated = Symptom.entries.map { it.category }.toSet()
        assertEquals(SymptomCategory.entries.toSet(), populated)
    }

    @Test
    fun `OTHER is the only symptom that is freetext`() {
        val freetext = Symptom.entries.filter { it.isFreeText }
        assertEquals(listOf(Symptom.OTHER), freetext)
    }
}
