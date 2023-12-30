package sh.zachwal.button.wrapped

import io.ktor.features.NotFoundException
import sh.zachwal.button.db.dao.ContactDAO
import sh.zachwal.button.db.dao.WrappedDAO
import sh.zachwal.button.db.jdbi.WrappedLink
import sh.zachwal.button.random.RandomStringGenerator
import java.time.Instant
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

    private val randomStringGenerator = RandomStringGenerator()
    private fun startOfYearInstant(year: Int): Instant =
        LocalDate.of(year, Month.JANUARY, 1)
            .atStartOfDay(easternTime)
            .toInstant()

    private fun endOfYearInstant(year: Int): Instant =
        LocalDate.of(year + 1, Month.JANUARY, 1)
            .atStartOfDay(easternTime)
            .toInstant()

    fun createWrappedLinks() {
        val year = LocalDate.now().year
        val links = wrappedDAO.wrappedLinks()

        if (links.any { it.year == year }) {
            throw RuntimeException("Links have already been generated for $year!")
        }

        val contactIds = wrappedDAO.contactsWithPresses(
            fromInstant = startOfYearInstant(year),
            toInstant = endOfYearInstant(year)
        )

        contactIds.forEach { contactId ->
            val wrappedId = randomStringGenerator.newToken(20)
            val wrappedLink = WrappedLink(
                wrappedId,
                year,
                contactId
            )

            wrappedDAO.insertWrappedLink(wrappedLink)
        }
    }

    fun listWrappedLinks(): List<WrappedLink> {
        return wrappedDAO.wrappedLinks()
    }

    private val easternTime = ZoneId.of("America/New_York")

    fun wrapped(year: Int, id: String): Wrapped {
        val wrappedLink = wrappedDAO.wrappedLinks()
            .find { it.wrappedId == id }
            ?: throw NotFoundException("Could not find Wrapped with id $id")

        val contact = contactDAO.findContact(wrappedLink.contactId) ?: throw NotFoundException(
            "Could not " +
                "find contact with id $id."
        )

        val presses = wrappedDAO.selectBetweenForContact(
            begin = startOfYearInstant(year),
            end = endOfYearInstant(year),
            contactId = contact.id
        )
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

        val wrappedRanks = wrappedDAO.wrappedRanks(
            fromInstant = startOfYearInstant(year),
            toInstant = endOfYearInstant(year)
        )
        val wrappedRank = wrappedRanks.find { it.contactId == contact.id }!!

        return Wrapped(
            year = year,
            name = contact.name,
            count = presses.size,
            favoriteDay = favoriteDay.key.getDisplayName(FULL, Locale.US),
            favoriteDayCount = favoriteDay.value.size,
            favoriteHourString = favoriteHourString,
            favoriteHourCount = favoriteHour.value.size,
            rank = wrappedRank.rank,
            percentile = percentileAsInt(wrappedRank.percentile),
            uniqueDaysCount = wrappedRank.uniqueDays,
            uniqueDaysRank = wrappedRank.uniqueDaysRank,
            uniqueDaysPercentile = percentileAsInt(wrappedRank.uniqueDaysPercentile)
        )
    }

    private fun percentileAsInt(percentile: Double) =
        (percentile * 100)
            .roundToInt()
            .takeIf { it != 0 }
            ?: 1 // round 0% to 1%
}
