package sh.zachwal.button.db.jdbi

import java.time.Instant

data class ContactToken(
    val token: String,
    val contactId: Int,
    val expiration: Instant
)
