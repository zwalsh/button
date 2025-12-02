package sh.zachwal.button.presser

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Test
import sh.zachwal.button.presser.protocol.client.ClientMessage
import sh.zachwal.button.presser.protocol.client.PressState
import sh.zachwal.button.presser.protocol.client.PressStateChanged
import sh.zachwal.button.presser.protocol.server.CurrentCount
import sh.zachwal.button.presser.protocol.server.ServerMessage
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class ProtocolSerDeTest {

    private val mapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `serialize and deserialize client PressStateChanged`() {
        val msg: ClientMessage = PressStateChanged(PressState.PRESSING)
        val json = mapper.writeValueAsString(msg)

        val parsed = mapper.readValue<ClientMessage>(json)
        assertTrue(parsed is PressStateChanged)
        assertEquals(PressState.PRESSING, parsed.state)
    }

    @Test
    fun `serialize and deserialize server CurrentCount`() {
        val msg: ServerMessage = CurrentCount(123)
        val json = mapper.writeValueAsString(msg)

        val parsed = mapper.readValue<ServerMessage>(json)
        assertTrue(parsed is CurrentCount)
        assertEquals(123, parsed.count)
    }
}
