package sh.zachwal.button.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import sh.zachwal.button.db.jdbi.User
import sh.zachwal.button.session.principals.UserSessionPrincipal
import sh.zachwal.button.users.UserService

suspend fun currentUser(call: ApplicationCall, userService: UserService): User {
    val p = call.sessions.get<UserSessionPrincipal>()
    return p?.let { userService.getUser(p.user) } ?: run {
        call.respond(HttpStatusCode.Unauthorized)
        throw UnauthorizedException()
    }
}

class UnauthorizedException : Exception()
