package sh.zachwal.button.session

import io.ktor.application.ApplicationCall
import io.ktor.sessions.sessions
import sh.zachwal.button.session.principals.ContactSessionPrincipal
import sh.zachwal.button.session.principals.UserSessionPrincipal
import java.time.Instant
import java.time.temporal.ChronoUnit

const val USER_SESSION = "USER_SESSION"
const val CONTACT_SESSION = "CONTACT_SESSION"

class SessionService {

    fun createUserSession(call: ApplicationCall, username: String) {
        val oneWeekAway = Instant.now().plus(1, ChronoUnit.WEEKS).toEpochMilli()
        call.sessions.set(USER_SESSION, UserSessionPrincipal(username, oneWeekAway))
    }

    fun createContactSession(call: ApplicationCall, contactId: Int) {
        val thirtyDaysAway = Instant.now().plus(30, ChronoUnit.DAYS).toEpochMilli()
        call.sessions.set(CONTACT_SESSION, ContactSessionPrincipal(contactId, thirtyDaysAway))
    }
}
