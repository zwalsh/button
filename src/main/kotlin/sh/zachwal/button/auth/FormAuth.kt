package sh.zachwal.button.auth

import io.ktor.server.application.call
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.UserPasswordCredential
import io.ktor.server.auth.form
import io.ktor.server.response.respondRedirect
import sh.zachwal.button.users.UserService

// taken from https://gitlab.com/nanodeath/ktor-session-auth-example
fun AuthenticationConfig.configureFormAuth(userService: UserService) {
    form("form") {
        userParamName = "username"
        passwordParamName = "password"
        challenge {
            call.respondRedirect("login?failed=true")
        }
        validate { cred: UserPasswordCredential ->
            userService.checkUser(cred.name, cred.password)?.let {
                UserIdPrincipal(it.username)
            }
        }
    }
}
