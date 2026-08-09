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
import sh.zachwal.button.db.jdbi.Notification
import sh.zachwal.button.db.jdbi.NotificationPreferences
import sh.zachwal.button.db.jdbi.contact
import sh.zachwal.button.home.TOKEN_PARAMETER
import sh.zachwal.button.presser.Presser
import sh.zachwal.button.sms.ControlledContactMessagingService
import sh.zachwal.button.sms.MessageQueued
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.test.assertFalse
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

    private val zachContact = contact(id = 1, name = "Zach", phoneNumber = "+18001234567")
    private val jackieContact = contact(id = 2, name = "Jackie", phoneNumber = "+18001225555")
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
        val zeroPressContact = contact(id = 3, name = "Zero", phoneNumber = "+18009998888")
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
    fun `does not notify contacts with notifications disabled`() {
        val disabledContact = contact(id = 3, name = "Opted Out", phoneNumber = "+18005550000", notificationsEnabled = false)
        every { notificationDAO.getLatestNotification() } returns Notification(
            1,
            Instant.now().minus(25, ChronoUnit.HOURS)
        )
        every { contactDao.selectActiveContacts() } returns listOf(zachContact, disabledContact)

        runBlocking {
            notifier.pressed(presser)
        }

        coVerify(timeout = 2000) { messagingService.sendMessage(zachContact, any()) }
        coVerify(exactly = 0, timeout = 1000) { messagingService.sendMessage(disabledContact, any()) }
    }

    @Test
    fun `does not notify contacts who are snoozed`() {
        val snoozedContact = contact(
            id = 3,
            name = "Snoozed",
            phoneNumber = "+18005551111",
            snoozedUntil = Instant.now().plus(1, ChronoUnit.DAYS),
        )
        every { notificationDAO.getLatestNotification() } returns Notification(
            1,
            Instant.now().minus(25, ChronoUnit.HOURS)
        )
        every { contactDao.selectActiveContacts() } returns listOf(zachContact, snoozedContact)

        runBlocking {
            notifier.pressed(presser)
        }

        coVerify(timeout = 2000) { messagingService.sendMessage(zachContact, any()) }
        coVerify(exactly = 0, timeout = 1000) { messagingService.sendMessage(snoozedContact, any()) }
    }

    @Test
    fun `notifies contacts whose snooze has expired`() {
        val expiredSnoozeContact = contact(
            id = 3,
            name = "No Longer Snoozed",
            phoneNumber = "+18005552222",
            snoozedUntil = Instant.now().minus(1, ChronoUnit.DAYS),
        )
        every { notificationDAO.getLatestNotification() } returns Notification(
            1,
            Instant.now().minus(25, ChronoUnit.HOURS)
        )
        every { contactDao.selectActiveContacts() } returns listOf(expiredSnoozeContact)

        runBlocking {
            notifier.pressed(presser)
        }

        coVerify(timeout = 2000) { messagingService.sendMessage(expiredSnoozeContact, any()) }
    }

    @Test
    fun `does not notify contacts who are in quiet hours`() {
        // Window built from the actual current UTC time so it always contains "now",
        // regardless of when the test runs (the real code calls Instant.now() internally).
        val nowLocal = LocalTime.now(ZoneOffset.UTC)
        val quietContact = contact(
            id = 3,
            name = "Quiet",
            phoneNumber = "+18005553333",
            quietHoursStart = nowLocal.minusMinutes(1),
            quietHoursEnd = nowLocal.plusHours(1),
            timezone = "UTC",
        )
        every { notificationDAO.getLatestNotification() } returns Notification(
            1,
            Instant.now().minus(25, ChronoUnit.HOURS)
        )
        every { contactDao.selectActiveContacts() } returns listOf(zachContact, quietContact)

        runBlocking {
            notifier.pressed(presser)
        }

        coVerify(timeout = 2000) { messagingService.sendMessage(zachContact, any()) }
        coVerify(exactly = 0, timeout = 1000) { messagingService.sendMessage(quietContact, any()) }
    }

    @Test
    fun `isInQuietHours - standard window`() {
        // window 22:00-23:00 UTC, not wrapping
        val prefs = NotificationPreferences(
            notificationsEnabled = true,
            snoozedUntil = null,
            quietHoursStart = LocalTime.of(22, 0),
            quietHoursEnd = LocalTime.of(23, 0),
            timezone = "UTC",
        )
        val inside = Instant.parse("2026-01-01T22:30:00Z")
        val outside = Instant.parse("2026-01-01T21:00:00Z")
        val atStart = Instant.parse("2026-01-01T22:00:00Z")
        val atEnd = Instant.parse("2026-01-01T23:00:00Z")

        assertTrue(notifier.isInQuietHours(prefs, inside))
        assertFalse(notifier.isInQuietHours(prefs, outside))
        assertTrue(notifier.isInQuietHours(prefs, atStart))
        assertFalse(notifier.isInQuietHours(prefs, atEnd))
    }

    @Test
    fun `isInQuietHours - midnight-wrapping window`() {
        val prefs = NotificationPreferences(
            notificationsEnabled = true,
            snoozedUntil = null,
            quietHoursStart = LocalTime.of(23, 0),
            quietHoursEnd = LocalTime.of(7, 0),
            timezone = "UTC",
        )
        val insideBeforeMidnight = Instant.parse("2026-01-01T23:30:00Z")
        val insideAfterMidnight = Instant.parse("2026-01-01T03:00:00Z")
        val outside = Instant.parse("2026-01-01T12:00:00Z")
        val atStart = Instant.parse("2026-01-01T23:00:00Z")
        val atEnd = Instant.parse("2026-01-01T07:00:00Z")

        assertTrue(notifier.isInQuietHours(prefs, insideBeforeMidnight))
        assertTrue(notifier.isInQuietHours(prefs, insideAfterMidnight))
        assertFalse(notifier.isInQuietHours(prefs, outside))
        assertTrue(notifier.isInQuietHours(prefs, atStart))
        assertFalse(notifier.isInQuietHours(prefs, atEnd))
    }

    @Test
    fun `isInQuietHours - non-wrapping window America New York`() {
        // window 22:00-23:00 local, EST (UTC-5), no DST in January
        val zone = "America/New_York"
        val date = LocalDate.of(2026, 1, 15)
        val prefs = NotificationPreferences(
            notificationsEnabled = true,
            snoozedUntil = null,
            quietHoursStart = LocalTime.of(22, 0),
            quietHoursEnd = LocalTime.of(23, 0),
            timezone = zone,
        )
        val inside = zonedInstant(date, LocalTime.of(22, 30), zone)
        val outside = zonedInstant(date, LocalTime.of(21, 0), zone)
        val atStart = zonedInstant(date, LocalTime.of(22, 0), zone)
        val atEnd = zonedInstant(date, LocalTime.of(23, 0), zone)

        assertTrue(notifier.isInQuietHours(prefs, inside))
        assertFalse(notifier.isInQuietHours(prefs, outside))
        assertTrue(notifier.isInQuietHours(prefs, atStart))
        assertFalse(notifier.isInQuietHours(prefs, atEnd))
    }

    @Test
    fun `isInQuietHours - midnight-wrapping window America New York`() {
        // window 23:00-07:00 local, EST (UTC-5), no DST in January
        val zone = "America/New_York"
        val date = LocalDate.of(2026, 1, 15)
        val nextDay = date.plusDays(1)
        val prefs = NotificationPreferences(
            notificationsEnabled = true,
            snoozedUntil = null,
            quietHoursStart = LocalTime.of(23, 0),
            quietHoursEnd = LocalTime.of(7, 0),
            timezone = zone,
        )
        val insideBeforeMidnight = zonedInstant(date, LocalTime.of(23, 30), zone)
        val insideAfterMidnight = zonedInstant(nextDay, LocalTime.of(3, 0), zone)
        val outside = zonedInstant(nextDay, LocalTime.of(12, 0), zone)
        val atStart = zonedInstant(date, LocalTime.of(23, 0), zone)
        val atEnd = zonedInstant(nextDay, LocalTime.of(7, 0), zone)

        assertTrue(notifier.isInQuietHours(prefs, insideBeforeMidnight))
        assertTrue(notifier.isInQuietHours(prefs, insideAfterMidnight))
        assertFalse(notifier.isInQuietHours(prefs, outside))
        assertTrue(notifier.isInQuietHours(prefs, atStart))
        assertFalse(notifier.isInQuietHours(prefs, atEnd))
    }

    @Test
    fun `isInQuietHours - non-wrapping window America Los Angeles`() {
        // window 22:00-23:00 local, PST (UTC-8), no DST in January
        val zone = "America/Los_Angeles"
        val date = LocalDate.of(2026, 1, 15)
        val prefs = NotificationPreferences(
            notificationsEnabled = true,
            snoozedUntil = null,
            quietHoursStart = LocalTime.of(22, 0),
            quietHoursEnd = LocalTime.of(23, 0),
            timezone = zone,
        )
        val inside = zonedInstant(date, LocalTime.of(22, 30), zone)
        val outside = zonedInstant(date, LocalTime.of(21, 0), zone)
        val atStart = zonedInstant(date, LocalTime.of(22, 0), zone)
        val atEnd = zonedInstant(date, LocalTime.of(23, 0), zone)

        assertTrue(notifier.isInQuietHours(prefs, inside))
        assertFalse(notifier.isInQuietHours(prefs, outside))
        assertTrue(notifier.isInQuietHours(prefs, atStart))
        assertFalse(notifier.isInQuietHours(prefs, atEnd))
    }

    @Test
    fun `isInQuietHours - midnight-wrapping window America Los Angeles`() {
        // window 23:00-07:00 local, PST (UTC-8), no DST in January
        val zone = "America/Los_Angeles"
        val date = LocalDate.of(2026, 1, 15)
        val nextDay = date.plusDays(1)
        val prefs = NotificationPreferences(
            notificationsEnabled = true,
            snoozedUntil = null,
            quietHoursStart = LocalTime.of(23, 0),
            quietHoursEnd = LocalTime.of(7, 0),
            timezone = zone,
        )
        val insideBeforeMidnight = zonedInstant(date, LocalTime.of(23, 30), zone)
        val insideAfterMidnight = zonedInstant(nextDay, LocalTime.of(3, 0), zone)
        val outside = zonedInstant(nextDay, LocalTime.of(12, 0), zone)
        val atStart = zonedInstant(date, LocalTime.of(23, 0), zone)
        val atEnd = zonedInstant(nextDay, LocalTime.of(7, 0), zone)

        assertTrue(notifier.isInQuietHours(prefs, insideBeforeMidnight))
        assertTrue(notifier.isInQuietHours(prefs, insideAfterMidnight))
        assertFalse(notifier.isInQuietHours(prefs, outside))
        assertTrue(notifier.isInQuietHours(prefs, atStart))
        assertFalse(notifier.isInQuietHours(prefs, atEnd))
    }

    @Test
    fun `isInQuietHours - null timezone or start returns false`() {
        val noTimezone = NotificationPreferences(
            notificationsEnabled = true,
            snoozedUntil = null,
            quietHoursStart = LocalTime.of(23, 0),
            quietHoursEnd = LocalTime.of(7, 0),
            timezone = null,
        )
        val noStart = NotificationPreferences(
            notificationsEnabled = true,
            snoozedUntil = null,
            quietHoursStart = null,
            quietHoursEnd = LocalTime.of(7, 0),
            timezone = "UTC",
        )
        val now = Instant.now()

        assertFalse(notifier.isInQuietHours(noTimezone, now))
        assertFalse(notifier.isInQuietHours(noStart, now))
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

    private fun zonedInstant(date: LocalDate, time: LocalTime, zone: String): Instant =
        ZonedDateTime.of(date, time, ZoneId.of(zone)).toInstant()
}
