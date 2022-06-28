package sh.zachwal.button.friends

import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.routing.Routing
import io.ktor.routing.get
import kotlinx.html.DIV
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.td
import kotlinx.html.title
import kotlinx.html.tr
import sh.zachwal.authserver.auth.currentUser
import sh.zachwal.authserver.controller.Controller
import sh.zachwal.authserver.db.jdbi.FriendRequest
import sh.zachwal.authserver.db.jdbi.User
import sh.zachwal.button.roles.approvedUserRoute
import sh.zachwal.button.shared_html.bootstrapCss
import sh.zachwal.button.shared_html.responsiveTable
import sh.zachwal.button.users.UserService
import javax.inject.Inject

@Controller
class FriendController @Inject constructor(
    private val requestsService: RequestsService,
    private val userService: UserService,
    private val friendService: FriendService,
) {
    internal fun Routing.friendsPage() {
        approvedUserRoute("/friends") {
            get {
                val currentUser = currentUser(call, userService)
                val friends = friendService.friendsForUser(currentUser)
                val friendables = friendService.friendable(currentUser)
                val pending = requestsService.pendingRequests(currentUser)
                val sent = requestsService.sentRequests(currentUser)
                call.respondHtml {
                    head {
                        title { +"Friends" }
                        bootstrapCss()
                        script(
                            src = "https://ajax.googleapis.com/ajax/libs/jquery/3.5.1/jquery.min" +
                                ".js"
                        ) {}
                        script(src = "/static/src/friends/friends.js") {}
                    }
                    body {
                        div(classes = "container") {
                            div(classes = "row justify-content-center") {
                                h1 { +"Friends" }
                            }
                            div(classes = "row") {
                                myFriendsCol(friends)
                                friendablesCol(friendables)
                                requestsCol(pending, sent)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun DIV.col4CardBody(cardHeader: String, cardBody: DIV.() -> Unit) {
        div(classes = "col-md-4 col-sm-6 mb-4") {
            card(cardHeader, cardBody = cardBody)
        }
    }

    private fun DIV.card(
        cardHeader: String,
        classes: String = "mt-4 h-100",
        cardBody: DIV.() ->
        Unit
    ) {
        div(classes = "card $classes") {
            div(classes = "card-header") {
                +cardHeader
            }
            div(classes = "card-body", block = cardBody)
        }
    }

    private fun DIV.myFriendsCol(friends: List<User>) {
        col4CardBody("My Friends") {
            if (friends.isEmpty()) {
                p {
                    +"None?!? Add some friends!"
                }
            }
            responsiveTable {
                friends.forEach {
                    tr {
                        td {
                            +it.username
                        }
                    }
                }
            }
        }
    }

    private fun DIV.friendablesCol(friendables: List<User>) {
        col4CardBody("Users to Add") {
            responsiveTable {
                friendables.forEach {
                    tr {
                        td {
                            +it.username
                        }
                        td(classes = "d-flex justify-content-end") {
                            button(classes = "btn btn-primary send-request") {
                                attributes["data-requested-user-id"] = it.id.toString()
                                +"Send Request"
                            }
                        }
                    }
                }
            }
        }
    }

    private fun DIV.requestsCol(pending: List<FriendRequest>, sent: List<FriendRequest>) {
        col4CardBody("Requests") {
            if (pending.isNotEmpty()) {
                card("Pending", classes = "") {
                    responsiveTable {
                        pending.forEach {
                            tr {
                                td {
                                    +it.requester.username
                                }
                                td(classes = "d-flex justify-content-end") {
                                    button(classes = "btn btn-primary accept-request") {
                                        attributes["data-requester-user-id"] = it.requester.id.toString()
                                        +"Accept Request"
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (sent.isNotEmpty()) {
                val needsTopMargin = pending.isNotEmpty()
                val sentRequestsCardClasses = if (needsTopMargin) "mt-3" else ""
                card("Sent", classes = sentRequestsCardClasses) {
                    responsiveTable {
                        sent.forEach {
                            tr {
                                td {
                                    +it.requested.username
                                }
                            }
                        }
                    }
                }
            }
            if (pending.isEmpty() && sent.isEmpty()) {
                p {
                    +"No requests pending or sent"
                }
            }
        }
    }
}
