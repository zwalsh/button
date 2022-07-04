package sh.zachwal.button.sms

import java.time.Instant

sealed class MessageStatus

data class MessageFailed(
    val reason: String
) : MessageStatus()

data class MessageQueued(
    val id: String,
    val sentDate: Instant,
) : MessageStatus()
