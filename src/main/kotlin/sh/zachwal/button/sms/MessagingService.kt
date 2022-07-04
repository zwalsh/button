package sh.zachwal.button.sms

interface MessagingService {
    suspend fun validateNumber(phoneNumber: String): PhoneNumberValidation

    suspend fun sendMessage(toPhoneNumber: String, body: String): MessageStatus
}
