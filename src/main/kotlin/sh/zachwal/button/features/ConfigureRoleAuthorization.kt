package sh.zachwal.button.features

import io.ktor.server.application.Application
import io.ktor.server.application.log
import sh.zachwal.button.roles.RoleAuthorization.RoleBasedAuthorizer
import sh.zachwal.button.roles.RoleService
import sh.zachwal.button.users.UserService

fun configureRoleAuthorization(roleBasedAuthorizer: RoleBasedAuthorizer, application: Application, userService: UserService, roleService: RoleService) {
    roleBasedAuthorizer.validate { roles, session ->
        application.log.info("Checking $roles for session ${session.user}")
        val user = userService.getUser(session.user)
        user?.let { roleService.firstRoleOrNull(user, roles) }
    }
}
