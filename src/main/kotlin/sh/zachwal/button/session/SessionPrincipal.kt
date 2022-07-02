package sh.zachwal.button.session

import io.ktor.auth.Principal
import org.slf4j.LoggerFactory
import java.time.Instant

private val logger = LoggerFactory.getLogger(SessionPrincipal::class.java)

data class SessionPrincipal(
    val user: String,
    val expiration: Long // time in epoch milliseconds at which this session expires
) : Principal {
    fun isValid(): Boolean {
        val curEpochMilli = Instant.now().toEpochMilli()
        logger.info("Validating session $user expiring at $expiration cur time $curEpochMilli, valid: ${expiration > curEpochMilli}")
        return expiration > curEpochMilli
    }
}
