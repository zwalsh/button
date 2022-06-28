package sh.zachwal.authserver.db.jdbi

data class FriendRequest(
    val requester: User,
    val requested: User
)
