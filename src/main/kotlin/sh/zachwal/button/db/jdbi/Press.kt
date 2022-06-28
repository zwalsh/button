package sh.zachwal.button.db.jdbi

import java.time.Instant

data class Press(
    val time: Instant,
    val ip: String,
)
