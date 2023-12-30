package sh.zachwal.button.wrapped

import io.ktor.features.NotFoundException
import sh.zachwal.button.db.dao.ContactDAO
import sh.zachwal.button.db.dao.WrappedDAO
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import java.time.format.TextStyle.FULL
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

class WrappedService @Inject constructor(
    private val contactDAO: ContactDAO,
    private val wrappedDAO: WrappedDAO
) {

    fun wrapped(year: Int, id: String): Wrapped {
        val contact = contactDAO.findContact(id.toInt()) ?: throw NotFoundException(
            "Could not " +
                "find contact with id $id."
        )

        val easternTime = ZoneId.of("America/New_York")
        val start = LocalDate.of(year, Month.JANUARY, 1).atStartOfDay(easternTime).toInstant()
        val end = LocalDate.of(year, Month.DECEMBER, 15).atStartOfDay(easternTime).toInstant()

        val presses = wrappedDAO.selectBetweenForContact(start, end, id.toInt())
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

        val thisYear = LocalDate.of(year, 1, 1)
        val nextYear = LocalDate.of(year + 1, 1, 1)
        val wrappedRanks = wrappedDAO.wrappedRanks(
            fromInstant = thisYear.atStartOfDay(easternTime).toInstant(),
            toInstant = nextYear.atStartOfDay(easternTime).toInstant()
        )
        val wrappedRank = wrappedRanks.find { it.contactId == contact.id }!!

        val percentile = (wrappedRank.percentile * 100)
            .roundToInt()
            .takeIf { it != 0 }
            ?: 1 // round 0% to 1%
        return Wrapped(
            year = year,
            name = contact.name,
            count = presses.size,
            favoriteDay = favoriteDay.key.getDisplayName(FULL, Locale.US),
            favoriteDayCount = favoriteDay.value.size,
            favoriteHourString = favoriteHourString,
            favoriteHourCount = favoriteHour.value.size,
            rank = wrappedRank.rank,
            percentile = percentile
        )
    }
}
