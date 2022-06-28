package sh.zachwal.button.friends

import io.ktor.application.call
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.post
import sh.zachwal.authserver.auth.currentUser
import sh.zachwal.authserver.controller.Controller
import sh.zachwal.authserver.db.jdbi.FriendRequest
import sh.zachwal.button.friends.api.AcceptFriendRequest
import sh.zachwal.button.friends.api.SendFriendRequest
import sh.zachwal.button.roles.approvedUserRoute
import sh.zachwal.button.users.UserService
import javax.inject.Inject

@Controller
class FriendRequestController @Inject constructor(
    private val requestsService: RequestsService,
    private val userService: UserService,
) {

    internal fun Routing.sendRequestRoute() {
        approvedUserRoute("/api/friendrequest") {
            post {
                val currentUser = currentUser(call, userService)
                val sendRequest = call.receive<SendFriendRequest>()
                val requested = userService.getUser(sendRequest.toUserId)
                if (requested == null) {
                    call.respond(NotFound)
                    return@post
                }
                requestsService.sendRequest(currentUser, requested)
                call.respond(200)
            }
        }
    }

    internal fun Routing.acceptRequestRoute() {
        approvedUserRoute("/api/acceptrequest") {
            post {
                val currentUser = currentUser(call, userService)
                val acceptRequest = call.receive<AcceptFriendRequest>()
                val requester = userService.getUser(acceptRequest.fromUserId)
                if (requester == null) {
                    call.respond(NotFound)
                    return@post
                }
                requestsService.acceptRequest(currentUser, FriendRequest(requester, currentUser))
                call.respond(200)
            }
        }
    }
}
