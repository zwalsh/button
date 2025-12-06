package sh.zachwal.button.db.dao

import org.jdbi.v3.sqlobject.config.KeyColumn
import org.jdbi.v3.sqlobject.config.ValueColumn
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.customizer.BindBean
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import java.time.LocalDate
import sh.zachwal.button.db.jdbi.ContactPressCount

interface ContactPressCountDAO {
    @SqlUpdate("""
        insert into contact_press_counts (contact_id, date, press_count)
        values (:contactId, :date, :pressCount)
        on conflict (contact_id, date) do update set press_count = excluded.press_count
    """)
    fun upsert(@BindBean contactPressCount: ContactPressCount)

    @SqlQuery("""
        select contact_id, date, press_count
        from contact_press_counts
        where contact_id = :contactId and date = :date
    """)
    fun find(contactId: Int, date: LocalDate): ContactPressCount?

    @SqlQuery("""
        select contact_id, date, press_count
        from contact_press_counts
        where contact_id = :contactId
        order by date desc
    """)
    fun findAllForContact(contactId: Int): List<ContactPressCount>

    @KeyColumn("contact_id") @ValueColumn("press_count")
    @SqlQuery("""
        select contact_id, sum(press_count) as press_count
        from contact_press_counts
        where date >= :startDate and date <= :endDate
        group by contact_id
    """)
    fun aggregateCountsByContact(startDate: java.time.LocalDate, endDate: java.time.LocalDate): Map<Int, Int>

}
