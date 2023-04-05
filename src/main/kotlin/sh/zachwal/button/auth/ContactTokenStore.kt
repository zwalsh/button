package sh.zachwal.button.auth

import org.slf4j.LoggerFactory
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// Custom class that stores a token -> contact mapping
@Singleton
class ContactTokenStore @Inject constructor() {

    private val logger = LoggerFactory.getLogger(ContactTokenStore::class.java)

    private val tokens = mutableMapOf<String, Int>()

    fun createToken(contactId: Int): String {
        val newToken = UUID.randomUUID().toString()

        tokens[newToken] = contactId

        logger.info("Stored $newToken token for contact $contactId")
        return newToken
    }

    fun checkToken(token: String): Int? {
        return tokens[token]
    }
}
