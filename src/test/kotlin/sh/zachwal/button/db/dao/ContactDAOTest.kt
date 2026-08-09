package sh.zachwal.button.db.dao

import com.google.common.truth.Truth.assertThat
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.statement.UnableToExecuteStatementException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import sh.zachwal.button.db.extension.DatabaseExtension
import java.time.Instant
import java.time.LocalTime
import java.time.temporal.ChronoUnit

@ExtendWith(DatabaseExtension::class)
class ContactDAOTest(private val jdbi: Jdbi) {

    private lateinit var dao: ContactDAO

    @BeforeEach
    fun setUp() {
        dao = jdbi.onDemand(ContactDAO::class.java)
    }

    @Test
    fun `contacts have notifications enabled by default`() {
        val contact = dao.createContact("Alice", "+15550001")
        assertThat(contact.notificationPreferences.notificationsEnabled).isTrue()
    }

    @Test
    fun `updateNotificationsEnabled disables notifications`() {
        val contact = dao.createContact("Alice", "+15550001")
        val updated = dao.updateNotificationsEnabled(contact.id, false)
        assertThat(updated!!.notificationPreferences.notificationsEnabled).isFalse()
    }

    @Test
    fun `updateNotificationsEnabled re-enables notifications`() {
        val contact = dao.createContact("Alice", "+15550001")
        dao.updateNotificationsEnabled(contact.id, false)
        val updated = dao.updateNotificationsEnabled(contact.id, true)
        assertThat(updated!!.notificationPreferences.notificationsEnabled).isTrue()
    }

    @Test
    fun `updateNotificationsEnabled leaves snoozedUntil untouched`() {
        val contact = dao.createContact("Alice", "+15550001")
        val snoozedUntil = Instant.now().plus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS)
        dao.updateSnoozedUntil(contact.id, snoozedUntil)
        val updated = dao.updateNotificationsEnabled(contact.id, false)
        assertThat(updated!!.notificationPreferences.snoozedUntil).isEqualTo(snoozedUntil)
    }

    @Test
    fun `updateSnoozedUntil sets snoozedUntil`() {
        val contact = dao.createContact("Alice", "+15550001")
        val snoozedUntil = Instant.now().plus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS)
        val updated = dao.updateSnoozedUntil(contact.id, snoozedUntil)
        assertThat(updated!!.notificationPreferences.snoozedUntil).isEqualTo(snoozedUntil)
    }

    @Test
    fun `updateSnoozedUntil clears snoozedUntil`() {
        val contact = dao.createContact("Alice", "+15550001")
        val snoozedUntil = Instant.now().plus(7, ChronoUnit.DAYS)
        dao.updateSnoozedUntil(contact.id, snoozedUntil)
        val updated = dao.updateSnoozedUntil(contact.id, null)
        assertThat(updated!!.notificationPreferences.snoozedUntil).isNull()
    }

    @Test
    fun `updateSnoozedUntil leaves notificationsEnabled untouched`() {
        val contact = dao.createContact("Alice", "+15550001")
        dao.updateNotificationsEnabled(contact.id, false)
        val updated = dao.updateSnoozedUntil(contact.id, Instant.now().plus(1, ChronoUnit.DAYS))
        assertThat(updated!!.notificationPreferences.notificationsEnabled).isFalse()
    }

    @Test
    fun `contacts default to America New_York timezone`() {
        val contact = dao.createContact("Alice", "+15550001")
        assertThat(contact.notificationPreferences.timezone).isEqualTo("America/New_York")
    }

    @Test
    fun `updateQuietHours round-trips quiet hours and timezone`() {
        val contact = dao.createContact("Alice", "+15550001")
        val start = LocalTime.of(23, 0)
        val end = LocalTime.of(7, 0)
        val updated = dao.updateQuietHours(contact.id, start, end, "America/Los_Angeles")

        assertThat(updated!!.notificationPreferences.quietHoursStart).isEqualTo(start)
        assertThat(updated.notificationPreferences.quietHoursEnd).isEqualTo(end)
        assertThat(updated.notificationPreferences.timezone).isEqualTo("America/Los_Angeles")
    }

    @Test
    fun `updateQuietHours allows timezone alone without quiet hours`() {
        val contact = dao.createContact("Alice", "+15550001")
        val updated = dao.updateQuietHours(contact.id, null, null, "Europe/London")

        assertThat(updated!!.notificationPreferences.quietHoursStart).isNull()
        assertThat(updated.notificationPreferences.quietHoursEnd).isNull()
        assertThat(updated.notificationPreferences.timezone).isEqualTo("Europe/London")
    }

    @Test
    fun `updateQuietHours rejects quietHoursStart without timezone`() {
        val contact = dao.createContact("Alice", "+15550001")

        assertThrows(UnableToExecuteStatementException::class.java) {
            dao.updateQuietHours(contact.id, LocalTime.of(23, 0), LocalTime.of(7, 0), null)
        }
    }
}
