package sh.zachwal.button.presser

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import sh.zachwal.button.presser.protocol.server.ServerMessage
import sh.zachwal.button.presser.protocol.server.Snapshot

class PresserTest {

    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    @Test
    fun `newer snapshot overwrites older when snapshot channel is full`() = runBlocking {
        val session = mockk<WebSocketServerSession>(relaxed = true)
        every { session.incoming } returns Channel<Frame>() // never produces frames

        // Gate that blocks WebSocket sends until we release it
        val firstSendStarted = CompletableDeferred<Unit>()
        val allowSend = CompletableDeferred<Unit>()
        val sentTexts = Channel<String>(Channel.UNLIMITED)

        coEvery { session.send(any<Frame>()) } coAnswers {
            firstSendStarted.complete(Unit)
            allowSend.await()
            sentTexts.send((firstArg<Frame>() as Frame.Text).readText())
        }

        val presser = Presser(session, mockk(relaxed = true), "host", null, mapper, Dispatchers.IO)
        val job = launch(Dispatchers.IO) { presser.watchChannels() }

        // Send snapshot 1 — coroutine picks it up immediately and blocks on the WebSocket send
        presser.sendSnapshot(Snapshot(count = 1, names = listOf("old")))
        firstSendStarted.await() // coroutine is now stuck in send; snapshotChannel is empty

        // Fill and overflow the channel while the coroutine is blocked
        presser.sendSnapshot(Snapshot(count = 2, names = listOf("middle"))) // fills channel (capacity 1)
        presser.sendSnapshot(Snapshot(count = 3, names = listOf("new")))    // should drop "middle", keep "new"

        // Unblock all WebSocket sends
        allowSend.complete(Unit)

        // First message sent: snapshot 1 (was already picked up before the block)
        val firstSent = mapper.readValue<ServerMessage>(withTimeout(1000) { sentTexts.receive() })
        assertThat(firstSent).isInstanceOf(Snapshot::class.java)
        assertThat((firstSent as Snapshot).names).containsExactly("old")

        // Second message sent: should be snapshot 3 ("new"), not snapshot 2 ("middle")
        val secondSent = mapper.readValue<ServerMessage>(withTimeout(1000) { sentTexts.receive() })
        assertThat(secondSent).isInstanceOf(Snapshot::class.java)
        assertThat((secondSent as Snapshot).names).containsExactly("new")

        job.cancel()
    }
}
