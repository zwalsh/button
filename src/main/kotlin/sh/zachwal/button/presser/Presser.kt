package sh.zachwal.button.presser

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.CloseReason.Codes
import io.ktor.http.cio.websocket.CloseReason.Codes.PROTOCOL_ERROR
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.Frame.Text
import io.ktor.http.cio.websocket.close
import io.ktor.http.cio.websocket.readText
import io.ktor.websocket.WebSocketServerSession
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import sh.zachwal.button.db.jdbi.Contact
import sh.zachwal.button.ktorutils.remote
import sh.zachwal.button.presser.protocol.client.ClientMessage
import sh.zachwal.button.presser.protocol.client.PressState
import sh.zachwal.button.presser.protocol.client.PressStateChanged
import sh.zachwal.button.presser.protocol.server.CurrentCount

class Presser constructor(
    private val socketSession: WebSocketServerSession,
    private val observer: PresserObserver,
    val remoteHost: String,
    val contact: Contact?,
    private val objectMapper: ObjectMapper,
    dispatcher: CoroutineDispatcher
) {
    private val logger = LoggerFactory.getLogger(Presser::class.java)

    // uses two coroutines, one to accept incoming & one to send outgoing
    private val scope = CoroutineScope(dispatcher)

    // updates to the current count of pressers
    private val countUpdateChannel = Channel<Int>(UNLIMITED)

    suspend fun watchChannels() {
        val incoming = scope.launch {
            // handle incoming
            for (frame in socketSession.incoming) {
                handleIncomingFrame(frame)
            }
        }
        val outgoing = scope.launch {
            // handle outgoing
            for (updatedCount in countUpdateChannel) {
                val message = CurrentCount(count = updatedCount)
                val text = objectMapper.writeValueAsString(message)
                socketSession.send(Text(text))
            }
        }
        incoming.join()
        outgoing.join()
        observer.disconnected(this)
    }

    private suspend fun handleIncomingFrame(frame: Frame) {
        when (frame) {
            is Text -> handleIncomingText(frame)
            else -> {
                logger.info("Got unexpected frame: $frame, disconnecting.")
                observer.disconnected(this@Presser)
                socketSession.close(CloseReason(Codes.PROTOCOL_ERROR, "Invalid frame type"))
            }
        }
    }

    private suspend fun Presser.handleIncomingText(frame: Text) {
        val text = frame.readText()
        logger.info("Presser at $remoteHost sent $text")
        val message = try {
            objectMapper.readValue<ClientMessage>(text)
        } catch (e: JsonParseException) {
            logger.error("Client sent unparseable message, disconnecting.", e)
            observer.disconnected(this)
            socketSession.close(CloseReason(PROTOCOL_ERROR, "Invalid text $text"))
            scope.cancel("Client sent unparseable message, disconnecting.", e)
            return
        }

        when (message) {
            is PressStateChanged -> handlePressStateChanged(message)
        }
    }

    private suspend fun handlePressStateChanged(pressStateChanged: PressStateChanged) {
        when (pressStateChanged.state) {
            PressState.PRESSING -> observer.pressed(this)
            PressState.RELEASED -> observer.released(this)
        }
    }

    suspend fun updatePressingCount(count: Int) {
        countUpdateChannel.send(count)
    }

    fun remote(): String = socketSession.call.request.remote()
}
