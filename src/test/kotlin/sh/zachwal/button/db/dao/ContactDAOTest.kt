package sh.zachwal.button.db.dao

import com.google.common.truth.Truth.assertThat
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import sh.zachwal.button.db.extension.DatabaseExtension
import java.time.Instant
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
}
