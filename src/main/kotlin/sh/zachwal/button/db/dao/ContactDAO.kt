package sh.zachwal.button.db.dao

import org.jdbi.v3.sqlobject.customizer.Bind
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
            select * 
            from public.contact
            where id = ?
        """
    )
    fun findContact(contactId: Int): Contact?

    @SqlQuery(
        """
            select * from public.contact where active order by created_date;
        """
    )
    fun selectActiveContacts(): List<Contact>

    @SqlQuery(
        """
            select * from public.contact order by name;
        """
    )
    fun selectContacts(): List<Contact>

    @SqlQuery(
        """
            update public.contact set active = :active where id = :contactId returning *;
        """
    )
    fun updateContactStatus(
        @Bind("contactId")
        contactId: Int,
        @Bind("active")
        active: Boolean
    ): Contact?

    // TODO deactivate number
}
