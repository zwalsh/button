package sh.zachwal.button.db.dao

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.jdbi.v3.core.Jdbi
import sh.zachwal.button.db.extension.DatabaseExtension
import sh.zachwal.button.db.jdbi.ContactPressCount
import java.time.LocalDate

@ExtendWith(DatabaseExtension::class)
class ContactPressCountDAOTest(private val jdbi: Jdbi) {

    private lateinit var dao: ContactPressCountDAO

    @BeforeEach
    fun setUp() {
        dao = jdbi.onDemand(ContactPressCountDAO::class.java)
        // Insert contacts 1 and 2 for FK constraint
        jdbi.useHandle<Exception> { h ->
            h.execute("insert into contact (id, created_date, name, phone_number, active) values (1, now(), 'Contact 1', '+15550001', true)")
            h.execute("insert into contact (id, created_date, name, phone_number, active) values (2, now(), 'Contact 2', '+15550002', true)")
        }
    }

    @Test
    fun `upsert and find by contact and date`() {
        val cpc = ContactPressCount(contactId = 1, date = LocalDate.parse("2025-12-01"), pressCount = 5)
        dao.upsert(cpc)
        val found = dao.find(1, LocalDate.parse("2025-12-01"))
        assertThat(found).isEqualTo(cpc)
    }

    @Test
    fun `upsert updates existing row`() {
        val cpc = ContactPressCount(1, LocalDate.parse("2025-12-01"), 5)
        dao.upsert(cpc)
        val updated = cpc.copy(pressCount = 10)
        dao.upsert(updated)
        val found = dao.find(1, LocalDate.parse("2025-12-01"))
        assertThat(found).isEqualTo(updated)
    }

    @Test
    fun `findAllForContact returns all rows for contact ordered by date desc`() {
        val c1 = ContactPressCount(1, LocalDate.parse("2025-12-01"), 5)
        val c2 = ContactPressCount(1, LocalDate.parse("2025-12-02"), 7)
        val c3 = ContactPressCount(1, LocalDate.parse("2025-12-03"), 2)
        dao.upsert(c1)
        dao.upsert(c2)
        dao.upsert(c3)
        val all = dao.findAllForContact(1)
        assertThat(all).containsExactly(c3, c2, c1)
    }

    @Test
    fun `aggregateCountsByContact returns correct sums for all contacts in range`() {
        // Contact 1: 5+7+2=14, Contact 2: 3+4=7
        dao.upsert(ContactPressCount(1, LocalDate.parse("2025-12-01"), 5))
        dao.upsert(ContactPressCount(1, LocalDate.parse("2025-12-02"), 7))
        dao.upsert(ContactPressCount(1, LocalDate.parse("2025-12-03"), 2))
        dao.upsert(ContactPressCount(2, LocalDate.parse("2025-12-01"), 3))
        dao.upsert(ContactPressCount(2, LocalDate.parse("2025-12-03"), 4))
        val result = dao.aggregateCountsByContact(
            LocalDate.parse("2025-12-01"),
            LocalDate.parse("2025-12-03")
        )
        assertThat(result).containsExactly(1, 14, 2, 7)
    }

    @Test
    fun `aggregateCountsByContact returns empty map if no data in range`() {
        val result = dao.aggregateCountsByContact(
            LocalDate.parse("2025-01-01"),
            LocalDate.parse("2025-01-31")
        )
        assertThat(result).isEmpty()
    }
}
