package sh.zachwal.button.sms

import com.google.inject.Inject
import com.google.inject.Singleton
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
    suspend fun sendMessage(contact: Contact, body: String): MessageStatus {
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
