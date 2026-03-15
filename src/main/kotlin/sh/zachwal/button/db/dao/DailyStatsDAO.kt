package sh.zachwal.button.db.dao

import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import sh.zachwal.button.db.jdbi.DailyStatsRow
import java.time.LocalDate

interface DailyStatsDAO {
    @SqlQuery("SELECT * FROM daily_stats WHERE date = :date")
    fun findByDate(@Bind("date") date: LocalDate): DailyStatsRow?

    @SqlUpdate("INSERT INTO daily_stats (date) VALUES (:date) ON CONFLICT DO NOTHING")
    fun ensureRow(@Bind("date") date: LocalDate)

    @SqlUpdate("UPDATE daily_stats SET total_press_count = total_press_count + 1 WHERE date = :date")
    fun incrementTotalPresses(@Bind("date") date: LocalDate)

    @SqlUpdate("UPDATE daily_stats SET peak_concurrent = GREATEST(peak_concurrent, :peak) WHERE date = :date")
    fun updatePeakIfHigher(@Bind("date") date: LocalDate, @Bind("peak") peak: Int)
}
