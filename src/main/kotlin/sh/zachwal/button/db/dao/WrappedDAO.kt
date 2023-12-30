package sh.zachwal.button.db.dao

import org.jdbi.v3.sqlobject.statement.SqlQuery
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
}
