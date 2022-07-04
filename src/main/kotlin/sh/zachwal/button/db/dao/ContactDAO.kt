package sh.zachwal.button.db.dao

import org.jdbi.v3.sqlobject.statement.SqlQuery
import sh.zachwal.button.db.jdbi.Contact

interface ContactDAO {

    @SqlQuery(
        """
            insert into public.contact (name, phone_number) values (?, ?)
            returning *;
        """
    )
    fun createContact(name: String, phoneNumber: String): Contact

    @SqlQuery(
        """
            select * from public.contact where active order by created_date;
        """
    )
    fun selectActiveContacts(): List<Contact>

    // TODO deactivate number
}
