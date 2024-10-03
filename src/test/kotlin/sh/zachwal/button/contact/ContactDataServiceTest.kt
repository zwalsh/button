package sh.zachwal.button.contact

import com.opencsv.CSVReaderBuilder
import io.mockk.every
import io.mockk.mockk
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import sh.zachwal.button.db.dao.PressDAO
import sh.zachwal.button.db.jdbi.Press
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.time.Instant
import java.util.stream.Stream

class ContactDataServiceTest {

    private val pressDAO: PressDAO = mockk()

    private val handle: Handle = mockk(relaxed = true) {
        every { attach(PressDAO::class.java) } returns pressDAO
    }

    private val jdbi: Jdbi = mockk {
        every { open() } returns handle
    }

    private val contactDataService = ContactDataService(jdbi = jdbi)


    @Test
    fun `writes returned press data to an output stream`() {
        val press = Press(
            time = Instant.parse("2021-01-01T00:00:00Z"),
            remote = "1.1.1.1",
            contactId = 1,
        )

        every { pressDAO.allPressesForContact(1) } returns Stream.of(
            press,
            press.copy(time = Instant.parse("2021-01-02T00:00:00Z")),
            press.copy(time = Instant.parse("2021-01-03T00:00:00Z")),
        )

        val outputStream = ByteArrayOutputStream()

        contactDataService.writeAllPressesToStream(1, outputStream)

        val reader = InputStreamReader(ByteArrayInputStream(outputStream.toByteArray()))

        val csvReader = CSVReaderBuilder(reader)
            .build()

        val header = csvReader.readNext()
        assertEquals(1, header.size)
        assertEquals("Time", header[0])

        val rest = csvReader.readAll()
        assertEquals(3, rest.size)
        assertEquals("2021-01-01T00:00:00Z", rest[0][0])
        assertEquals("2021-01-02T00:00:00Z", rest[1][0])
        assertEquals("2021-01-03T00:00:00Z", rest[2][0])
    }
}