package sh.zachwal.button.admin.contact

data class UpdateContactRequest(
    val contactId: Long,
    val active: Boolean
)
