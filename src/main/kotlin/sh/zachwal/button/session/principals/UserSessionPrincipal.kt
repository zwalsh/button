package sh.zachwal.button.session.principals

import io.ktor.server.auth.Principal

data class UserSessionPrincipal constructor(
    val user: String,
    override val expiration: Long
) : Principal, SessionPrincipal
