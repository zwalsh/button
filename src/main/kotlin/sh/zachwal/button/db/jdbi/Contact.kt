package sh.zachwal.button.db.jdbi

import java.time.Instant

data class Contact(
    val id: Int,
    val createdDate: Instant,
    val name: String,
    val phoneNumber: String,
    val active: Boolean,
)
