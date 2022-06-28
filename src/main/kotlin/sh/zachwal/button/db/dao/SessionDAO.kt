package sh.zachwal.authserver.db.dao

import org.jdbi.v3.sqlobject.customizer.BindBean
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.jdbi.v3.sqlobject.transaction.Transaction
import sh.zachwal.authserver.db.jdbi.Session
import java.time.Instant

interface SessionDAO {
    @SqlQuery("select * from public.session where id = ?")
    fun getById(id: String): Session?

    @Transaction
    @SqlUpdate(
        """
        insert into public.session (id, data, expiration) values (:id, :data, :expiration)
        on conflict (id) do update set data = excluded.data, expiration = excluded.expiration
    """
    )
    fun createOrUpdateSession(@BindBean session: Session)

    @SqlUpdate("delete from public.session where id = ?")
    fun deleteSession(id: String)

    @SqlQuery("delete from public.session where expiration < ? returning *")
    fun deleteExpiredBefore(time: Instant): List<Session>
}
