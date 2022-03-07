package sh.zachwal.button.presser

import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.CloseReason.Codes
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
    private var observer: PresserObserver? = null,
    dispatcher: CoroutineDispatcher
) {
    // uses two coroutines, one to accept incoming & one to send outgoing
    private val scope = CoroutineScope(dispatcher)

    // updates to the current count of pressers
    private val countUpdateChannel = Channel<Int>(UNLIMITED)

    suspend fun watch() {
        val incoming = scope.launch {
            // handle incoming
            for (frame in socketSession.incoming) {
                when (frame) {
                    is Text -> {
                        logger.info("Received text! ${frame.readText()}")
                        observer?.pressed(this@Presser)
                    }
                    else -> {
                        logger.info("Got unexpected frame $frame, disconnecting...")
                        observer?.disconnected(this@Presser)
                        socketSession.close(CloseReason(Codes.PROTOCOL_ERROR, "Invalid frame type"))
                    }
                }
            }
        }
        val outgoing = scope.launch {
            // handle outgoing
            for (updatedCount in countUpdateChannel) {
                logger.info("Got updated count: $updatedCount")
                socketSession.send(Text(updatedCount.toString()))
            }
        }
        incoming.join()
        outgoing.join()
    }

    fun setObserver(observer: PresserObserver) {
        this.observer = observer
    }

    suspend fun updatePressingCount(count: Int) {
        countUpdateChannel.send(count)
    }
}
