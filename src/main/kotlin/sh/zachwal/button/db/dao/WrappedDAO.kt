package sh.zachwal.button.db.dao

import org.jdbi.v3.sqlobject.customizer.BindBean
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import sh.zachwal.button.db.jdbi.Press
import sh.zachwal.button.db.jdbi.WrappedLink
import sh.zachwal.button.db.jdbi.WrappedRank
import java.time.Instant

interface WrappedDAO {

    /**
     * Returns a ranked list of contacts
     */
    @SqlQuery(
        """
            select 
              c.id as contactId,
              count(distinct date_trunc('day', p.time)) as uniqueDays,
              rank() over (order by count(distinct date_trunc('day', p.time)) desc) as uniqueDaysRank,
              percent_rank() over (order by count(distinct date_trunc('day', p.time)) desc) as uniqueDaysPercentile,
              rank() over (order by count(*) desc) as rank,
              percent_rank() over (order by count(*) desc) as percentile
            from 
              contact c 
              left join press p on p.contact_id = c.id
            where 
              (p.time > ? AND p.time <= ?) 
              OR p is null 
            group by 
              c.id
            order by 
              rank;
        """
    )
    fun wrappedRanks(fromInstant: Instant, toInstant: Instant): List<WrappedRank>

    @SqlQuery(
        "select * from public.press where time > ? and time < ? and contact_id = ? order by time"
    )
    fun selectBetweenForContact(begin: Instant, end: Instant, contactId: Int): List<Press>

    @SqlQuery(
        """
            select
              distinct(c.id)
            from 
              contact c 
              join press p on p.contact_id = c.id
            where 
              p.time >= ? and p.time < ?;
        """
    )
    fun contactsWithPresses(fromInstant: Instant, toInstant: Instant): List<Int>

    @SqlUpdate(
        """
            insert into public.wrapped_link
            (id, contact_id, year)
            values (:wrappedId, :contactId, :year)
        """
    )
    fun insertWrappedLink(@BindBean wrappedLink: WrappedLink)

    @SqlQuery(
        """
            select 
              id as wrappedId,
              year,
              contact_id as contactId
            from
              wrapped_link;
        """
    )
    fun wrappedLinks(): List<WrappedLink>
}
