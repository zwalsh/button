package sh.zachwal.button.presshistory

import io.mockk.Runs
import io.mockk.andThenJust
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import sh.zachwal.button.presser.Presser
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.test.assertTrue

internal class PressHistoryObserverTest {

    private val pressHistoryService = mockk<PressHistoryService>()
    private val observer = PressHistoryObserver(pressHistoryService)

    @Test
    fun `creates press with service in background`() {
        val presser = mockk<Presser>()
        val latch = CountDownLatch(1)
        var succeeded = false
        every { presser.remoteHost } returns "127.0.0.1"
        every { pressHistoryService.createPress(any()) } answers {
            // createPress blocks until the latch counts down, or for 100ms
            succeeded = latch.await(1000, MILLISECONDS)
        }

        runBlocking {
            observer.pressed(presser)
        }
        latch.countDown()

        verify(timeout = 1000) {
            pressHistoryService.createPress(any())
        }
        assertTrue(succeeded)
    }

    @Test
    fun `uses the remote host of the presser as the ip`() {
        val presser = mockk<Presser>()
        val remoteHost = "192.168.0.1"
        every { presser.remoteHost } returns remoteHost
        every { pressHistoryService.createPress(any()) } just Runs

        runBlocking {
            observer.pressed(presser)
        }

        verify(timeout = 100) {
            pressHistoryService.createPress(remoteHost)
        }
    }

    @Test
    fun `one failed create will not prevent others`() {
        val presser = mockk<Presser>()
        val remoteHost = "192.168.0.1"
        every { presser.remoteHost } returns remoteHost
        every { pressHistoryService.createPress(any()) } throws Exception("Oops!") andThenJust runs

        runBlocking {
            // first will fail
            observer.pressed(presser)
            // second should succeed
            observer.pressed(presser)
            // third should succeed
            observer.pressed(presser)
        }

        verify(timeout = 100, exactly = 3) {
            pressHistoryService.createPress(remoteHost)
        }
    }
}
