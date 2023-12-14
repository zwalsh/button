package sh.zachwal.button.wrapped

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import sh.zachwal.button.db.dao.PressDAO
import sh.zachwal.button.db.jdbi.Press
import java.time.DayOfWeek
import java.time.Instant
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

        assertEquals(DayOfWeek.SUNDAY, wrapped.favoriteDay)
        assertEquals(2, wrapped.favoriteDayCount)
    }
}
