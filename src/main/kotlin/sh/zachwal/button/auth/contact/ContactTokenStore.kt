package sh.zachwal.button.auth.contact

import org.slf4j.LoggerFactory
import sh.zachwal.button.db.dao.ContactTokenDAO
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// Custom class that stores a token -> contact mapping
@Singleton
class ContactTokenStore @Inject constructor(
    private val contactTokenDAO: ContactTokenDAO
) {

    private val logger = LoggerFactory.getLogger(ContactTokenStore::class.java)

    fun createToken(contactId: Int): String {
        val newToken = UUID.randomUUID().toString()
        val expiration = Instant.now().plus(7, ChronoUnit.DAYS)
        val contactToken = contactTokenDAO.createToken(newToken, contactId, expiration)

        logger.info("Stored contact token $contactToken.")
        return newToken
    }

    fun checkToken(token: String): Int? {
        return contactTokenDAO.findToken(token)?.contactId
    }
}
