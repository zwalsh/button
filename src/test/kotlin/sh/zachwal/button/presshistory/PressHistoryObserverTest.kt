package sh.zachwal.button.presshistory

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import sh.zachwal.button.db.dao.PressDAO
import sh.zachwal.button.db.jdbi.Contact
import sh.zachwal.button.db.jdbi.Press
import sh.zachwal.button.presser.Presser
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.test.assertTrue

internal class PressHistoryObserverTest {

    private val pressDAO = mockk<PressDAO>()
    private val observer = PressHistoryObserver(pressDAO)

    @Test
    @Disabled // flaky
    fun `creates press with service in background`() {
        val presser = mockk<Presser>()
        val latch = CountDownLatch(1)
        var succeeded = false
        every { presser.remoteHost } returns "127.0.0.1"
        every { presser.contact } returns null
        every { pressDAO.createPress(any(), any()) } answers {
            // createPress blocks until the latch counts down, or for 100ms
            succeeded = latch.await(1000, MILLISECONDS)
            Press(Instant.now(), firstArg(), secondArg())
        }

        runBlocking {
            observer.pressed(presser)
        }
        latch.countDown()

        verify(timeout = 1000) {
            pressDAO.createPress(any(), any())
        }
        assertTrue(succeeded)
    }

    @Test
    fun `uses the remote host of the presser as the ip`() {
        val presser = mockk<Presser>()
        val remoteHost = "192.168.0.1"
        every { presser.remoteHost } returns remoteHost
        every { presser.contact } returns null
        every { pressDAO.createPress(any(), any()) } returns Press(Instant.now(), "", 1)

        runBlocking {
            observer.pressed(presser)
        }

        verify(timeout = 100) {
            pressDAO.createPress(remoteHost, null)
        }
    }
    @Test
    fun `uses the contact id of the presser on the press`() {
        val presser = mockk<Presser>()
        val remoteHost = "192.168.0.1"
        every { presser.remoteHost } returns remoteHost
        every { presser.contact } returns Contact(id = 10, Instant.now(), "", "", true)
        every { pressDAO.createPress(any(), any()) } returns Press(Instant.now(), "", 1)

        runBlocking {
            observer.pressed(presser)
        }

        verify(timeout = 100) {
            pressDAO.createPress(remoteHost, 10)
        }
    }

    @Test
    fun `one failed create will not prevent others`() {
        val presser = mockk<Presser>()
        val remoteHost = "192.168.0.1"
        every { presser.remoteHost } returns remoteHost
        every { presser.contact } returns null
        every { pressDAO.createPress(any(), any()) } throws
            Exception("Oops!") andThen
            Press(Instant.now(), "", 1)

        runBlocking {
            // first will fail
            observer.pressed(presser)
            // second should succeed
            observer.pressed(presser)
            // third should succeed
            observer.pressed(presser)
        }

        verify(timeout = 100, exactly = 3) {
            pressDAO.createPress(remoteHost, null)
        }
    }
}
