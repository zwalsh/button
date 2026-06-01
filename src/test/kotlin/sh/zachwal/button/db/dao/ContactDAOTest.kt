package sh.zachwal.button.db.dao

import com.google.common.truth.Truth.assertThat
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import sh.zachwal.button.db.extension.DatabaseExtension

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
        val updated = dao.updateNotificationPreferences(contact.id, false)
        assertThat(updated!!.notificationPreferences.notificationsEnabled).isFalse()
    }

    @Test
    fun `updateNotificationPreferences re-enables notifications`() {
        val contact = dao.createContact("Alice", "+15550001")
        dao.updateNotificationPreferences(contact.id, false)
        val updated = dao.updateNotificationPreferences(contact.id, true)
        assertThat(updated!!.notificationPreferences.notificationsEnabled).isTrue()
    }
}
