package sh.zachwal.button.admin

import sh.zachwal.button.db.dao.ContactDAO
import sh.zachwal.button.db.dao.ContactPressCountDAO
import sh.zachwal.button.db.dao.PressDAO
import sh.zachwal.button.db.jdbi.Contact
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

enum class TimeRange(val label: String, val queryParam: String) {
    TODAY("Today", "today"),
    LAST_7_DAYS("Last 7 Days", "7d"),
    LAST_30_DAYS("Last 30 Days", "30d"),
    LAST_90_DAYS("Last 90 Days", "90d"),
    YEAR_TO_DATE("Year to Date", "ytd"),
    ALL_TIME("All Time", "all");

    companion object {
        fun fromParam(param: String?): TimeRange =
            entries.find { it.queryParam == param } ?: LAST_30_DAYS
    }
}

data class ContactPressStat(val contact: Contact, val count: Long)

@Singleton
class ContactPressStatsService @Inject constructor(
    private val contactDAO: ContactDAO,
    private val contactPressCountDAO: ContactPressCountDAO,
    private val pressDAO: PressDAO,
) {
    fun allContactStats(range: TimeRange): List<ContactPressStat> {
        val statsById = pressStats(range).associateBy { it.contact.id }
        return contactDAO.selectContacts()
            .map { c -> statsById[c.id] ?: ContactPressStat(c, 0L) }
            .sortedByDescending { it.count }
    }

    fun pressStats(range: TimeRange): List<ContactPressStat> {
        val today = LocalDate.now(ZoneOffset.UTC)
        val startDate: LocalDate = when (range) {
            TimeRange.TODAY -> today
            TimeRange.LAST_7_DAYS -> today.minusDays(7)
            TimeRange.LAST_30_DAYS -> today.minusDays(30)
            TimeRange.LAST_90_DAYS -> today.minusDays(90)
            TimeRange.YEAR_TO_DATE -> LocalDate.of(today.year, 1, 1)
            TimeRange.ALL_TIME -> LocalDate.ofEpochDay(0)
        }

        val yesterday = today.minusDays(1)
        val materialized: Map<Int, Long> = if (startDate <= yesterday) {
            contactPressCountDAO
                .aggregateCountsByContact(startDate, yesterday)
                .mapValues { it.value.toLong() }
        } else {
            emptyMap()
        }

        val todayStart = today.atStartOfDay(ZoneOffset.UTC).toInstant()
        val todayCounts: Map<Int, Long> = pressDAO.countByContactSince(todayStart)

        val allIds = (materialized.keys + todayCounts.keys).toSet()
        val contactsById = contactDAO.selectContacts().associateBy { it.id }

        return allIds.mapNotNull { id ->
            val contact = contactsById[id] ?: return@mapNotNull null
            val count = (materialized[id] ?: 0L) + (todayCounts[id] ?: 0L)
            ContactPressStat(contact, count)
        }
            .filter { it.count > 0 }
            .sortedByDescending { it.count }
    }
}
