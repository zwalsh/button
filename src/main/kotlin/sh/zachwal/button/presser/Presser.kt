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

private val logger = LoggerFactory.getLogger(Presser::class.java)

class Presser(
    private val socketSession: WebSocketServerSession,
    private var observer: PresserObserver,
    private val remoteHost: String,
    dispatcher: CoroutineDispatcher
) {
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
                socketSession.send(Text(updatedCount.toString()))
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
        logger.info("Presser at $remoteHost set to $text")
        when (text) {
            "pressing" -> observer.pressed(this)
            "released" -> observer.released(this)
            else -> {
                logger.error("Got unexpected text from client on socket: $text, disconnecting.")
                observer.disconnected(this@Presser)
                socketSession.close(CloseReason(PROTOCOL_ERROR, "Invalid text $text"))
            }
        }
    }

    suspend fun updatePressingCount(count: Int) {
        countUpdateChannel.send(count)
    }
}
