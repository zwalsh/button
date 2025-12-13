package sh.zachwal.button.presser.protocol.client

data class PressStateChanged(val state: PressState) : ClientMessage
