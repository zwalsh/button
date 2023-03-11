package sh.zachwal.button.admin

import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import kotlinx.html.FlowContent
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.head
import kotlinx.html.li
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.title
import kotlinx.html.tr
import kotlinx.html.ul
import org.slf4j.LoggerFactory
import sh.zachwal.button.controller.Controller
import sh.zachwal.button.db.jdbi.User
import sh.zachwal.button.roles.Role
import sh.zachwal.button.roles.Role.ADMIN
import sh.zachwal.button.roles.Role.USER
import sh.zachwal.button.roles.RoleService
import sh.zachwal.button.roles.adminRoute
import sh.zachwal.button.shared_html.headSetup
import sh.zachwal.button.users.UserService
import javax.inject.Inject

@Controller
class AdminController @Inject constructor(
    private val userService: UserService,
    private val roleService: RoleService,
) {

    private val logger = LoggerFactory.getLogger("AdminRoutes")

    private fun sortedUsers(users: List<User>, roles: Map<User, List<Role>>): List<User> {
        return users.sortedWith(
            Comparator { u1, u2 ->
                val u1Roles = roles[u1]
                val u2Roles = roles[u2]

                val u1Admin = u1Roles?.contains(ADMIN) ?: false
                val u2Admin = u2Roles?.contains(ADMIN) ?: false

                if (u1Admin && !u2Admin) {
                    return@Comparator -1
                }
                if (u2Admin && !u1Admin) {
                    return@Comparator 1
                }

                val u1User = u1Roles?.contains(USER) ?: false
                val u2User = u2Roles?.contains(USER) ?: false

                if (u1User && !u2User) {
                    return@Comparator -1
                }
                if (u2User && !u1User) {
                    return@Comparator 1
                }

                u1.username.lowercase().compareTo(u2.username.lowercase())
            }
        )
    }

    internal fun Routing.admin() {
        adminRoute("/admin") {
            get {
                call.respondHtml {
                    head {
                        title {
                            +"Admin"
                        }
                        headSetup()
                    }
                    body {
                        div(classes = "container") {
                            h1 {
                                +"Admin"
                            }
                            h2 {
                                +"Users"
                            }
                            ul {
                                li {
                                    a(href = "/admin/users") {
                                        +"Users"
                                    }
                                }
                                li {
                                    a(href = "/admin/pending") {
                                        +"Pending"
                                    }
                                }
                            }
                            h2 {
                                +"Contacts"
                            }
                            ul {
                                li {
                                    a(href = "/admin/contacts") {
                                        +"Manage Contacts"
                                    }
                                }
                            }
                            h2 {
                                +"Stats"
                            }
                            ul {
                                li {
                                    a(href = "/admin/press-stats") {
                                        +"Press Stats"
                                    }
                                }
                            }
                            h2 {
                                +"Config"
                            }
                            ul {
                                li {
                                    a(href = "/admin/config") {
                                        +"Button Config"
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    internal fun Routing.listUsers() {
        adminRoute("/admin/users") {
            get {
                val roles = roleService.allRoles()
                val users = sortedUsers(userService.list(), roles)

                call.respondHtml {
                    head {
                        title { +"Users" }
                        headSetup()
                    }
                    body {
                        div(classes = "container") {
                            h1 { +"Users" }
                            table(classes = "table") {
                                thead {
                                    tr {
                                        th {
                                            +"Name"
                                        }
                                        th {
                                            +"Approved"
                                        }
                                        th {
                                            +"Admin"
                                        }
                                    }
                                }
                                tbody {
                                    users.forEach {
                                        tr {
                                            td {
                                                +it.username
                                            }
                                            td {
                                                if (roles[it]?.contains(USER) == true) {
                                                    +"✅"
                                                } else {
                                                    +"❌"
                                                }
                                            }
                                            td {
                                                if (roles[it]?.contains(ADMIN) == true) {
                                                    +"✅"
                                                } else {
                                                    +"❌"
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    internal fun Routing.pendingUsers() {
        adminRoute("/admin/pending") {
            get {
                val pendingUsers =
                    roleService.usersWithoutRole(USER).sortedBy { it.username.lowercase() }
                call.respondHtml {
                    head {
                        title {
                            +"Pending Users"
                        }

                        headSetup()

                        script(
                            src = "https://ajax.googleapis.com/ajax/libs/jquery/3.5.1/jquery.min" +
                                ".js"
                        ) {}
                        script(src = "/static/src/admin/pending.js") {}
                    }
                    body {
                        div(classes = "container") {
                            h1 {
                                +"Pending Users"
                            }
                            pendingUserTable(pendingUsers)
                        }
                    }
                }
            }
        }
    }

    private fun FlowContent.pendingUserTable(pendingUsers: List<User>) {
        if (pendingUsers.isNotEmpty()) {
            table(classes = "table") {
                thead {
                    tr {
                        th {
                            +"User"
                        }
                        th {
                            +"Approve"
                        }
                    }
                }
                tbody {
                    pendingUsers.forEach {
                        tr {
                            td {
                                +it.username
                            }
                            td {
                                button(classes = "user-approve btn btn-primary") {
                                    attributes["data-user-id"] = it.id.toString()
                                    +"Approve"
                                }
                            }
                        }
                    }
                }
            }
        } else {
            p {
                +"No pending users"
            }
        }
    }

    internal fun Routing.approveUser() {
        adminRoute("/admin/pending/approve") {
            post {
                val request = call.receive<ApproveUserRequest>()
                val user = userService.getUser(request.userId)
                if (user != null) {
                    roleService.grantRole(user, USER)
                    call.respond(OK, ApproveUserResponse("Approved user ${user.id}"))
                } else {
                    call.respond(NotFound)
                }
            }
        }
    }
}
