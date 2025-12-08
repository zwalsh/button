package sh.zachwal.button.session

import io.ktor.sessions.SessionStorage
import io.ktor.util.toByteArray
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.core.ExperimentalIoApi
import io.ktor.utils.io.writer
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

    @ExperimentalIoApi
    override suspend fun <R> read(id: String, consumer: suspend (ByteReadChannel) -> R): R {
        return withContext(Dispatchers.IO) {
            val bytes = sessionDAO.getById(id)?.data
                ?: throw NoSuchElementException("No session with id $id")
            consumer(ByteReadChannel(bytes))
        }
    }

    override suspend fun write(id: String, provider: suspend (ByteWriteChannel) -> Unit) {
        // Note that this function is called every time the session is used.
        withContext(Dispatchers.IO) {
            val bytes = writer {
                provider(channel)
            }.channel
            val session = Session(
                id = id,
                data = bytes.toByteArray(),
                // CONTACT_SESSION_LENGTH is the longer of the two. Set the db expiration to the longer one as a session
                // is only valid if the `bytes` contains an expiration time that hasn't passed, and the row is still
                // present in the database (i.e. hasn't been cleaned up by SessionCleanupTask).
                expiration = Instant.now().plus(CONTACT_SESSION_LENGTH)
            )
            sessionDAO.createOrUpdateSession(session)
        }
    }
}
