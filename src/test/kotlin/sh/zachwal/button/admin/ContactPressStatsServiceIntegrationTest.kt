package sh.zachwal.button.admin

import com.google.common.truth.Truth.assertThat
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import sh.zachwal.button.db.dao.ContactDAO
import sh.zachwal.button.db.dao.ContactPressCountDAO
import sh.zachwal.button.db.dao.PressDAO
import sh.zachwal.button.db.extension.DatabaseExtension
import sh.zachwal.button.db.jdbi.Contact
import sh.zachwal.button.db.jdbi.ContactPressCount
import java.time.LocalDate
import java.time.ZoneOffset

@ExtendWith(DatabaseExtension::class)
class ContactPressStatsServiceIntegrationTest(private val jdbi: Jdbi) {

    private lateinit var service: ContactPressStatsService

    private lateinit var alice: Contact
    private lateinit var bob: Contact
    private lateinit var carol: Contact

    private val today = LocalDate.now(ZoneOffset.UTC)

    @BeforeEach
    fun setup() {
        val contactDAO = jdbi.onDemand(ContactDAO::class.java)
        val contactPressCountDAO = jdbi.onDemand(ContactPressCountDAO::class.java)
        val pressDAO = jdbi.onDemand(PressDAO::class.java)

        service = ContactPressStatsService(contactDAO, contactPressCountDAO, pressDAO)

        alice = contactDAO.createContact("Alice", "+15550000001")
        bob = contactDAO.createContact("Bob", "+15550000002")
        carol = contactDAO.createContact("Carol", "+15550000003") // no presses — always excluded

        // Materialized counts within last 30 days
        // Alice: day -15: 5, day -5: 7 → 12 materialized in last 30d
        contactPressCountDAO.upsert(ContactPressCount(alice.id, today.minusDays(15), 5))
        contactPressCountDAO.upsert(ContactPressCount(alice.id, today.minusDays(5), 7))
        // Alice: older than 30 days (only visible in ALL_TIME)
        contactPressCountDAO.upsert(ContactPressCount(alice.id, today.minusDays(60), 20))
        // Bob: day -10 — inside last 30d but outside last 7d
        contactPressCountDAO.upsert(ContactPressCount(bob.id, today.minusDays(10), 8))

        // Live presses today: Alice 2, Bob 5
        repeat(2) { pressDAO.createPress("127.0.0.1", alice.id) }
        repeat(5) { pressDAO.createPress("127.0.0.1", bob.id) }
    }

    @Test
    fun `TODAY returns only live press counts from the press table`() {
        val result = service.pressStats(TimeRange.TODAY)
        val byName = result.associateBy { it.contact.name }

        assertThat(byName["Alice"]!!.count).isEqualTo(2)
        assertThat(byName["Bob"]!!.count).isEqualTo(5)
        assertThat(byName.keys).doesNotContain("Carol")
    }

    @Test
    fun `LAST_7_DAYS excludes materialized rows older than 7 days`() {
        val result = service.pressStats(TimeRange.LAST_7_DAYS)
        val byName = result.associateBy { it.contact.name }

        // Alice: day -5 (7) + 2 today = 9; day -15 excluded
        assertThat(byName["Alice"]!!.count).isEqualTo(9)
        // Bob: day -10 excluded; only 5 today
        assertThat(byName["Bob"]!!.count).isEqualTo(5)
    }

    @Test
    fun `LAST_30_DAYS merges materialized and live counts and sorts descending`() {
        val result = service.pressStats(TimeRange.LAST_30_DAYS)
        val byName = result.associateBy { it.contact.name }

        assertThat(byName["Alice"]!!.count).isEqualTo(14) // 5+7+2
        assertThat(byName["Bob"]!!.count).isEqualTo(13)   // 8+5
        assertThat(result.map { it.count }).isInOrder(Comparator.reverseOrder<Long>())
        assertThat(byName.keys).doesNotContain("Carol")
    }

    @Test
    fun `ALL_TIME includes materialized rows older than 30 days`() {
        val result = service.pressStats(TimeRange.ALL_TIME)
        val byName = result.associateBy { it.contact.name }

        // Alice: 5+7+20 materialized + 2 today = 34
        assertThat(byName["Alice"]!!.count).isEqualTo(34)
        // Bob: no old presses, same as LAST_30_DAYS
        assertThat(byName["Bob"]!!.count).isEqualTo(13)
    }

    @Test
    fun `contacts with no presses are excluded from all ranges`() {
        for (range in TimeRange.values()) {
            val names = service.pressStats(range).map { it.contact.name }
            assertThat(names).doesNotContain("Carol")
        }
    }
}
