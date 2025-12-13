package sh.zachwal.button.presser.protocol.server

data class CurrentCount(
    val count: Int
) : ServerMessage
