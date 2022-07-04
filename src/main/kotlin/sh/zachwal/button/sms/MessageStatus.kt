package sh.zachwal.button.sms

sealed class MessageStatus

data class MessageFailed(
    val reason: String
) : MessageStatus()

object MessageQueued : MessageStatus()
