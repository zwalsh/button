package sh.zachwal.button.sms

interface MessagingService {
    suspend fun validateNumber(phoneNumber: String): String

}
