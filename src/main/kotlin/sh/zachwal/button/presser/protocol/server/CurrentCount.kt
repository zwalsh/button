package sh.zachwal.button.presser.protocol.server

import java.time.Instant

data class CurrentCount(
    val count: Int
) : ServerMessage {
    override val ts: Instant = Instant.now()
}