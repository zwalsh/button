package sh.zachwal.button.db.jdbi

import org.jdbi.v3.core.mapper.Nested
import java.time.Instant

data class Contact(
    val id: Int,
    val createdDate: Instant,
    val name: String,
    val phoneNumber: String,
    val active: Boolean,
    @Nested val notificationPreferences: NotificationPreferences,
)
