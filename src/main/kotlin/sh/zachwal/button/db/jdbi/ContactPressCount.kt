package sh.zachwal.button.db.jdbi

import java.time.LocalDate

data class ContactPressCount(
    val contactId: Int,
    val date: LocalDate,
    val pressCount: Int
)
