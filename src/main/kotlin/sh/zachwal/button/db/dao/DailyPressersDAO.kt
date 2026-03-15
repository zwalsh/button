package sh.zachwal.button.db.dao

import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery
import java.time.LocalDate

interface DailyPressersDAO {
    @SqlQuery("SELECT presser_id FROM daily_pressers WHERE date = :date")
    fun findByDate(@Bind("date") date: LocalDate): List<String>

    // Returns the presser_id if the row was inserted (presser is new for this date), null on conflict
    @SqlQuery(
        """
        INSERT INTO daily_pressers (date, presser_id) VALUES (:date, :presserId)
        ON CONFLICT DO NOTHING
        RETURNING presser_id
        """
    )
    fun insertIfAbsent(@Bind("date") date: LocalDate, @Bind("presserId") presserId: String): String?
}
