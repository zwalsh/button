package sh.zachwal.button.db.dao

import com.google.common.collect.Range
import com.google.common.truth.Truth.assertThat
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import sh.zachwal.button.db.extension.DatabaseExtension
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

@ExtendWith(DatabaseExtension::class)
class PressDAOTest(private val jdbi: Jdbi) {
    private lateinit var pressDAO: PressDAO
    private lateinit var contactDAO: ContactDAO

    @BeforeEach
    fun setup() {
        pressDAO = jdbi.onDemand(PressDAO::class.java)
        contactDAO = jdbi.onDemand(ContactDAO::class.java)
    }

    @Test
    fun `firstPressTimestampForContact returns correct timestamp`() {
        val contact = contactDAO.createContact("Alice", "+15550000001")
        val before = Instant.now()
        pressDAO.createPress("remote1", contact.id)
        val ts = pressDAO.firstPressTimestampForContact(contact.id)!!

        assertThat(ts.toInstant()).isIn(Range.closed(before, Instant.now()))
    }

    @Test
    fun `aggregatePressCountsByDate returns correct counts`() {
        val contact = contactDAO.createContact("Bob", "+15550000002")
        val today = LocalDate.now(ZoneOffset.UTC)
        val yesterday = today.minusDays(1)
        val twoDaysAgo = today.minusDays(2)
        // Insert presses for two days ago, yesterday, today
        pressDAO.createPressAtTime("remote", contact.id, Instant.now().minus(2, ChronoUnit.DAYS))
        pressDAO.createPressAtTime("remote", contact.id, Instant.now().minus(1, ChronoUnit.DAYS))
        pressDAO.createPressAtTime("remote", contact.id, Instant.now())
        val counts = pressDAO.aggregatePressCountsByDate(contact.id, twoDaysAgo, yesterday)
        assertEquals(1, counts[twoDaysAgo])
        assertEquals(1, counts[yesterday])
        assertEquals(null, counts[today])
    }
}
