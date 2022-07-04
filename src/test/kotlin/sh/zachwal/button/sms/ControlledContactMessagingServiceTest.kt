package sh.zachwal.button.sms

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import sh.zachwal.button.config.MessagingConfig
import sh.zachwal.button.db.dao.SentMessageDAO
import sh.zachwal.button.db.jdbi.Contact
import sh.zachwal.button.db.jdbi.SentMessage
import java.time.Instant

internal class ControlledContactMessagingServiceTest {

    private val messagingService: MessagingService = mockk()
    private val sentMessageDAO: SentMessageDAO = mockk()
    private val messagingConfig = MessagingConfig(
        monthlyLimit = 3
    )
    private val service = ControlledContactMessagingService(
        messagingService, sentMessageDAO, messagingConfig
    )

    private val contact = Contact(1, Instant.now(), "Zach", "+18001234567", active = true)

    @BeforeEach
    fun setup() {
        every { sentMessageDAO.recordSentMessage(any()) } returnsArgument 0
    }

    @Test
    fun `records in the db when a message is sent`() {
        every { sentMessageDAO.countSentSince(any()) } returns 2 // under limit of 3

        val response = MessageQueued(
            id = "SomeTwilioId",
            sentDate = Instant.now()
        )
        coEvery { messagingService.sendMessage(any(), any()) } returns response


        val message = runBlocking {
            service.sendMessage(contact, "body")
        }
        assertThat(message).isEqualTo(response)
        verify {
            sentMessageDAO.recordSentMessage(
                SentMessage(
                    response.id, response.sentDate, contact.id
                )
            )
        }
    }

    @Test
    fun `does not send if the count is over the limit`() {
        every { sentMessageDAO.countSentSince(any()) } returns 3 // at limit of 3

        assertThrows<MessageLimitExceededException> {
            runBlocking {
                service.sendMessage(contact, "body")
            }
        }
        coVerify(exactly = 0) {
            messagingService.sendMessage(any(), any())
        }
    }
}
