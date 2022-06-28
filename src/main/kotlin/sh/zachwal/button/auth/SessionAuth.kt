package sh.zachwal.button.auth

import io.ktor.application.call
import io.ktor.auth.Authentication
import io.ktor.auth.session
import io.ktor.response.respondRedirect
import org.slf4j.LoggerFactory
import sh.zachwal.button.session.SessionPrincipal

private val logger = LoggerFactory.getLogger("SessionAuth")

fun Authentication.Configuration.configureSessionAuth() {
    session<SessionPrincipal> {
        challenge {
            call.respondRedirect("/login")
        }
        validate {
            it.takeIf(SessionPrincipal::isValid)
        }
    }
}
