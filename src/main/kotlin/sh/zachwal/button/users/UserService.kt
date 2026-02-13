package sh.zachwal.button.users

import org.jdbi.v3.core.statement.UnableToExecuteStatementException
import org.mindrot.jbcrypt.BCrypt
import org.slf4j.LoggerFactory
import sh.zachwal.button.db.dao.UserDAO
import sh.zachwal.button.db.jdbi.User
import javax.inject.Inject

class UserService @Inject constructor(private val userDAO: UserDAO) {

    private val logger = LoggerFactory.getLogger(UserService::class.java)

    fun getUser(username: String): User? {
        return try {
            userDAO.getByUsername(username)
        } catch (e: Exception) {
            logger.info("Error retrieving user $username.", e)
            null
        }
    }

    fun getUser(id: Long): User? {
        return try {
            userDAO.getById(id)
        } catch (e: Exception) {
            logger.info("User with id $id could not be found", e)
            null
        }
    }

    fun checkUser(username: String, password: String): User? = getUser(username)?.takeIf {
        try {
            BCrypt.checkpw(password, it.hashedPassword)
        } catch (e: Exception) {
            logger.warn("Failed to check password for user $username", e)
            false
        }
    }

    fun createUser(username: String, password: String): User? {
        val hash = BCrypt.hashpw(password, BCrypt.gensalt())
        return try {
            userDAO.createUser(username, hash)
        } catch (e: UnableToExecuteStatementException) {
            logger.error("Failed to create user $username", e)
            null
        }
    }

    fun list(): List<User> = userDAO.listUsers()
}
