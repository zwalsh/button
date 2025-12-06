package sh.zachwal.button.presser.protocol.server

import java.time.Instant

data class PersonPressing(
    val displayName: String,
    override val ts: Instant
) : ServerMessage
