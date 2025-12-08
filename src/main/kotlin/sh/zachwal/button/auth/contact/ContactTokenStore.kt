package sh.zachwal.button.auth.contact

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

    private val randomStringGenerator = RandomStringGenerator()

    fun createToken(contactId: Int): String {
        val token = randomStringGenerator.newToken(20)
        val expiration = Instant.now().plus(30, ChronoUnit.DAYS)
        contactTokenDAO.createToken(token, contactId, expiration)
        return token
    }

    fun checkToken(token: String): Int? {
        return contactTokenDAO.findToken(token)?.contactId
    }
}
