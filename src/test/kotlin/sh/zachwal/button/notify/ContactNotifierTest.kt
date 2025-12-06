package sh.zachwal.button.notify

import io.mockk.Ordering
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import sh.zachwal.button.auth.contact.ContactTokenStore
import sh.zachwal.button.db.dao.ContactDAO
import sh.zachwal.button.db.dao.ContactPressCountDAO
import sh.zachwal.button.db.dao.NotificationDAO
import sh.zachwal.button.db.jdbi.Contact
import sh.zachwal.button.db.jdbi.Notification
import sh.zachwal.button.home.TOKEN_PARAMETER
import sh.zachwal.button.presser.Presser
import sh.zachwal.button.sms.ControlledContactMessagingService
import sh.zachwal.button.sms.MessageQueued
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.test.assertTrue

internal class ContactNotifierTest {

    private val contactDao: ContactDAO = mockk()
    private val notificationDAO: NotificationDAO = mockk()
    private val messagingService: ControlledContactMessagingService = mockk()
    private val contactTokenStore = mockk<ContactTokenStore> {
        every { createToken(any()) } returns "123"
        every { checkToken(any()) } returns 1
    }
    private val contactPressCountDAO = mockk<ContactPressCountDAO>()
    private val notifier = ContactNotifier(
        contactDAO = contactDao,
        contactPressCountDAO = contactPressCountDAO,
        controlledContactMessagingService = messagingService,
        notificationDAO = notificationDAO,
        host = "example.com",
        contactTokenStore = contactTokenStore
    )

    private val zachContact = Contact(1, Instant.now(), "Zach", "+18001234567", active = true)
    private val jackieContact = Contact(2, Instant.now(), "Jackie", "+18001225555", active = true)
    private val presser: Presser = mockk {
        every { contact } returns null
        every { remote() } returns "remote"
    }

    @BeforeEach
    fun setup() {
        coEvery { messagingService.sendMessage(any(), any()) } returns MessageQueued(
            "blah",
            Instant.now()
        )
        every { contactDao.selectActiveContacts() } returns emptyList()
        every { notificationDAO.createNotification() } returns Notification(1, Instant.now())
        every { contactPressCountDAO.aggregateCountsByContact(any(), any()) } returns mapOf(
            zachContact.id to 10,
            jackieContact.id to 5
        )
    }

    @Test
    fun `contacts are notified in order of press count`() {
        every { notificationDAO.getLatestNotification() } returns Notification(
            1,
            Instant.now().minus(25, ChronoUnit.HOURS)
        )
        every { contactDao.selectActiveContacts() } returns listOf(zachContact, jackieContact)
        // Zach has 5 presses, Jackie has 10
        every { contactPressCountDAO.aggregateCountsByContact(any(), any()) } returns mapOf(
            zachContact.id to 5,
            jackieContact.id to 10
        )
        runBlocking {
            notifier.pressed(presser)
        }
        // Jackie should be notified before Zach
        coVerify(timeout = 2000, ordering = Ordering.ORDERED) {
            messagingService.sendMessage(jackieContact, any())
            messagingService.sendMessage(zachContact, any())
        }
    }

    @Test
    fun `contacts with zero presses are notified last`() {
        val zeroPressContact = Contact(3, Instant.parse("2025-12-06T21:15:12.338Z"), "Zero", "+18009998888", active = true)
        every { notificationDAO.getLatestNotification() } returns Notification(
            1,
            Instant.now().minus(25, ChronoUnit.HOURS)
        )
        every { contactDao.selectActiveContacts() } returns listOf(zachContact, jackieContact, zeroPressContact)
        every { contactPressCountDAO.aggregateCountsByContact(any(), any()) } returns mapOf(
            zachContact.id to 5,
            jackieContact.id to 10
            // zeroPressContact.id not present, should default to 0
        )
        runBlocking {
            notifier.pressed(presser)
        }
        // zeroPressContact should be notified last
        coVerify(timeout = 2000, ordering = Ordering.ORDERED) {
            messagingService.sendMessage(jackieContact, any())
            messagingService.sendMessage(zachContact, any())
            messagingService.sendMessage(zeroPressContact, any())
        }
    }

    @Test
    fun `sends a message for each contact`() {
        every { notificationDAO.getLatestNotification() } returns Notification(
            1,
            Instant.now().minus(25, ChronoUnit.HOURS)
        )
        every { contactDao.selectActiveContacts() } returns listOf(zachContact, jackieContact)

        runBlocking {
            notifier.pressed(presser)
        }

        coVerify(timeout = 2000) {
            messagingService.sendMessage(zachContact, any())
            messagingService.sendMessage(jackieContact, any())
        }
    }

    @Test
    fun `contact gets a message with a specific token`() {
        every { notificationDAO.getLatestNotification() } returns Notification(
            1,
            Instant.now().minus(25, ChronoUnit.HOURS)
        )
        every { contactDao.selectActiveContacts() } returns listOf(zachContact)

        runBlocking {
            notifier.pressed(presser)
        }
        val message = slot<String>()
        coVerify(timeout = 2000) {
            messagingService.sendMessage(zachContact, capture(message))
        }
        assertTrue(message.captured.startsWith("Someone's pressing The Button! Join in: "))
        val token = message.captured.substringAfter("$TOKEN_PARAMETER=")
        assertEquals(zachContact.id, contactTokenStore.checkToken(token))
    }

    @Test
    fun `does not block while sending messages`() {
        every { notificationDAO.getLatestNotification() } returns Notification(
            1,
            Instant.now().minus(25, ChronoUnit.HOURS)
        )
        every { contactDao.selectActiveContacts() } returns List(10) { jackieContact }

        assertDoesNotThrow {
            runBlocking {
                withTimeout(timeMillis = 500) {
                    notifier.pressed(presser)
                }
            }
        }
    }

    @Test
    fun `does not send a notification if the prior notification was within a day`() {
        val almostOneDayAgo = Instant.now()
            .minus(1, ChronoUnit.DAYS)
            .plus(1, ChronoUnit.MINUTES)
        every { notificationDAO.getLatestNotification() } returns Notification(1, almostOneDayAgo)

        runBlocking {
            notifier.pressed(presser)
        }

        coVerify(exactly = 0, timeout = 1000) {
            messagingService.sendMessage(any(), any())
        }
    }

    @Test
    fun `sends notification if there is no prior notification`() {
        every { notificationDAO.getLatestNotification() } returns null
        every { contactDao.selectActiveContacts() } returns listOf(zachContact)

        runBlocking {
            notifier.pressed(presser)
        }

        coVerify(timeout = 2000) {
            messagingService.sendMessage(zachContact, any())
        }
    }

    @Test
    fun `creates new notification record`() {
        val overOneDayAgo = Instant.now().minus(25, ChronoUnit.HOURS)
        every { notificationDAO.getLatestNotification() } returns Notification(1, overOneDayAgo)

        runBlocking {
            notifier.pressed(presser)
        }

        verify(timeout = 1000) { notificationDAO.createNotification() }
    }
}
