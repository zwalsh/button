package sh.zachwal.button.presshistory

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import sh.zachwal.button.db.dao.PressDAO
import sh.zachwal.button.db.jdbi.Press
import java.time.Instant

internal class PressHistoryServiceTest {

    private val pressDAO = mockk<PressDAO>()
    private val service = PressHistoryService(pressDAO)

    @BeforeEach
    fun setup() {
        every {
            pressDAO.createPress(any())
        } answers {
            val ip = firstArg<String>()
            Press(Instant.now(), ip)
        }
    }

    @Test
    fun `observing a press creates a new press record`() {
        service.createPress("some-ip")

        verify {
            pressDAO.createPress(eq("some-ip"))
        }
    }

    @Test
    fun `listing presses returns what the DAO provides`() {
        val presses = listOf(
            Press(Instant.now().minusSeconds(35), "127.0.0.1"),
            Press(Instant.now().minusSeconds(25), "192.168.0.1"),
            Press(Instant.now().minusSeconds(15), "10.0.0.1"),
        )
        every { pressDAO.selectSince(any()) } returns presses

        val since = Instant.now().minusSeconds(60)
        val result = service.listPresses(since)

        verify { pressDAO.selectSince(since) }
        assertThat(result).containsExactlyElementsIn(presses).inOrder()
    }
}
