package sh.zachwal.button.sms

sealed class PhoneNumberValidation

data class InvalidNumber(
    val originalNumber: String,
    val reason: String,
) : PhoneNumberValidation()

data class ValidNumber(
    val validNumber: String
) : PhoneNumberValidation()
