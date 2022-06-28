package sh.zachwal.authserver.db.jdbi

import java.time.Instant

/**
 * Session object stored in the database.
 *
 * Sessions in Ktor have an ID and some data, stored as a blob. We additionally add
 * the approximate expiration time when writing to the database, here represented as
 * an Instant. The source of truth for expiration time is in the data blob (from SessionPrincipal),
 * but we add it to the DB to be able to remove those rows in the background.
 *
 * This expiration value will be updated every time the session is touched, so we will
 * delete it at least one hour after it is last used, when it's guaranteed to be expired.
 */
data class Session(
    val id: String,
    val data: ByteArray,
    val expiration: Instant
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Session

        if (id != other.id) return false
        if (!data.contentEquals(other.data)) return false
        if (expiration != other.expiration) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + expiration.hashCode()
        return result
    }
}
