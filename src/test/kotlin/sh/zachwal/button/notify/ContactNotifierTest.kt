package sh.zachwal.button.notify

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import sh.zachwal.button.db.dao.ContactDAO
import sh.zachwal.button.db.jdbi.Contact
import sh.zachwal.button.presser.Presser
import sh.zachwal.button.sms.ControlledContactMessagingService
import sh.zachwal.button.sms.MessageQueued
import java.time.Instant

internal class ContactNotifierTest {

    private val contactDao: ContactDAO = mockk()
    private val messagingService: ControlledContactMessagingService = mockk()
    private val notifier = ContactNotifier(contactDao, messagingService)

    private val zachContact = Contact(1, Instant.now(), "Zach", "+18001234567", active = true)
    private val jackieContact = Contact(2, Instant.now(), "Jackie", "+18001225555", active = true)

    @Test
    fun `sends a message for each contact`() {
        every { contactDao.selectActiveContacts() } returns listOf(zachContact, jackieContact)
        coEvery { messagingService.sendMessage(any(), any()) } returns MessageQueued("blah",
            Instant.now())

        val presser: Presser = mockk()
        runBlocking {
            notifier.pressed(presser)
        }

        coVerify {
            messagingService.sendMessage(zachContact, any())
            messagingService.sendMessage(jackieContact, any())
        }
    }
}
