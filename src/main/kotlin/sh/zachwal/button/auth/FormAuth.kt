package sh.zachwal.authserver.auth

import io.ktor.application.call
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.UserPasswordCredential
import io.ktor.auth.form
import io.ktor.response.respondRedirect
import sh.zachwal.button.users.UserService

// taken from https://gitlab.com/nanodeath/ktor-session-auth-example
fun Authentication.Configuration.configureFormAuth(userService: UserService) {
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
