package sh.zachwal.button.roles

import sh.zachwal.authserver.db.dao.UserDAO
import sh.zachwal.authserver.db.dao.UserRoleDAO
import sh.zachwal.authserver.db.jdbi.User
import javax.inject.Inject

class RoleService @Inject constructor(private val userRoleDAO: UserRoleDAO, private val userDAO: UserDAO) {

    private fun hasRole(user: User, role: Role): Boolean = userRoleDAO.rolesForUser(user).any { it.role == role }

    fun firstRoleOrNull(user: User, roles: Set<Role>): Role? = roles.firstOrNull { r ->
        hasRole(user, r)
    }

    fun grantRole(user: User, role: Role) = userRoleDAO.grantRoleForUser(role, user)

    fun allRoles(): Map<User, List<Role>> {
        val userRoles = userRoleDAO.allRoles()
        val users = userDAO.listUsers()
        val usersById = users.associateBy { it.id }
        return userRoles.groupBy({ ur ->
            usersById[ur.userId] ?: error("user not found for it ${ur.userId}")
        }) { it.role }
    }

    fun usersWithoutRole(role: Role): List<User> = userRoleDAO.usersWithoutRole(role)
}
