package sh.zachwal.button.auth.contact

import org.slf4j.LoggerFactory
import sh.zachwal.button.db.dao.ContactTokenDAO
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.streams.toList

// Custom class that stores a token -> contact mapping
@Singleton
class ContactTokenStore @Inject constructor(
    private val contactTokenDAO: ContactTokenDAO
) {

    private val logger = LoggerFactory.getLogger(ContactTokenStore::class.java)

    private val secureRandom = SecureRandom()
    private val chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"

    private fun newToken(): String {
        return secureRandom.ints(20, 0, chars.length).toList()
            .map { randInt ->
                chars[randInt]
            }.fold(StringBuilder()) { sb, char ->
                sb.append(char)
            }.toString()
    }

    fun createToken(contactId: Int): String {
        val token = newToken()
        val expiration = Instant.now().plus(7, ChronoUnit.DAYS)
        val contactToken = contactTokenDAO.createToken(token, contactId, expiration)

        logger.info("Stored contact token $contactToken.")
        return token
    }

    fun checkToken(token: String): Int? {
        return contactTokenDAO.findToken(token)?.contactId
    }
}
