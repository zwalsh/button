package sh.zachwal.button.auth

import io.ktor.application.ApplicationCall
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import sh.zachwal.button.db.jdbi.User
import sh.zachwal.button.session.SessionPrincipal
import sh.zachwal.button.users.UserService

suspend fun currentUser(call: ApplicationCall, userService: UserService): User {
    val p = call.sessions.get<SessionPrincipal>()
    return p?.let { userService.getUser(p.user) } ?: run {
        call.respond(HttpStatusCode.Unauthorized)
        throw UnauthorizedException()
    }
}

class UnauthorizedException : Exception()
