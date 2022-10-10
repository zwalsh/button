package sh.zachwal.button.admin.contact

data class UpdateContactRequest(
    val contactId: Int,
    val active: Boolean
)
