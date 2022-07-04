package sh.zachwal.button.phone

import com.google.inject.Inject
import com.google.inject.Singleton
import sh.zachwal.button.db.dao.ContactDAO
import sh.zachwal.button.db.jdbi.Contact
import sh.zachwal.button.sms.InvalidNumber
import sh.zachwal.button.sms.MessagingService
import sh.zachwal.button.sms.ValidNumber

class InvalidNumberException(val invalidNumber: InvalidNumber) : Exception(invalidNumber.reason)

@Singleton
class PhoneBookService @Inject constructor(
    private val messagingService: MessagingService,
    private val contactDAO: ContactDAO,
) {
    suspend fun register(name: String, number: String): Contact {
        val validNumber = when (val validated = messagingService.validateNumber(number)) {
            is InvalidNumber -> throw InvalidNumberException(validated)
            is ValidNumber -> validated.validNumber
        }

        // TODO check if phone number already exists

        return contactDAO.createContact(name, validNumber)
    }
}