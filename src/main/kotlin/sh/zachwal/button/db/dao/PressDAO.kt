package sh.zachwal.button.db.dao

import org.jdbi.v3.sqlobject.statement.SqlQuery
import sh.zachwal.button.db.jdbi.Press
import java.time.Instant

interface PressDAO {

    @SqlQuery(
        """
            insert into public.press (remote) values (:remote)
            returning *
        """
    )
    fun createPress(remote: String): Press

    @SqlQuery(
        "select * from public.press where time > ? order by time"
    )
    fun selectSince(time: Instant): List<Press>

    @SqlQuery(
        "select count(*) from public.press where time > ?"
    )
    fun countSince(time: Instant): Long
}
