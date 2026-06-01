package sh.zachwal.button.auth.contact

import org.slf4j.LoggerFactory
import sh.zachwal.button.db.dao.ContactTokenDAO
import java.time.Instant
import javax.inject.Inject
import kotlin.concurrent.timer

class ContactTokenCleanupTask @Inject constructor(private val contactTokenDAO: ContactTokenDAO) {

    private val logger = LoggerFactory.getLogger(ContactTokenCleanupTask::class.java)

    fun cleanup() {
        val deleted = contactTokenDAO.deleteExpiredBefore(Instant.now())
        logger.info("Cleaned up {} expired contact tokens", deleted.size)
        deleted.forEach {
            logger.debug("Deleted contact token {}, expired at {}", it, it.expiration)
        }
    }

    fun repeatCleanup() {
        // run every hour
        timer("contact-token-cleanup", true, period = 1000L * 60 * 60) {
            try {
                cleanup()
            } catch (e: Exception) {
                logger.error("Failed to clean up sessions", e)
            }
        }
    }
}
