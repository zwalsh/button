package sh.zachwal.button.wrapped

import sh.zachwal.button.db.dao.PressDAO
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import java.time.format.TextStyle.FULL
import java.util.Locale
import javax.inject.Inject

class WrappedService @Inject constructor(
    private val pressDAO: PressDAO
) {

    fun wrapped(year: Int, id: String): Wrapped {
        val easternTime = ZoneId.of("America/New_York")
        val start = LocalDate.of(year, Month.JANUARY, 1).atStartOfDay(easternTime).toInstant()
        val end = LocalDate.of(year, Month.DECEMBER, 15).atStartOfDay(easternTime).toInstant()

        val presses = pressDAO.selectBetweenForContact(start, end, id.toInt())
        val countByDay = presses.groupBy {
            LocalDate.ofInstant(it.time, easternTime).dayOfWeek
        }
        val favoriteDay = countByDay.entries.maxByOrNull {
            it.value.count()
        }!!

        val countByHour = presses.groupBy {
            LocalDateTime.ofInstant(it.time, easternTime).hour
        }
        val favoriteHour = countByHour.entries.maxByOrNull {
            it.value.count()
        }!!
        val hour = favoriteHour.key
        val favoriteHour12Hour = if (hour % 12 == 0) 12 else hour % 12
        val favoriteHourAmPm = if (favoriteHour.key < 12) {
            "AM"
        } else {
            "PM"
        }
        val favoriteHourString = "$favoriteHour12Hour$favoriteHourAmPm"

        return Wrapped(
            year = year,
            id = id,
            count = presses.size,
            favoriteDay = favoriteDay.key.getDisplayName(FULL, Locale.US),
            favoriteDayCount = favoriteDay.value.size,
            favoriteHourString = favoriteHourString,
            favoriteHourCount = favoriteHour.value.size
        )
    }
}
