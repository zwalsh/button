package sh.zachwal.button.users

import io.ktor.application.call
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.authenticate
import io.ktor.auth.principal
import io.ktor.html.respondHtml
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.sessions.clear
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import io.ktor.util.getOrFail
import kotlinx.html.FormMethod.post
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.br
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.label
import kotlinx.html.passwordInput
import kotlinx.html.submitInput
import kotlinx.html.textInput
import kotlinx.html.title
import sh.zachwal.button.controller.Controller
import sh.zachwal.button.session.SessionPrincipal
import sh.zachwal.button.session.SessionService
import sh.zachwal.button.shared_html.headSetup
import javax.inject.Inject

@Controller
class UserController @Inject constructor(
    private val sessionService: SessionService,
    private val userService: UserService,
) {
    internal fun Routing.loginRoutes() {
        route("/login") {
            get {
                val session = call.sessions.get<SessionPrincipal>()
                if (session?.isValid() == true) {
                    return@get call.respondRedirect("/profile")
                } else if (session?.isValid() == false) {
                    call.sessions.clear<SessionPrincipal>()
                }

                val failed = call.request.queryParameters["failed"]?.equals("true") ?: false

                call.respondHtml {
                    head {
                        title {
                            +"Login"
                        }
                        headSetup()
                    }
                    body {
                        div(classes = "container") {
                            div(classes = "row justify-content-center") {
                                div(classes = "card mt-4") {
                                    div(classes = "card-body") {
                                        form(method = post, classes = "mb-1") {
                                            div(classes = "form-group") {
                                                h1 {
                                                    +"Login"
                                                }
                                            }
                                            div(classes = "form-group") {
                                                label { +"Username" }
                                                textInput(
                                                    name = "username",
                                                    classes = "form-control"
                                                ) {
                                                    placeholder = "user"
                                                }
                                            }
                                            div(classes = "form-group") {
                                                label { +"Password" }
                                                passwordInput(
                                                    name = "password",
                                                    classes = "form-control"
                                                ) {
                                                    placeholder = "password"
                                                }
                                            }
                                            if (failed) {
                                                div(classes = "alert alert-danger") {
                                                    +"Login attempt failed"
                                                }
                                            }
                                            submitInput(classes = "btn btn-primary") {
                                                value = "Log in"
                                            }
                                        }
                                        a(href = "/register") {
                                            +"Register"
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            authenticate("form") {
                post {
                    val p = call.principal<UserIdPrincipal>()
                        ?: return@post call.respond(
                            HttpStatusCode.InternalServerError,
                            "No User principal found after post"
                        )
                    sessionService.createSession(call, p.name)
                    call.respondRedirect("/profile")
                }
            }
        }
    }

    private fun greeting(): String = listOf(
        "Hello",
        "Sup",
        "Hi",
        "Howdy",
        "Salutations",
        "What's good"
    ).random()

    internal fun Routing.profileRoute() {
        route("/profile") {
            authenticate {
                get {
                    val p = call.sessions.get<SessionPrincipal>()
                    call.respondHtml {
                        head {
                            title {
                                +"${p?.user}'s Profile"
                            }
                            headSetup()
                        }
                        body {
                            div(classes = "container") {
                                h1 {
                                    +"${greeting()}, ${p?.user}!"
                                }
                                a(href = "/logout") {
                                    +"Log out"
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    internal fun Routing.logoutRoute() {
        route("/logout") {
            authenticate {
                get {
                    call.sessions.clear<SessionPrincipal>()
                    call.respondRedirect("/login")
                }
            }
        }
    }

    internal fun Routing.registerRoutes() {
        route("/register") {
            get {
                call.respondHtml {
                    head {
                        title {
                            +"Register"
                        }
                        headSetup()
                    }
                    body {
                        form(method = post) {
                            h1 {
                                +"Register"
                            }
                            textInput(name = "username") {
                                placeholder = "user"
                            }
                            br
                            passwordInput(name = "password") {
                                placeholder = "password"
                            }
                            br
                            submitInput {
                                value = "Create New User"
                            }
                        }
                    }
                }
            }
            post {
                val params = call.receiveParameters()
                val user = userService.createUser(
                    params.getOrFail("username"),
                    params.getOrFail("password")
                )

                if (user != null) {
                    sessionService.createSession(call, user.username)
                    call.respondRedirect("/profile")
                } else {
                    call.respond(HttpStatusCode.Conflict, "User already exists")
                }
            }
        }
    }
}
