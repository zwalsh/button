package sh.zachwal.button.db.dao

import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import sh.zachwal.button.db.jdbi.Press
import java.time.Instant

interface PressDAO {

    @SqlUpdate(
        """
            insert into public.press (ip) values (:ip)
            returning *
        """
    )
    fun createPress(ip: String): Press

    @SqlQuery(
        "select * from public.press where time > ? order by time"
    )
    fun selectSince(time: Instant): List<Press>
}
