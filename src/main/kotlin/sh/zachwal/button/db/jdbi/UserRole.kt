package sh.zachwal.button.db.jdbi

import sh.zachwal.button.roles.Role

data class UserRole(
    val userId: Long,
    val role: Role
)
