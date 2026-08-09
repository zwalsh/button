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
    fun `updateNotificationPreferences disables notifications`() {
        val contact = dao.createContact("Alice", "+15550001")
        val updated = dao.updateNotificationPreferences(contact.id, false, null)
        assertThat(updated!!.notificationPreferences.notificationsEnabled).isFalse()
    }

    @Test
    fun `updateNotificationPreferences re-enables notifications`() {
        val contact = dao.createContact("Alice", "+15550001")
        dao.updateNotificationPreferences(contact.id, false, null)
        val updated = dao.updateNotificationPreferences(contact.id, true, null)
        assertThat(updated!!.notificationPreferences.notificationsEnabled).isTrue()
    }

    @Test
    fun `updateNotificationPreferences sets snoozedUntil`() {
        val contact = dao.createContact("Alice", "+15550001")
        val snoozedUntil = Instant.now().plus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS)
        val updated = dao.updateNotificationPreferences(contact.id, true, snoozedUntil)
        assertThat(updated!!.notificationPreferences.snoozedUntil).isEqualTo(snoozedUntil)
    }

    @Test
    fun `updateNotificationPreferences clears snoozedUntil`() {
        val contact = dao.createContact("Alice", "+15550001")
        val snoozedUntil = Instant.now().plus(7, ChronoUnit.DAYS)
        dao.updateNotificationPreferences(contact.id, true, snoozedUntil)
        val updated = dao.updateNotificationPreferences(contact.id, true, null)
        assertThat(updated!!.notificationPreferences.snoozedUntil).isNull()
    }
}
