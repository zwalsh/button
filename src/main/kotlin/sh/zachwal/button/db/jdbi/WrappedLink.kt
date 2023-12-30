package sh.zachwal.button.db.jdbi

/**
 * Stores the ID of a given Wrapped for some contact for some year.
 *
 * Contacts are sent a URL including this wrapped ID and use that to find
 * their Wrapped.
 *
 * Wrapped IDs are random.
 */
data class WrappedLink constructor(
    val wrappedId: String,
    val year: Int,
    val contactId: Int
)
