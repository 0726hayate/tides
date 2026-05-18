package com.hayate0726.tides.ui.export

import com.hayate0726.tides.domain.FigoAnalysis
import org.junit.jupiter.api.Test

class DoctorPdfStructureTest {

    @Test
    fun figo_pattern_to_descriptive_text_is_never_diagnostic() {
        val phrases = FigoAnalysis.Pattern.values().map { DoctorPdfBuilder.descriptiveText(it) }
        // Spec §5.7: descriptive only, never diagnostic.
        val banned = listOf("PCOS", "diagnosis", "you may have", "suggests")
        for (p in phrases) {
            for (b in banned) {
                assert(!p.contains(b, ignoreCase = true)) {
                    "phrase \"$p\" contains banned diagnostic word \"$b\""
                }
            }
        }
    }

    @Test
    fun every_pattern_has_a_phrase() {
        for (p in FigoAnalysis.Pattern.values()) {
            val text = DoctorPdfBuilder.descriptiveText(p)
            assert(text.isNotBlank()) { "Pattern $p has no descriptive text" }
        }
    }
}
