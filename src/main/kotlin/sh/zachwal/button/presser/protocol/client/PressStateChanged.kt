package sh.zachwal.button.presser.protocol.client

import java.time.Instant

data class PressStateChanged(val state: PressState) : ClientMessage {
    override val ts: Instant = Instant.now()
}
