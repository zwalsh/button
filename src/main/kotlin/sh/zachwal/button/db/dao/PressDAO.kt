package sh.zachwal.button.db.dao

import org.jdbi.v3.sqlobject.config.KeyColumn
import org.jdbi.v3.sqlobject.config.ValueColumn
import org.jdbi.v3.sqlobject.statement.SqlQuery
import sh.zachwal.button.db.jdbi.Press
import sh.zachwal.button.db.jdbi.RecentPressCount
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.util.stream.Stream

interface PressDAO {

    @SqlQuery(
        """
            insert into public.press (remote, contact_id) values (:remote, :contactId)
            returning *
        """
    )
    fun createPress(remote: String, contactId: Int?): Press

    @SqlQuery(
        """
            insert into public.press (remote, contact_id, time) values (:remote, :contactId, :time)
            returning *
        """
    )
    fun createPressAtTime(remote: String, contactId: Int?, time: Instant): Press

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

    @SqlQuery(
        """
            select * 
            from public.press 
            where contact_id = :contactId 
            order by time;
        """
    )
    fun allPressesForContact(contactId: Int): Stream<Press>
    @SqlQuery(
        """
        select min(time) from public.press where contact_id = :contactId
        """
    )
    fun firstPressTimestampForContact(contactId: Int): Timestamp?

    @SqlQuery(
        """
        select date(time at time zone 'UTC') as date, count(*) as press_count
        from public.press
        where contact_id = :contactId and time >= :startDate and time <= :endDate + interval '1 day' - interval '1 second'
        group by date
        order by date
        """
    )
    @KeyColumn("date")
    @ValueColumn("press_count")
    fun aggregatePressCountsByDate(
        contactId: Int,
        startDate: LocalDate,
        endDate: LocalDate
    ): Map<LocalDate, Int>
}
