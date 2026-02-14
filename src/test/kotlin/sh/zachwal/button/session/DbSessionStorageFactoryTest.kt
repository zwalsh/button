package sh.zachwal.button.session

import io.mockk.mockk
import org.junit.Test
import sh.zachwal.button.db.dao.SessionDAO
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DbSessionStorageFactoryTest {
    private val sessionDao = mockk<SessionDAO>()
    private val factory = DbSessionStorageFactory(sessionDao)

    @Test
    fun `buildStorage creates DbSessionStorage with correct prefix`() {
        val prefix = "TEST_PREFIX"
        val storage = factory.buildStorage(prefix)

        assertNotNull(storage)
        // The storage instance is created - further testing of prefix behavior
        // is covered in DbSessionStorageTest
    }

    @Test
    fun `buildStorage creates separate instances for different prefixes`() {
        val userStorage = factory.buildStorage(USER_SESSION)
        val contactStorage = factory.buildStorage(CONTACT_SESSION)

        assertNotNull(userStorage)
        assertNotNull(contactStorage)
        // Each call creates a new instance
        assertEquals(false, userStorage === contactStorage)
    }
}
