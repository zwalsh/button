package sh.zachwal.button.sms

import com.google.inject.Inject
import com.google.inject.Singleton
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

/**
 * Logs to standard out instead of sending a message. Useful in dev.
 */
@Singleton
class StdOutMessagingService @Inject constructor() : MessagingService {

    private val logger = LoggerFactory.getLogger(StdOutMessagingService::class.java)

    override suspend fun validateNumber(phoneNumber: String): PhoneNumberValidation {
        logger.info("Validating $phoneNumber")
        return ValidNumber("+1$phoneNumber")
    }

    override suspend fun sendMessage(toPhoneNumber: String, body: String): MessageStatus {
        logger.info("Sending $body to $toPhoneNumber")
        return MessageQueued(UUID.randomUUID().toString(), Instant.now())
    }
}
