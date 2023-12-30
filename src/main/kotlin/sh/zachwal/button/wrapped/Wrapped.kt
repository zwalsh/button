package sh.zachwal.button.wrapped

data class Wrapped(
    val year: Int,
    val name: String,
    val count: Int,
    val favoriteDay: String,
    val favoriteDayCount: Int,
    val favoriteHourString: String,
    val favoriteHourCount: Int,
    val rank: Int,
    val percentile: Int, // 1-100
    val uniqueDaysCount: Int,
    val uniqueDaysRank: Int,
    val uniqueDaysPercentile: Int, // 1-100
)
