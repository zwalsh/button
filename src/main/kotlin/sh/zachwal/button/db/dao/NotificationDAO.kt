package sh.zachwal.button.db.dao

import org.jdbi.v3.sqlobject.statement.SqlQuery
import sh.zachwal.button.db.jdbi.Notification

interface NotificationDAO {

    @SqlQuery(
        """
            insert into public.notification default values returning *;
        """
    )
    fun createNotification(): Notification

    @SqlQuery(
        """
            select * from public.notification 
            order by sent_date desc 
            limit 1;
        """
    )
    fun getLatestNotification(): Notification?
}
