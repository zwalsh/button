package sh.zachwal.button.wrapped

import sh.zachwal.button.db.dao.PressDAO
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import javax.inject.Inject


class WrappedService @Inject constructor(
    private val pressDAO: PressDAO
) {

    fun wrapped(year: Int, id: String): Wrapped {
        val easternTime = ZoneId.of("America/New_York")
        val start = LocalDate.of(year, Month.JANUARY, 1).atStartOfDay(easternTime).toInstant()
        val end = LocalDate.of(year, Month.DECEMBER, 15).atStartOfDay(easternTime).toInstant()

        val presses = pressDAO.selectBetweenForContact(start, end, id.toInt())

        return Wrapped(
            year = year,
            id = id,
            count = presses.count()
        )
    }
}
