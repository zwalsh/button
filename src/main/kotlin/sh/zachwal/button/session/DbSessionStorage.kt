package sh.zachwal.button.session

import io.ktor.server.sessions.SessionStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import sh.zachwal.button.db.dao.SessionDAO
import sh.zachwal.button.db.jdbi.Session
import java.time.Instant
import javax.inject.Inject

class DbSessionStorage @Inject constructor(private val sessionDAO: SessionDAO) : SessionStorage {
    private val logger = LoggerFactory.getLogger(DbSessionStorage::class.java)

    override suspend fun invalidate(id: String) {
        logger.info("Clearing $id")
        withContext(Dispatchers.IO) {
            sessionDAO.deleteSession(id)
        }
    }

    override suspend fun read(id: String): String {
        return withContext(Dispatchers.IO) {
            val bytes = sessionDAO.getById(id)?.data
                ?: throw NoSuchElementException("No session with id $id")
            bytes.decodeToString()
        }
    }

    override suspend fun write(id: String, value: String) {
        // Note that this function is called every time the session is used.
        withContext(Dispatchers.IO) {
            val session = Session(
                id = id,
                data = value.encodeToByteArray(),
                // CONTACT_SESSION_LENGTH is the longer of the two. Set the db expiration to the longer one as a session
                // is only valid if the `bytes` contains an expiration time that hasn't passed, and the row is still
                // present in the database (i.e. hasn't been cleaned up by SessionCleanupTask).
                expiration = Instant.now().plus(CONTACT_SESSION_LENGTH)
            )
            sessionDAO.createOrUpdateSession(session)
        }
    }
}
