package sh.zachwal.button.wrapped

import java.time.DayOfWeek

data class Wrapped(
    val year: Int,
    val id: String,
    val count: Int,
    val favoriteDay: String,
    val favoriteDayCount: Int,
    val favoriteHourString: String,
    val favoriteHourCount: Int
)
