package sh.zachwal.button.auth.contact

import org.slf4j.LoggerFactory
import sh.zachwal.button.db.dao.ContactTokenDAO
import sh.zachwal.button.random.RandomStringGenerator
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

// Custom class that stores a token -> contact mapping
@Singleton
class ContactTokenStore @Inject constructor(
    private val contactTokenDAO: ContactTokenDAO
) {

    private val logger = LoggerFactory.getLogger(ContactTokenStore::class.java)

    private val randomStringGenerator = RandomStringGenerator()

    fun createToken(contactId: Int): String {
        val token = randomStringGenerator.newToken(20)
        val expiration = Instant.now().plus(7, ChronoUnit.DAYS)
        val contactToken = contactTokenDAO.createToken(token, contactId, expiration)

        logger.info("Stored contact token $contactToken.")
        return token
    }

    fun checkToken(token: String): Int? {
        return contactTokenDAO.findToken(token)?.contactId
    }
}
