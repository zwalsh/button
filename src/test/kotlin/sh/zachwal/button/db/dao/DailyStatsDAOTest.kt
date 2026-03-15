package sh.zachwal.button.db.dao

import com.google.common.truth.Truth.assertThat
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import sh.zachwal.button.db.extension.DatabaseExtension
import java.time.LocalDate

@ExtendWith(DatabaseExtension::class)
class DailyStatsDAOTest(private val jdbi: Jdbi) {

    private lateinit var dao: DailyStatsDAO
    private val date = LocalDate.parse("2026-03-15")

    @BeforeEach
    fun setUp() {
        dao = jdbi.onDemand(DailyStatsDAO::class.java)
    }

    @Test
    fun `findByDate returns null when no row exists`() {
        assertThat(dao.findByDate(date)).isNull()
    }

    @Test
    fun `ensureRow creates a row with zero defaults`() {
        dao.ensureRow(date)
        val row = dao.findByDate(date)!!
        assertThat(row.date).isEqualTo(date)
        assertThat(row.peakConcurrent).isEqualTo(0)
        assertThat(row.totalPressCount).isEqualTo(0)
    }

    @Test
    fun `ensureRow is idempotent`() {
        dao.ensureRow(date)
        dao.ensureRow(date)
        val row = dao.findByDate(date)!!
        assertThat(row.peakConcurrent).isEqualTo(0)
    }

    @Test
    fun `incrementTotalPresses increments count`() {
        dao.ensureRow(date)
        dao.incrementTotalPresses(date)
        dao.incrementTotalPresses(date)
        val row = dao.findByDate(date)!!
        assertThat(row.totalPressCount).isEqualTo(2)
    }

    @Test
    fun `updatePeakIfHigher sets peak when higher`() {
        dao.ensureRow(date)
        dao.updatePeakIfHigher(date, 5)
        val row = dao.findByDate(date)!!
        assertThat(row.peakConcurrent).isEqualTo(5)
    }

    @Test
    fun `updatePeakIfHigher does not lower existing peak`() {
        dao.ensureRow(date)
        dao.updatePeakIfHigher(date, 5)
        dao.updatePeakIfHigher(date, 3)
        val row = dao.findByDate(date)!!
        assertThat(row.peakConcurrent).isEqualTo(5)
    }
}
