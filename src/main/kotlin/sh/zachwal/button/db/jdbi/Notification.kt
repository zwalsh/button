package sh.zachwal.button.db.jdbi

import java.time.Instant

/**
 * A time that a notification was sent to Button contacts.
 */
data class Notification(
    val id: Int,
    val sentDate: Instant
)
