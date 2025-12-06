package sh.zachwal.button.presshistory

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.jdbi.v3.core.Jdbi
import sh.zachwal.button.db.dao.ContactDAO
import sh.zachwal.button.db.dao.ContactPressCountDAO
import sh.zachwal.button.db.dao.PressDAO
import java.time.LocalDate
import java.time.ZoneOffset

import sh.zachwal.button.db.extension.DatabaseExtension
import java.time.Instant
import java.time.temporal.ChronoUnit

@ExtendWith(DatabaseExtension::class)
class ContactPressCountMaterializationTaskTest(private val jdbi: Jdbi) {
    private lateinit var pressDAO: PressDAO
    private lateinit var contactDAO: ContactDAO
    private lateinit var contactPressCountDAO: ContactPressCountDAO
    private lateinit var task: ContactPressCountMaterializationTask

    @BeforeEach
    fun setup() {
        pressDAO = jdbi.onDemand(PressDAO::class.java)
        contactDAO = jdbi.onDemand(ContactDAO::class.java)
        contactPressCountDAO = jdbi.onDemand(ContactPressCountDAO::class.java)
        task = ContactPressCountMaterializationTask(contactDAO, pressDAO, contactPressCountDAO)
    }

    @Test
    fun `materializes all missing days from first press to yesterday`() {
        val contact = contactDAO.createContact("Charlie", "+15550000003")
        val today = LocalDate.now(ZoneOffset.UTC)
        val twoDaysAgo = today.minusDays(2)
        val yesterday = today.minusDays(1)
        // Insert presses for two days ago and yesterday
        pressDAO.createPressAtTime("remote", contact.id, Instant.now().minus(2, ChronoUnit.DAYS))
        pressDAO.createPressAtTime("remote", contact.id, Instant.now().minus(1, ChronoUnit.DAYS))
        task.runMaterialization()
        val counts = contactPressCountDAO.findAllForContact(contact.id)
        assertEquals(2, counts.size)
        assertEquals(yesterday, counts[0].date)
        assertEquals(1, counts[0].pressCount)
        assertEquals(twoDaysAgo, counts[1].date)
        assertEquals(1, counts[1].pressCount)
    }

    @Test
    fun `does not touch today`() {
        val contact = contactDAO.createContact("Dana", "+15550000004")
        val today = LocalDate.now(ZoneOffset.UTC)
        pressDAO.createPress("remote", contact.id)
        task.runMaterialization()
        val counts = contactPressCountDAO.findAllForContact(contact.id)
        assertEquals(0, counts.size)
    }
}
