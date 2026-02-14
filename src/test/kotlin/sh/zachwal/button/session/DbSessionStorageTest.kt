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
    private val sessionPrefix = "TEST_SESSION"
    private val storage = DbSessionStorage(sessionDao, sessionPrefix)

    private val sessionId = "hi"
    private val dataString = "data"
    private val data = dataString.toByteArray()
    private val expiration = Instant.now()
    private val dbSessionId = "${sessionPrefix}_$sessionId"
    private val session = Session(dbSessionId, data, expiration)

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

        verify { sessionDao.deleteSession("${sessionPrefix}_$sessionId") }
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
            assertEquals(dbSessionId, capturedSession.id)
            assertEquals(dataString, String(capturedSession.data))
            assertThat(capturedSession.expiration.epochSecond.toDouble())
                .isWithin(1.0)
                .of(expiration.plus(CONTACT_SESSION_LENGTH.toDays(), ChronoUnit.DAYS).epochSecond.toDouble())
        }
    }

    @Test
    fun `different prefixes produce different database IDs`() {
        val userStorage = DbSessionStorage(sessionDao, USER_SESSION)
        val contactStorage = DbSessionStorage(sessionDao, CONTACT_SESSION)

        val userSlot = slot<Session>()
        val contactSlot = slot<Session>()
        every { sessionDao.createOrUpdateSession(capture(userSlot)) } just Runs
        every { sessionDao.createOrUpdateSession(capture(contactSlot)) } just Runs

        val sharedSessionId = "same-id-from-ktor"

        runBlocking {
            userStorage.write(sharedSessionId, "user-data")
            contactStorage.write(sharedSessionId, "contact-data")

            val userDbId = userSlot.captured.id
            val contactDbId = contactSlot.captured.id

            // Verify that the same Ktor-generated session ID results in different database IDs
            assertEquals("${USER_SESSION}_$sharedSessionId", userDbId)
            assertEquals("${CONTACT_SESSION}_$sharedSessionId", contactDbId)
            assertThat(userDbId).isNotEqualTo(contactDbId)
        }
    }

    @Test
    fun `prefix is applied consistently across operations`() {
        val slot = slot<Session>()
        every { sessionDao.createOrUpdateSession(capture(slot)) } just Runs
        every { sessionDao.deleteSession(any()) } just Runs

        runBlocking {
            // Write should use prefixed ID
            storage.write(sessionId, dataString)
            assertEquals(dbSessionId, slot.captured.id)

            // Read should look up with prefixed ID
            storage.read(sessionId)
            verify { sessionDao.getById(dbSessionId) }

            // Invalidate should delete with prefixed ID
            storage.invalidate(sessionId)
            verify { sessionDao.deleteSession(dbSessionId) }
        }
    }
}
