package sh.zachwal.button.db.dao

import org.jdbi.v3.sqlobject.statement.SqlQuery
import sh.zachwal.button.db.jdbi.ContactToken
import java.time.Instant

interface ContactTokenDAO {

    @SqlQuery(
        """
            select * 
            from public.contact_token
            where token = ?
        """
    )
    fun findToken(token: String): ContactToken?

    @SqlQuery(
        """
            insert into public.contact_token
            (token, contact_id, expiration)
            values (?, ?, ?)
            returning *;
        """
    )
    fun createToken(token: String, contactId: Int, expiration: Instant): ContactToken?

    @SqlQuery(
        """
            delete from public.contact_token
            where expiration < ?
            returning *;
        """
    )
    fun deleteExpiredBefore(expiration: Instant): List<ContactToken>
}
