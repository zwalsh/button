package sh.zachwal.button.db.jdbi

import java.time.Instant

data class SentMessage(
    // SID field from Twilio API
    val twilioId: String,
    val sentDate: Instant,
    val contactId: Int,
)
