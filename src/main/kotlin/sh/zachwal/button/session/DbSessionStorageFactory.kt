package sh.zachwal.button.session

import com.google.inject.Inject
import sh.zachwal.button.db.dao.SessionDAO

/**
 * Factory for creating [DbSessionStorage] instances with different session prefixes.
 *
 * This factory allows the application to create multiple session storage instances,
 * each with its own unique prefix to prevent session ID collisions in the database.
 * Typically used to create separate storage for USER_SESSION and CONTACT_SESSION.
 *
 * @param sessionDAO The DAO for database operations on sessions, injected by Guice
 */
class DbSessionStorageFactory @Inject constructor(
    private val sessionDAO: SessionDAO
) {

    /**
     * Creates a new [DbSessionStorage] instance with the specified prefix.
     *
     * @param prefix A unique prefix for this session type (e.g., "USER_SESSION" or "CONTACT_SESSION")
     * @return A new DbSessionStorage instance configured with the given prefix
     */
    fun buildStorage(prefix: String) = DbSessionStorage(sessionDAO, prefix)
}
