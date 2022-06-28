package sh.zachwal.button.session

import io.ktor.application.ApplicationCall
import io.ktor.sessions.sessions
import java.time.Instant
import java.time.temporal.ChronoUnit

class SessionService {

    fun createSession(call: ApplicationCall, name: String) {
        val oneHourAway = Instant.now().plus(1, ChronoUnit.HOURS).toEpochMilli()
        call.sessions.set("AUTH_SESSION", SessionPrincipal(name, oneHourAway))
    }
}
