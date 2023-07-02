package sh.zachwal.button.db.dao

import org.jdbi.v3.sqlobject.statement.SqlQuery
import sh.zachwal.button.db.jdbi.Press
import sh.zachwal.button.db.jdbi.RecentPressCount
import java.time.Instant

interface PressDAO {

    @SqlQuery(
        """
            insert into public.press (remote, contact_id) values (:remote, :contactId)
            returning *
        """
    )
    fun createPress(remote: String, contactId: Int?): Press

    @SqlQuery(
        "select * from public.press where time > ? order by time"
    )
    fun selectSince(time: Instant): List<Press>

    @SqlQuery(
        "select count(*) from public.press where time > ?"
    )
    fun countSince(time: Instant): Long

    @SqlQuery(
        """
            select name, count(*), max(time) as mostRecent
            from press p
                left join contact c on p.contact_id = c.id
            where time > now() -interval '7 day'
            group by name
            order by max(time) desc;
        """
    )
    fun recentPresses(): List<RecentPressCount>
}
