package sh.zachwal.button.contact

import com.opencsv.CSVWriterBuilder
import org.jdbi.v3.core.Jdbi
import sh.zachwal.button.db.dao.PressDAO
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactDataService @Inject constructor(
    private val jdbi: Jdbi,
) {

    fun writeAllPressesToStream(contactId: Int, outputStream: OutputStream) = jdbi.open().use { handle ->
        val pressDAO = handle.attach(PressDAO::class.java)
        val presses = pressDAO.allPressesForContact(contactId)

        ZipOutputStream(outputStream).use { stream ->
            stream.putNextEntry(ZipEntry("button-data.csv"))

            val csvWriter = CSVWriterBuilder(OutputStreamWriter(stream))
                .withSeparator(',')
                .build()

            csvWriter.writeNext(arrayOf("Time"))

            presses.forEach { press ->
                csvWriter.writeNext(arrayOf(press.time.toString()))
            }
            csvWriter.flush()

            stream.closeEntry()
        }
    }
}
