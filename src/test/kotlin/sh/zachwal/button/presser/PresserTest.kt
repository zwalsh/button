package sh.zachwal.button.presser

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.truth.Truth.assertThat
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import sh.zachwal.button.presser.protocol.server.DailyStats
import sh.zachwal.button.presser.protocol.server.ServerMessage
import sh.zachwal.button.presser.protocol.server.Snapshot

class PresserTest {

    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    @Test
    fun `oldest snapshot is dropped when drop-oldest channel overflows`() = runBlocking {
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
        val emptyStats = DailyStats(0, 0, 0)
        presser.sendSnapshot(Snapshot(count = 1, names = listOf("s1"), dailyStats = emptyStats))
        firstSendStarted.await() // coroutine is now stuck in send; channel is empty

        // Fill the channel to capacity (2) then overflow it while the coroutine is blocked
        presser.sendSnapshot(Snapshot(count = 2, names = listOf("s2"), dailyStats = emptyStats)) // fills slot 1
        presser.sendSnapshot(Snapshot(count = 3, names = listOf("s3"), dailyStats = emptyStats)) // fills slot 2
        presser.sendSnapshot(Snapshot(count = 4, names = listOf("s4"), dailyStats = emptyStats)) // overflows: drops s2, keeps s3+s4

        // Unblock all WebSocket sends
        allowSend.complete(Unit)

        // First message sent: snapshot 1 (was already picked up before the block)
        val firstSent = mapper.readValue<ServerMessage>(withTimeout(1000) { sentTexts.receive() })
        assertThat(firstSent).isInstanceOf(Snapshot::class.java)
        assertThat((firstSent as Snapshot).names).containsExactly("s1")

        // Second message sent: s3, because s2 (oldest queued) was dropped
        val secondSent = mapper.readValue<ServerMessage>(withTimeout(1000) { sentTexts.receive() })
        assertThat(secondSent).isInstanceOf(Snapshot::class.java)
        assertThat((secondSent as Snapshot).names).containsExactly("s3")

        // Third message sent: s4
        val thirdSent = mapper.readValue<ServerMessage>(withTimeout(1000) { sentTexts.receive() })
        assertThat(thirdSent).isInstanceOf(Snapshot::class.java)
        assertThat((thirdSent as Snapshot).names).containsExactly("s4")

        job.cancel()
    }
}
