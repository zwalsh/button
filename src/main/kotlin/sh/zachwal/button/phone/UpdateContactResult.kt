package sh.zachwal.button.phone

import sh.zachwal.button.db.jdbi.Contact

sealed interface UpdateContactResult

data class UpdatedContact(
    val contact: Contact
) : UpdateContactResult

object ContactNotFound : UpdateContactResult
