package sh.zachwal.button.config

// TODO consider storing this in the db so it can be edited on the fly?
data class MessagingConfig constructor(
    val monthlyLimit: Int,
    val adminPhone: String,
)
