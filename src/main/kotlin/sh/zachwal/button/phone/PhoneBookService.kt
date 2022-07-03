package sh.zachwal.button.phone

import com.google.inject.Inject
import com.google.inject.Singleton
import sh.zachwal.button.sms.InvalidNumber
import sh.zachwal.button.sms.MessagingService
import sh.zachwal.button.sms.ValidNumber

@Singleton
class PhoneBookService @Inject constructor(
    private val messagingService: MessagingService
) {

    suspend fun register(name: String, number: String) {
        val validation = messagingService.validateNumber(number)
        if (validation is InvalidNumber) {
            // propagate error
            return
        } else if (validation is ValidNumber) {
            validation.validNumber
        }
    }
}
