package sh.zachwal.authserver.db.jdbi

import sh.zachwal.button.roles.Role

data class UserRole(
    val userId: Long,
    val role: Role
)
