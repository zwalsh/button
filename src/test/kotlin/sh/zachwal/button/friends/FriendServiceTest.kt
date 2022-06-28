package sh.zachwal.button.friends

import com.google.common.truth.Truth.assertThat
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import sh.zachwal.authserver.db.dao.FriendDAO
import sh.zachwal.authserver.db.dao.FriendRequestDAO
import sh.zachwal.authserver.db.jdbi.FriendRequest
import sh.zachwal.authserver.db.jdbi.Friendship
import sh.zachwal.authserver.db.jdbi.User

class FriendServiceTest {

    private val friendDAO = mockk<FriendDAO>()
    private val requestDAO = mockk<FriendRequestDAO>()
    private val friendService = FriendService(friendDAO, requestDAO)

    private val alice = User(1, "alice", "hash")
    private val bob = User(2, "bob", "hash")
    private val carol = User(3, "carol", "hash")
    private val david = User(4, "david", "hash")
    private val erica = User(5, "erica", "hash")

    @Before
    fun setup() {
        clearAllMocks()
    }

    @Test
    fun `calls friend dao for all friends of user`() {
        every { friendDAO.friendsOfUser(alice) } returns listOf(bob, carol)
        every { friendDAO.friendsOfUser(carol) } returns listOf(alice)

        val aliceFriends = friendService.friendsForUser(alice)
        assertThat(aliceFriends).containsExactly(bob, carol)

        val carolFriends = friendService.friendsForUser(carol)
        assertThat(carolFriends).containsExactly(alice)
    }

    @Test
    fun `checks for existence of correct friendship even when order flipped`() {
        every { friendDAO.friendshipExists(any()) } returns true
        friendService.checkFriends(alice, bob)
        friendService.checkFriends(bob, alice)

        verify(exactly = 2) { friendDAO.friendshipExists(eq(Friendship.create(alice, bob))) }
    }

    @Test
    fun `gets correct friendable friends`() {
        every { friendDAO.nonFriendsWithUserRole(any()) } returns listOf(alice)
        every { requestDAO.pendingRequests(any()) } returns emptyList()
        every { requestDAO.sentRequests(any()) } returns emptyList()

        val friendableBob = friendService.friendable(bob)

        assertThat(friendableBob).containsExactly(alice)
    }

    @Test
    fun `friendable excludes outstanding requests`() {
        every { friendDAO.nonFriendsWithUserRole(any()) } returns listOf(bob, carol, david, erica)
        every { requestDAO.sentRequests(any()) } returns listOf(FriendRequest(alice, bob))
        every { requestDAO.pendingRequests(any()) } returns listOf(
            FriendRequest(carol, alice),
            FriendRequest(david, alice)
        )

        val friendablesAlice = friendService.friendable(alice)
        assertThat(friendablesAlice).containsExactly(erica)
    }
}
