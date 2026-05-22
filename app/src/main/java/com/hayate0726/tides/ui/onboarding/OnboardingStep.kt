package com.hayate0726.tides.ui.onboarding

/**
 * Steps the user can be paused on. `WELCOME` is the implicit "no draft yet"
 * starting point. `THREAT` is intentionally not persisted in the draft
 * (re-prompted on resume to avoid disclosing duress-mode consideration to a
 * forensic reader). FLOW_SYMPTOMS and PREDICTION are valid step targets only
 * after Tasks 5 and 6 land; in this commit the resume nav falls back to
 * `COMPLETE` for those values.
 */
enum class OnboardingStep {
    WELCOME,
    GOALS,
    PIN,
    BIOMETRIC,
    THREAT,
    BIRTH_CONTROL,
    IMPORT_PROMPT,
    LAST_PERIOD,
    FLOW_SYMPTOMS,
    PREDICTION,
    COMPLETE,
}
