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
import sh.zachwal.authserver.db.dao.SessionDAO
import sh.zachwal.authserver.db.jdbi.Session
import java.time.Instant
import java.time.temporal.ChronoUnit
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
        logger.info("Reading $id")
        return withContext(Dispatchers.IO) {
            val bytes = sessionDAO.getById(id)?.data
                ?: throw NoSuchElementException("No session with id $id")
            consumer(ByteReadChannel(bytes))
        }
    }

    override suspend fun write(id: String, provider: suspend (ByteWriteChannel) -> Unit) {
        logger.info("Writing $id")
        withContext(Dispatchers.IO) {
            val bytes = writer {
                provider(channel)
            }.channel
            sessionDAO.createOrUpdateSession(Session(id, bytes.toByteArray(), Instant.now().plus(1, ChronoUnit.HOURS)))
        }
    }
}
