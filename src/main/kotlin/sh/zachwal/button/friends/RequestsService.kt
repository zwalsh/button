package sh.zachwal.button.friends

import org.jdbi.v3.core.statement.UnableToExecuteStatementException
import org.slf4j.LoggerFactory
import sh.zachwal.authserver.db.dao.FriendRequestDAO
import sh.zachwal.authserver.db.jdbi.FriendRequest
import sh.zachwal.authserver.db.jdbi.Friendship
import sh.zachwal.authserver.db.jdbi.User
import javax.inject.Inject

class RequestsService @Inject constructor(private val friendRequestDAO: FriendRequestDAO) {

    private val logger = LoggerFactory.getLogger(RequestsService::class.java)

    fun sendRequest(requester: User, requested: User) {
        try {
            friendRequestDAO.createRequest(
                FriendRequest(
                    requester,
                    requested
                )
            )
        } catch (e: UnableToExecuteStatementException) {
            logger.error(
                "Could not send request from ${requester.username} to ${requested.username}",
                e
            )
        }
    }

    fun sentRequests(requester: User): List<FriendRequest> = friendRequestDAO.sentRequests(requester)

    fun pendingRequests(requested: User): List<FriendRequest> = friendRequestDAO.pendingRequests(requested)

    fun acceptRequest(user: User, request: FriendRequest): Friendship {
        if (request.requested == user) {
            return friendRequestDAO.acceptFriendRequest(request)
        } else {
            throw IllegalStateException(
                "A User (${user.username}) cannot accept a request on " +
                    "behalf of another User (${request.requested.username})"
            )
        }
    }
}
