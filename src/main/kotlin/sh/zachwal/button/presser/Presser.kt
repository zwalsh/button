package sh.zachwal.button.presser

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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import sh.zachwal.button.presser.Presser.PresserState.PRESSING
import sh.zachwal.button.presser.Presser.PresserState.RELEASED

private val logger = LoggerFactory.getLogger(Presser::class.java)

class Presser(
    private val socketSession: WebSocketServerSession,
    private var observer: PresserObserver,
    dispatcher: CoroutineDispatcher
) {
    // uses two coroutines, one to accept incoming & one to send outgoing
    private val scope = CoroutineScope(dispatcher)

    // updates to the current count of pressers
    private val countUpdateChannel = Channel<Int>(UNLIMITED)

    private enum class PresserState {
        PRESSING, RELEASED
    }

    private var presserState: PresserState = RELEASED

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
                socketSession.send(Text(updatedCount.toString()))
            }
        }
        incoming.join()
        outgoing.join()
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
        val newState = when (text) {
            "pressing" -> PRESSING
            "released" -> RELEASED
            else -> null
        }
        newState?.let {
            setPresserState(it)
        } ?: run {
            logger.error("Got unexpected text from client on socket: $text, disconnecting.")
            observer.disconnected(this@Presser)
            socketSession.close(CloseReason(PROTOCOL_ERROR, "Invalid text $text"))
        }
    }

    private suspend fun setPresserState(presserState: PresserState) {
        assert(this.presserState != presserState) {
            "Setting presser state to current state $presserState is not allowed"
        }
        when (presserState) {
            PRESSING -> observer.pressed(this)
            RELEASED -> observer.released(this)
        }
        this.presserState = presserState
    }

    suspend fun updatePressingCount(count: Int) {
        countUpdateChannel.send(count)
    }
}
