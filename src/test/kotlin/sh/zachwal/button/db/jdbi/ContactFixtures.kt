package sh.zachwal.button.db.jdbi

import java.time.Instant

fun contact(
    id: Int = 1,
    createdDate: Instant = Instant.EPOCH,
    name: String = "Alice",
    phoneNumber: String = "+15550000000",
    active: Boolean = true,
    notificationsEnabled: Boolean = true,
) = Contact(
    id = id,
    createdDate = createdDate,
    name = name,
    phoneNumber = phoneNumber,
    active = active,
    notificationPreferences = NotificationPreferences(notificationsEnabled = notificationsEnabled),
)
