package sh.zachwal.button.phone

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import sh.zachwal.button.db.dao.ContactDAO
import sh.zachwal.button.db.jdbi.Contact
import sh.zachwal.button.sms.InvalidNumber
import sh.zachwal.button.sms.MessagingService
import sh.zachwal.button.sms.ValidNumber
import java.time.Instant

internal class PhoneBookServiceTest {

    private val messagingService: MessagingService = mockk()
    private val contactDAO: ContactDAO = mockk()
    private val phoneBookService = PhoneBookService(messagingService, contactDAO)

    @Test
    fun `throws when phone number is invalid`() {
        val reason = "oops invalid!"
        coEvery { messagingService.validateNumber(any()) } returns InvalidNumber(
            "123", reason
        )

        assertThrows<IllegalArgumentException> {
            runBlocking {
                phoneBookService.register("My Name", "123")
            }
        }
    }

    @Test
    fun `persists when phone number is valid`() {
        val validNumber = "+18001234567"
        coEvery { messagingService.validateNumber(any()) } returns ValidNumber(validNumber)
        every { contactDAO.createContact(any(), any()) } answers {
            Contact(1, Instant.now(), firstArg(), secondArg(), true)
        }

        val contact = runBlocking {
            phoneBookService.register("My Name", "123")
        }
        assertThat(contact.name).isEqualTo("My Name")
        assertThat(contact.phoneNumber).isEqualTo(validNumber)
    }
}
