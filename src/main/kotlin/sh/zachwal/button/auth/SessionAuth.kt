package sh.zachwal.button.auth

import io.ktor.application.call
import io.ktor.auth.Authentication
import io.ktor.auth.session
import io.ktor.response.respondRedirect
import sh.zachwal.button.session.principals.ContactSessionPrincipal
import sh.zachwal.button.session.principals.UserSessionPrincipal

fun Authentication.Configuration.configureUserSessionAuth() {
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

fun Authentication.Configuration.configureContactSessionAuth() {
    session<ContactSessionPrincipal>(CONTACT_SESSION_AUTH) {
        challenge {
            call.respondRedirect("/") // Redirect to the button home page for now, but in the future to an MFA page?
        }
        validate {
            it.takeIf(ContactSessionPrincipal::isValid)
        }
    }
}
