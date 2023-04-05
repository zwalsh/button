package sh.zachwal.button.session

import io.ktor.application.ApplicationCall
import io.ktor.sessions.sessions
import sh.zachwal.button.session.principals.UserSessionPrincipal
import java.time.Instant
import java.time.temporal.ChronoUnit

const val USER_SESSION = "USER_SESSION"

class SessionService {

    fun createUserSession(call: ApplicationCall, username: String) {
        val oneHourAway = Instant.now().plus(1, ChronoUnit.HOURS).toEpochMilli()
        call.sessions.set(USER_SESSION, UserSessionPrincipal(username, oneHourAway))
    }
}
