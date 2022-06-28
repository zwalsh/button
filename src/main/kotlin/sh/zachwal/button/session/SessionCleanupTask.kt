package sh.zachwal.button.session

import org.slf4j.LoggerFactory
import sh.zachwal.authserver.db.dao.SessionDAO
import java.time.Instant
import javax.inject.Inject
import kotlin.concurrent.timer

class SessionCleanupTask @Inject constructor(private val sessionDAO: SessionDAO) {

    private val logger = LoggerFactory.getLogger(SessionCleanupTask::class.java)

    fun cleanup() {
        logger.info("Cleaning up expired sessions")
        sessionDAO.deleteExpiredBefore(Instant.now()).forEach {
            logger.info("Deleted session ${it.id}, expired at ${it.expiration}")
        }
    }

    fun repeatCleanup() {
        // run every hour
        timer("session-cleanup", true, period = 1000L * 60 * 60) {
            try {
                cleanup()
            } catch (e: Exception) {
                logger.error("Failed to clean up sessions", e)
            }
        }
    }
}
