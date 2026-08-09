package sh.zachwal.button.db.jdbi

import java.time.Instant
import java.time.LocalTime

fun contact(
    id: Int = 1,
    createdDate: Instant = Instant.EPOCH,
    name: String = "Alice",
    phoneNumber: String = "+15550000000",
    active: Boolean = true,
    notificationsEnabled: Boolean = true,
    snoozedUntil: Instant? = null,
    quietHoursStart: LocalTime? = null,
    quietHoursEnd: LocalTime? = null,
    timezone: String? = null,
) = Contact(
    id = id,
    createdDate = createdDate,
    name = name,
    phoneNumber = phoneNumber,
    active = active,
    notificationPreferences = NotificationPreferences(
        notificationsEnabled = notificationsEnabled,
        snoozedUntil = snoozedUntil,
        quietHoursStart = quietHoursStart,
        quietHoursEnd = quietHoursEnd,
        timezone = timezone,
    ),
)
