package sh.zachwal.button.db.jdbi

data class WrappedRank(
    val contactId: Int,
    val rank: Int,
    val percentile: Double
)
