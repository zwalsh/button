package sh.zachwal.button.db.jdbi

import java.time.Instant
import java.time.LocalTime

data class NotificationPreferences(
    // TODO: Default to false once the opt-in onboarding flow is built — see PhoneBookService.register()
    val notificationsEnabled: Boolean,
    val snoozedUntil: Instant?,
    val quietHoursStart: LocalTime?,
    val quietHoursEnd: LocalTime?,
    val timezone: String?,
)
