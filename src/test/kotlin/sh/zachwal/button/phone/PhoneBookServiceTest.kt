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

    private val zachContact = Contact(1, Instant.now(), "Zach", "+18001234567", active = true)
    private val jackieContact = Contact(2, Instant.now(), "Jackie", "+18001225555", active = true)

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

    @Test
    fun `one admin text failure does not prevent future admin texts`() {
        coEvery {
            messagingService.sendMessage(
                any(),
                any()
            )
        } throws Exception("Oops!") andThen MessageQueued("", Instant.now())

        // should not fail
        runBlocking {
            phoneBookService.register("My Name", "123")
        }
        runBlocking {
            phoneBookService.register("Second one", "456")
        }

        coVerify(timeout = 1000) {
            messagingService.sendMessage(
                messagingConfig.adminPhone,
                "New contact just signed up:" +
                    " Second one at 456."
            )
        }
    }

    @Test
    fun `updateContactStatus updates contact to active`() {
        val inactiveContact = zachContact.copy(active = false)
        every {
            contactDAO.updateContactStatus(inactiveContact.id, true)
        } returns zachContact

        val returned = phoneBookService.updateContactStatus(inactiveContact.id, true)
        assertThat(returned).isEqualTo(UpdatedContact(zachContact))
        verify {
            contactDAO.updateContactStatus(inactiveContact.id, true)
        }
    }

    @Test
    fun `updateContactStatus updates contact to inactive`() {
        val inactiveContact = zachContact.copy(active = false)
        every {
            contactDAO.updateContactStatus(zachContact.id, false)
        } returns inactiveContact

        val returned = phoneBookService.updateContactStatus(inactiveContact.id, false)
        assertThat(returned).isEqualTo(UpdatedContact(inactiveContact))
        verify {
            contactDAO.updateContactStatus(inactiveContact.id, false)
        }
    }

    @Test
    fun `updateContactStatus on nonexistent contact returns ContactNotFound`() {
        every {
            contactDAO.updateContactStatus(zachContact.id, false)
        } returns null

        val returned = phoneBookService.updateContactStatus(inactiveContact.id, false)
        assertThat(returned).isEqualTo(ContactNotFound)
    }
}
