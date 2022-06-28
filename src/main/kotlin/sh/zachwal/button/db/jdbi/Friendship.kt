package sh.zachwal.authserver.db.jdbi

// This is just a data class but with a private constructor. All other boilerplate is standard.
class Friendship private constructor(
    val user1: User,
    val user2: User
) {
    companion object {
        // For ease of data modeling, all friendships have the user with the lower id first
        // This is so that in the database, it is easy to see if two users are friends
        // and avoid duplication. See create_friendship.json or public.friendship
        fun create(user1: User, user2: User): Friendship = if (user1.id < user2.id) {
            Friendship(user1, user2)
        } else {
            Friendship(user2, user1)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Friendship

        if (user1 != other.user1) return false
        if (user2 != other.user2) return false

        return true
    }

    override fun hashCode(): Int {
        var result = user1.hashCode()
        result = 31 * result + user2.hashCode()
        return result
    }

    override fun toString(): String {
        return "Friendship(user1=$user1, user2=$user2)"
    }
}
