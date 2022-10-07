package sh.zachwal.button.phone

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.google.inject.Inject
import com.google.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import sh.zachwal.button.config.MessagingConfig
import sh.zachwal.button.db.dao.ContactDAO
import sh.zachwal.button.db.jdbi.Contact
import sh.zachwal.button.sms.InvalidNumber
import sh.zachwal.button.sms.MessagingService
import sh.zachwal.button.sms.ValidNumber
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class InvalidNumberException(val invalidNumber: InvalidNumber) : Exception(invalidNumber.reason)

@Singleton
class PhoneBookService @Inject constructor(
    private val messagingService: MessagingService,
    private val contactDAO: ContactDAO,
    private val messagingConfig: MessagingConfig,
) {

    private val threadPool = Executors.newFixedThreadPool(
        1,
        ThreadFactoryBuilder()
            .setNameFormat("phone-book-service-thread-%d")
            .build()
    )

    // Use supervisor so individual coroutines can fail independently
    private val scope = CoroutineScope(threadPool.asCoroutineDispatcher() + SupervisorJob())

    init {
        Runtime.getRuntime().addShutdownHook(
            thread(start = false) {
                threadPool.shutdownNow()
            }
        )
    }

    suspend fun register(name: String, number: String): Contact {
        val validNumber = when (val validated = messagingService.validateNumber(number)) {
            is InvalidNumber -> throw InvalidNumberException(validated)
            is ValidNumber -> validated.validNumber
        }

        // TODO check if phone number already exists
        val contact = contactDAO.createContact(name, validNumber)

        scope.launch {
            messagingService.sendMessage(
                messagingConfig.adminPhone,
                "New contact just signed up: ${contact.name} at ${contact.phoneNumber}."
            )
        }

        return contact
    }

    fun contacts(): List<Contact> {
        return contactDAO.selectContacts()
    }

    fun updateContactStatus(contactId: Int, active: Boolean): UpdateContactResult {
        return contactDAO.updateContactStatus(contactId, active)?.let {
            UpdatedContact(it)
        } ?: ContactNotFound
    }
}
