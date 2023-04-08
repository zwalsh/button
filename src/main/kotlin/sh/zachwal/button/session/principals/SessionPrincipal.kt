package sh.zachwal.button.session.principals

import org.slf4j.LoggerFactory
import java.time.Instant

private val logger = LoggerFactory.getLogger(SessionPrincipal::class.java)

interface SessionPrincipal {

    /**
     * time in epoch milliseconds at which this session expires
     */
    val expiration: Long

    fun isValid(): Boolean {
        val curEpochMilli = Instant.now().toEpochMilli()
        logger.info(
            "Validating $this " +
                "expiring at $expiration cur time $curEpochMilli, " +
                "valid: ${expiration > curEpochMilli}"
        )
        return expiration > curEpochMilli
    }
}
