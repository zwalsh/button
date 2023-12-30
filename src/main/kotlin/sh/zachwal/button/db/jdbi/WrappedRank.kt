package sh.zachwal.button.db.jdbi

data class WrappedRank constructor(
    val contactId: Int,
    val uniqueDays: Int,
    val uniqueDaysRank: Int,
    val uniqueDaysPercentile: Double,
    val rank: Int,
    val percentile: Double
)
