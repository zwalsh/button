package sh.zachwal.button.notify

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
import sh.zachwal.button.db.dao.NotificationDAO
import sh.zachwal.button.db.jdbi.Contact
import sh.zachwal.button.db.jdbi.Notification
import sh.zachwal.button.home.TOKEN_PARAMETER
import sh.zachwal.button.presser.Presser
import sh.zachwal.button.sms.ControlledContactMessagingService
import sh.zachwal.button.sms.MessageQueued
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertTrue

internal class ContactNotifierTest {

    private val contactDao: ContactDAO = mockk()
    private val notificationDAO: NotificationDAO = mockk()
    private val messagingService: ControlledContactMessagingService = mockk()
    private val contactTokenStore = mockk<ContactTokenStore> {
        every { createToken(any())  } returns "123"
        every { checkToken(any()) } returns 1
    }
    private val notifier = ContactNotifier(
        contactDao, messagingService, notificationDAO,
        "example.com",
        contactTokenStore
    )

    private val zachContact = Contact(1, Instant.now(), "Zach", "+18001234567", active = true)
    private val jackieContact = Contact(2, Instant.now(), "Jackie", "+18001225555", active = true)
    private val presser: Presser = mockk()

    @BeforeEach
    fun setup() {
        coEvery { messagingService.sendMessage(any(), any()) } returns MessageQueued(
            "blah",
            Instant.now()
        )
        every { contactDao.selectActiveContacts() } returns emptyList()
        every { notificationDAO.createNotification() } returns Notification(1, Instant.now())
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
