package sh.zachwal.button.sms

import com.google.inject.Inject
import com.google.inject.Singleton
import org.slf4j.LoggerFactory
import sh.zachwal.button.config.MessagingConfig
import sh.zachwal.button.db.dao.SentMessageDAO
import sh.zachwal.button.db.jdbi.Contact
import sh.zachwal.button.db.jdbi.SentMessage
import java.time.Instant
import java.time.temporal.ChronoUnit

class MessageLimitExceededException(
    val messagesSent: Int
) : IllegalStateException()

/**
 * Controls the sending of messages to contacts. Limits the total number that can be sent in a time
 * period. Records when a message is sent.
 */
@Singleton
class ControlledContactMessagingService @Inject constructor(
    private val messagingService: MessagingService,
    private val sentMessageDAO: SentMessageDAO,
    private val messagingConfig: MessagingConfig,
) {
    private val logger = LoggerFactory.getLogger(ControlledContactMessagingService::class.java)

    suspend fun sendMessage(contact: Contact, body: String): MessageStatus {
        if (contact.active.not()) {
            logger.info("Contact ${contact.id} is not active, not sending message")
            return MessageFailed("Contact is not active")
        }

        val currentCount = sentMessageDAO.countSentSince(Instant.now().minus(30, ChronoUnit.DAYS))
        if (currentCount >= messagingConfig.monthlyLimit) {
            throw MessageLimitExceededException(messagesSent = currentCount)
        }

        val messageStatus = messagingService.sendMessage(contact.phoneNumber, body)

        if (messageStatus is MessageQueued) {
            // TODO if this errors, need to stop sending messages
            sentMessageDAO.recordSentMessage(
                SentMessage(
                    messageStatus.id, messageStatus.sentDate, contact.id
                )
            )
        }
        return messageStatus
    }
}
