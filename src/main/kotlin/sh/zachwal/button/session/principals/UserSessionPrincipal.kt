package sh.zachwal.button.session.principals

import io.ktor.auth.Principal
import org.slf4j.LoggerFactory
import java.time.Instant

data class UserSessionPrincipal constructor(
    val user: String,
    val expiration: Long // time in epoch milliseconds at which this session expires
) : Principal {

    private val logger = LoggerFactory.getLogger(UserSessionPrincipal::class.java)

    fun isValid(): Boolean {
        val curEpochMilli = Instant.now().toEpochMilli()
        logger.info("Validating session $user expiring at $expiration cur time $curEpochMilli, valid: ${expiration > curEpochMilli}")
        return expiration > curEpochMilli
    }
}
