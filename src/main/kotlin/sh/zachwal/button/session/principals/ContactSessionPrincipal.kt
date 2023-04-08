package sh.zachwal.button.session.principals

import io.ktor.auth.Principal

data class ContactSessionPrincipal constructor(
    val contactId: Int,
    override val expiration: Long
) : Principal, SessionPrincipal
