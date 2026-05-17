package com.hayate0726.tides.domain.model

enum class ThreatPreset(
    val backgroundLockTimeoutMs: Long?,
    val lockScreenPreviewVisible: Boolean,
    val defaultNotificationTitle: String,
    val duressAvailable: Boolean,
) {
    JUST_FOR_ME(
        backgroundLockTimeoutMs = null, // never auto-lock
        lockScreenPreviewVisible = true,
        defaultNotificationTitle = "Tides",
        duressAvailable = false,
    ),
    LOCKED_WHEN_AWAY(
        backgroundLockTimeoutMs = 5 * 60 * 1000L,
        lockScreenPreviewVisible = false,
        defaultNotificationTitle = "Reminder",
        duressAvailable = false,
    ),
    ALWAYS_LOCKED(
        backgroundLockTimeoutMs = 30 * 1000L,
        lockScreenPreviewVisible = false,
        defaultNotificationTitle = "Reminder",
        duressAvailable = true,
    );

    companion object {
        val DEFAULT = LOCKED_WHEN_AWAY
    }
}
