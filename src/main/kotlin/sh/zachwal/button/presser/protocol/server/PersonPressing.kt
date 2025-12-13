package sh.zachwal.button.presser.protocol.server

data class PersonPressing(
    val displayName: String
) : ServerMessage
