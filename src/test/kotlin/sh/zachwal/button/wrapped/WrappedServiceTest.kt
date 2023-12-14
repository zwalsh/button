package sh.zachwal.button.wrapped

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import sh.zachwal.button.db.dao.PressDAO
import sh.zachwal.button.db.jdbi.Press
import java.time.Instant
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
}
