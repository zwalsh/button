package sh.zachwal.button.session

import com.google.common.truth.Truth.assertThat
import io.ktor.util.toByteArray
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.core.ExperimentalIoApi
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import sh.zachwal.button.db.dao.SessionDAO
import sh.zachwal.button.db.jdbi.Session
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals

class DbSessionStorageTest {
    private val sessionDao = mockk<SessionDAO>()
    private val storage = DbSessionStorage(sessionDao)

    private val sessionId = "hi"
    private val data = "data".toByteArray()
    private val expiration = Instant.now()
    private val session = Session(sessionId, data, expiration)

    @Before
    fun setup() {
        every { sessionDao.getById(any()) } returns session
    }

    @Test
    fun testInvalidateClearsSession() {
        every { sessionDao.deleteSession(any()) } just Runs

        runBlocking {
            storage.invalidate(sessionId)
        }

        verify { sessionDao.deleteSession(sessionId) }
    }

    @ExperimentalIoApi
    @Test
    fun `read calls consumer with bytes from session dao`() {
        val slot = slot<ByteReadChannel>()
        val consumer: suspend (ByteReadChannel) -> Session = mockk()
        coEvery { consumer(capture(slot)) } returns session

        runBlocking {
            val returnedSession = storage.read(sessionId, consumer)

            assertEquals(String(data), String(slot.captured.toByteArray()))
            assertEquals(session, returnedSession)
        }
    }

    @Test
    fun write() {
        val slot = slot<Session>()
        every { sessionDao.createOrUpdateSession(capture(slot)) } just Runs
        val provider: suspend (ByteWriteChannel) -> Unit = {
            it.write { byteBuffer ->
                byteBuffer.put(data)
            }
        }

        runBlocking {
            storage.write(sessionId, provider)
            val capturedSession = slot.captured
            assertEquals(sessionId, capturedSession.id)
            assertEquals(String(data), String(capturedSession.data))
            assertThat(capturedSession.expiration.epochSecond.toDouble())
                .isWithin(1.0)
                .of(expiration.plus(1, ChronoUnit.HOURS).epochSecond.toDouble())
        }
    }
}
