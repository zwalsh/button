package sh.zachwal.button.phone

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import sh.zachwal.button.config.MessagingConfig
import sh.zachwal.button.db.dao.ContactDAO
import sh.zachwal.button.db.jdbi.Contact
import sh.zachwal.button.sms.InvalidNumber
import sh.zachwal.button.sms.MessageQueued
import sh.zachwal.button.sms.MessagingService
import sh.zachwal.button.sms.ValidNumber
import java.time.Instant

internal class PhoneBookServiceTest {

    private val messagingService: MessagingService = mockk()
    private val contactDAO: ContactDAO = mockk()

    private val messagingConfig = MessagingConfig(monthlyLimit = 600, adminPhone = "+18001234567")
    private val phoneBookService = PhoneBookService(messagingService, contactDAO, messagingConfig)

    @BeforeEach
    fun setup() {
        coEvery { messagingService.sendMessage(any(), any()) } returns MessageQueued(
            "",
            Instant.now()
        )
        coEvery { messagingService.validateNumber(any()) } answers {
            ValidNumber(firstArg())
        }
        every { contactDAO.createContact(any(), any()) } answers {
            Contact(1, Instant.now(), firstArg(), secondArg(), true)
        }
    }

    @Test
    fun `throws when phone number is invalid`() {
        val reason = "oops invalid!"
        coEvery { messagingService.validateNumber(any()) } returns InvalidNumber(
            "123", reason
        )

        assertThrows<InvalidNumberException> {
            runBlocking {
                phoneBookService.register("My Name", "123")
            }
        }
    }

    @Test
    fun `persists when phone number is valid`() {
        val validNumber = "+18001234567"
        coEvery { messagingService.validateNumber(any()) } returns ValidNumber(validNumber)

        val contact = runBlocking {
            phoneBookService.register("My Name", "123")
        }
        assertThat(contact.name).isEqualTo("My Name")
        assertThat(contact.phoneNumber).isEqualTo(validNumber)
    }

    @Test
    fun `sends message to admin phone on new contact`() {
        runBlocking {
            phoneBookService.register("New Contact", "123")
        }

        coVerify(timeout = 1000) {
            messagingService.sendMessage(messagingConfig.adminPhone, any())
        }
    }

    @Test
    fun `handles admin text failure`() {
        coEvery { messagingService.sendMessage(any(), any()) } throws Exception("Oops!")

        // should not fail
        val contact = runBlocking {
            phoneBookService.register("My Name", "123")
        }
        assertThat(contact.name).isEqualTo("My Name")
        assertThat(contact.phoneNumber).isEqualTo("123")
    }
}
