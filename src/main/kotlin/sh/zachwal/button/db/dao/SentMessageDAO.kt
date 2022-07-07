package sh.zachwal.button.db.dao

import org.jdbi.v3.sqlobject.customizer.BindBean
import org.jdbi.v3.sqlobject.statement.SqlQuery
import sh.zachwal.button.db.jdbi.SentMessage
import java.time.Instant

interface SentMessageDAO {

    @SqlQuery(
        """
            insert into public.sent_message (twilio_id, sent_date, contact_id) 
            values (:twilioId, :sentDate, :contactId)
            returning *;
        """
    )
    fun recordSentMessage(@BindBean sentMessage: SentMessage): SentMessage

    @SqlQuery(
        """
            select count(*)
            from public.sent_message
            where sent_date > ?;
        """
    )
    fun countSentSince(since: Instant): Int
}
