package sh.zachwal.button.wrapped

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import sh.zachwal.button.db.dao.PressDAO
import sh.zachwal.button.db.jdbi.Press
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals

class WrappedServiceTest {

    private val dao: PressDAO = mockk()
    private val service = WrappedService(dao)

    @Test
    fun `includes count of presses`() {
        every { dao.selectBetweenForContact(any(), any(), any()) } returns listOf(
            Press(Instant.now(), "", 1),
            Press(Instant.now(), "", 1),
            Press(Instant.now(), "", 1)
        )

        val wrapped = service.wrapped(2023, "1")

        assertEquals(3, wrapped.count)
    }

    @Test
    fun `includes favorite day with count`() {
        val sundayNoon = Instant.parse("2023-12-10T12:00:00Z")
        val sundayOne = sundayNoon.plus(1, ChronoUnit.HOURS)
        val p1 = Press(time = sundayNoon, "", 1)
        val p2 = p1.copy(time = sundayOne)
        val p3 = p2.copy(time = sundayOne.plus(1, ChronoUnit.DAYS))
        val p4 = p2.copy(time = sundayOne.plus(2, ChronoUnit.DAYS))
        val p5 = p2.copy(time = sundayOne.plus(3, ChronoUnit.DAYS))
        val p6 = p2.copy(time = sundayOne.plus(4, ChronoUnit.DAYS))
        val p7 = p2.copy(time = sundayOne.plus(5, ChronoUnit.DAYS))
        every { dao.selectBetweenForContact(any(), any(), any()) } returns listOf(
            p1, p2, p3, p4, p5, p6, p7
        )

        val wrapped = service.wrapped(2023, "1")

        assertEquals("Sunday", wrapped.favoriteDay)
        assertEquals(2, wrapped.favoriteDayCount)
    }

    @Test
    fun `includes favorite hour with count`() {
        val sundayNoon = Instant.from(
            DateTimeFormatter.ISO_ZONED_DATE_TIME.parse("2023-12-10T12:00:00-05:00")
        )

        val sundayOne = sundayNoon.plus(1, ChronoUnit.HOURS)
        val p1 = Press(time = sundayNoon, "", 1)
        val p2 = p1.copy(time = sundayOne)
        val p3 = p1.copy(time = sundayNoon.plus(1, ChronoUnit.DAYS))
        every { dao.selectBetweenForContact(any(), any(), any()) } returns listOf(p1, p2, p3)

        val wrapped = service.wrapped(2023, "1")

        assertEquals("12PM", wrapped.favoriteHourString)
        assertEquals(2, wrapped.favoriteHourCount)
    }

    @Test
    fun `formats favorite hour when 12AM`() {
        val midnight = Instant.from(
            DateTimeFormatter.ISO_ZONED_DATE_TIME.parse("2023-12-10T00:00:00-05:00")
        )
        val p1 = Press(time = midnight, "", 1)
        every { dao.selectBetweenForContact(any(), any(), any()) } returns listOf(p1)

        val wrapped = service.wrapped(2023, "1")

        assertEquals("12AM", wrapped.favoriteHourString)
    }

    @Test
    fun `formats favorite hour when 11PM`() {
        val midnight = Instant.from(
            DateTimeFormatter.ISO_ZONED_DATE_TIME.parse("2023-12-10T23:00:00-05:00")
        )
        val p1 = Press(time = midnight, "", 1)
        every { dao.selectBetweenForContact(any(), any(), any()) } returns listOf(p1)

        val wrapped = service.wrapped(2023, "1")

        assertEquals("11PM", wrapped.favoriteHourString)
    }
}
