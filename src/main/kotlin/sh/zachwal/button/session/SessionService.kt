package sh.zachwal.button.session

import io.ktor.server.application.ApplicationCall
import io.ktor.server.sessions.sessions
import sh.zachwal.button.session.principals.ContactSessionPrincipal
import sh.zachwal.button.session.principals.UserSessionPrincipal
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

const val USER_SESSION = "USER_SESSION"
const val CONTACT_SESSION = "CONTACT_SESSION"

val USER_SESSION_LENGTH: Duration = Duration.of(7, ChronoUnit.DAYS)
val CONTACT_SESSION_LENGTH: Duration = Duration.of(30, ChronoUnit.DAYS)

class SessionService {

    fun createUserSession(call: ApplicationCall, username: String) {
        val expiration = Instant.now().plus(USER_SESSION_LENGTH).toEpochMilli()
        call.sessions.set(USER_SESSION, UserSessionPrincipal(username, expiration))
    }

    fun createContactSession(call: ApplicationCall, contactId: Int) {
        val expiration = Instant.now().plus(CONTACT_SESSION_LENGTH).toEpochMilli()
        call.sessions.set(CONTACT_SESSION, ContactSessionPrincipal(contactId, expiration))
    }
}
