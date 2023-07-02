package sh.zachwal.button.db.jdbi

import java.time.Instant

data class RecentPressCount(
    val name: String?,
    val count: Int,
    val mostRecent: Instant,
)
