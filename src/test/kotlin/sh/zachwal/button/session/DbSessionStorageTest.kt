package sh.zachwal.button.session

import com.google.common.truth.Truth.assertThat
import io.mockk.Runs
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
    private val dataString = "data"
    private val data = dataString.toByteArray()
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

    @Test
    fun `read returns string from session dao`() {
        runBlocking {
            val returnedData = storage.read(sessionId)

            assertEquals(dataString, returnedData)
        }
    }

    @Test
    fun write() {
        val slot = slot<Session>()
        every { sessionDao.createOrUpdateSession(capture(slot)) } just Runs

        runBlocking {
            storage.write(sessionId, dataString)
            val capturedSession = slot.captured
            assertEquals(sessionId, capturedSession.id)
            assertEquals(dataString, String(capturedSession.data))
            assertThat(capturedSession.expiration.epochSecond.toDouble())
                .isWithin(1.0)
                .of(expiration.plus(1, ChronoUnit.HOURS).epochSecond.toDouble())
        }
    }
}
