package sh.zachwal.button.db.jdbi

import java.time.Instant

data class Press(
    val time: Instant,
    val remote: String,
    val contactId: Int?,
)
