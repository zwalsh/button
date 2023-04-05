package sh.zachwal.button.home

import io.ktor.features.origin
import io.ktor.routing.Routing
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import io.ktor.websocket.webSocket
import org.slf4j.LoggerFactory
import sh.zachwal.button.controller.Controller
import sh.zachwal.button.db.dao.ContactDAO
import sh.zachwal.button.presser.PresserFactory
import sh.zachwal.button.presser.PresserManager
import sh.zachwal.button.session.principals.ContactSessionPrincipal
import javax.inject.Inject

@Controller
class WebSocketController @Inject constructor(
    private val manager: PresserManager,
    private val presserFactory: PresserFactory,
    private val contactDAO: ContactDAO,
) {

    private val logger = LoggerFactory.getLogger(WebSocketController::class.java)

    internal fun Routing.webSocketRoute() {
        webSocket("/socket") {
            val contactSession = call.sessions.get<ContactSessionPrincipal>()
            val contact = contactSession?.contactId?.let { contactDAO.findContact(it) }
            if (contact != null) {
                logger.info("New connection from contact $contact")
            }

            val clientHost = call.request.origin.remoteHost
            val clientPort = call.request.origin.port
            val remote = "$clientHost:$clientPort"
            logger.info("New connection from $remote")
            val presser = presserFactory.createPresser(this, remote, contact)
            manager.addPresser(presser)
            presser.watchChannels()
            logger.info("$clientHost disconnected")
        }
    }
}
