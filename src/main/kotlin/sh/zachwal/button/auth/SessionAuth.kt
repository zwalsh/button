package sh.zachwal.button.auth

import io.ktor.server.application.call
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.session
import io.ktor.server.response.respondRedirect
import sh.zachwal.button.session.principals.ContactSessionPrincipal
import sh.zachwal.button.session.principals.UserSessionPrincipal

fun AuthenticationConfig.configureUserSessionAuth() {
    session<UserSessionPrincipal> {
        challenge {
            call.respondRedirect("/login")
        }
        validate {
            it.takeIf(UserSessionPrincipal::isValid)
        }
    }
}

const val CONTACT_SESSION_AUTH = "contact-session"

fun AuthenticationConfig.configureContactSessionAuth() {
    session<ContactSessionPrincipal>(CONTACT_SESSION_AUTH) {
        challenge {
            call.respondRedirect("/") // Redirect to the button home page for now, but in the future to an MFA page?
        }
        validate {
            it.takeIf(ContactSessionPrincipal::isValid)
        }
    }
}
