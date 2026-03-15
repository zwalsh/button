package sh.zachwal.button.db.dao

import com.google.common.truth.Truth.assertThat
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import sh.zachwal.button.db.extension.DatabaseExtension
import java.time.LocalDate

@ExtendWith(DatabaseExtension::class)
class DailyPressersDAOTest(private val jdbi: Jdbi) {

    private lateinit var dao: DailyPressersDAO
    private val date = LocalDate.parse("2026-03-15")

    @BeforeEach
    fun setUp() {
        dao = jdbi.onDemand(DailyPressersDAO::class.java)
    }

    @Test
    fun `findByDate returns empty list when no rows`() {
        assertThat(dao.findByDate(date)).isEmpty()
    }

    @Test
    fun `insertIfAbsent returns presser_id on first insert`() {
        val result = dao.insertIfAbsent(date, "presser-1")
        assertThat(result).isEqualTo("presser-1")
    }

    @Test
    fun `insertIfAbsent returns null on duplicate`() {
        dao.insertIfAbsent(date, "presser-1")
        val result = dao.insertIfAbsent(date, "presser-1")
        assertThat(result).isNull()
    }

    @Test
    fun `findByDate returns all pressers for that date`() {
        dao.insertIfAbsent(date, "presser-1")
        dao.insertIfAbsent(date, "presser-2")
        dao.insertIfAbsent(date, "presser-3")
        val results = dao.findByDate(date)
        assertThat(results).containsExactlyElementsIn(listOf("presser-1", "presser-2", "presser-3"))
    }

    @Test
    fun `findByDate does not return rows for other dates`() {
        val otherDate = date.plusDays(1)
        dao.insertIfAbsent(date, "presser-1")
        dao.insertIfAbsent(otherDate, "presser-2")
        assertThat(dao.findByDate(date)).containsExactly("presser-1")
        assertThat(dao.findByDate(otherDate)).containsExactly("presser-2")
    }
}
