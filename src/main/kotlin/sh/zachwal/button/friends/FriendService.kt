package sh.zachwal.button.friends

import sh.zachwal.authserver.db.dao.FriendDAO
import sh.zachwal.authserver.db.dao.FriendRequestDAO
import sh.zachwal.authserver.db.jdbi.Friendship
import sh.zachwal.authserver.db.jdbi.User
import javax.inject.Inject

class FriendService @Inject constructor(private val friendDAO: FriendDAO, private val requestDAO: FriendRequestDAO) {
    fun friendsForUser(user: User): List<User> = friendDAO.friendsOfUser(user)

    fun friendable(user: User): List<User> {
        val nonFriendsWithUserRole = friendDAO.nonFriendsWithUserRole(user)
        val requestsSent = requestDAO.sentRequests(user).map { it.requested }
        val requestsReceived = requestDAO.pendingRequests(user).map { it.requester }
        val notFriendable = requestsSent.toSet().plus(requestsReceived)
        return nonFriendsWithUserRole.filter { nonFriend ->
            notFriendable.contains(nonFriend).not()
        }
    }

    fun checkFriends(user1: User, user2: User): Boolean = friendDAO.friendshipExists(
        Friendship.create(user1, user2)
    )
}
