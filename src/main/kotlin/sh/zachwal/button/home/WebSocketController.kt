package sh.zachwal.button.home

import com.google.inject.name.Named
import io.ktor.features.origin
import io.ktor.routing.Routing
import io.ktor.websocket.webSocket
import kotlinx.coroutines.CoroutineDispatcher
import org.slf4j.LoggerFactory
import sh.zachwal.authserver.controller.Controller
import sh.zachwal.button.presser.Presser
import sh.zachwal.button.presser.PresserManager
import javax.inject.Inject

@Controller
class WebSocketController @Inject constructor(
    private val manager: PresserManager,
    @Named("presserDispatcher")
    private val presserDispatcher: CoroutineDispatcher,
) {

    private val logger = LoggerFactory.getLogger(WebSocketController::class.java)

    internal fun Routing.webSocketRoute() {
        webSocket("/socket") {
            val clientHost = call.request.origin.remoteHost
            val clientPort = call.request.origin.port
            val remote = "$clientHost:$clientPort"
            logger.info("New connection from $remote")
            val presser = Presser(this, manager, remote, presserDispatcher)
            manager.addPresser(presser)
            presser.watchChannels()
            logger.info("$clientHost disconnected")
        }
    }
}
