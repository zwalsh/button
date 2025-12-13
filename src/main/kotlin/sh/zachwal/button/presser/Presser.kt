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
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import sh.zachwal.button.db.jdbi.Contact
import sh.zachwal.button.ktorutils.remote
import sh.zachwal.button.presser.protocol.client.ClientMessage
import sh.zachwal.button.presser.protocol.client.PressState
import sh.zachwal.button.presser.protocol.client.PressStateChanged
import sh.zachwal.button.presser.protocol.server.CurrentCount
import sh.zachwal.button.presser.protocol.server.PersonPressing
import sh.zachwal.button.presser.protocol.server.PersonReleased
import sh.zachwal.button.presser.protocol.server.ServerMessage

/**
 * Handles a single WebSocket client connection, managing incoming and outgoing messages for a button presser.
 *
 * Receives state changes from the client, notifies observers (such as PresserManager), and sends updates (like
 * CurrentCount and PersonPressing) to the client. Each Presser is associated with a contact (if authenticated) and
 * participates in the global pressers data flow via its observer.
 */
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
    private val countUpdateChannel = Channel<Int>(10, onBufferOverflow = BufferOverflow.DROP_LATEST)
    // person pressing notifications
    private val personPressingChannel = Channel<String>(10, onBufferOverflow = BufferOverflow.DROP_LATEST)
    // person released notifications
    private val personReleasedChannel = Channel<String>(10, onBufferOverflow = BufferOverflow.DROP_LATEST)

    suspend fun watchChannels() {
        val incoming = scope.launch {
            // handle incoming
            for (frame in socketSession.incoming) {
                handleIncomingFrame(frame)
            }
        }
        val outgoingCount = scope.launch {
            for (updatedCount in countUpdateChannel) {
                sendServerMessage(CurrentCount(count = updatedCount))
            }
        }
        val outgoingPerson = scope.launch {
            for (name in personPressingChannel) {
                sendServerMessage(PersonPressing(displayName = name))
            }
        }
        val outgoingReleased = scope.launch {
            for (name in personReleasedChannel) {
                sendServerMessage(PersonReleased(displayName = name))
            }
        }
        incoming.join()
        outgoingCount.join()
        outgoingPerson.join()
        outgoingReleased.join()
        observer.disconnected(this)
    }

    private suspend fun sendServerMessage(message: ServerMessage) {
        val text = objectMapper.writeValueAsString(message)
        socketSession.send(Text(text))
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

    private suspend fun handleIncomingText(frame: Text) {
        val text = frame.readText()
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

    suspend fun notifyPersonPressing(name: String) {
        personPressingChannel.send(name)
    }

    suspend fun notifyPersonReleased(name: String) {
        personReleasedChannel.send(name)
    }

    fun remote(): String = socketSession.call.request.remote()
}
