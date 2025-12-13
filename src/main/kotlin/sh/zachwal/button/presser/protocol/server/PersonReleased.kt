package sh.zachwal.button.presser.protocol.server

data class PersonReleased(
    val displayName: String
) : ServerMessage
