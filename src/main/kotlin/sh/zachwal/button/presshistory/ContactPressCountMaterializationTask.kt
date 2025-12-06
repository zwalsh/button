package sh.zachwal.button.presshistory

import com.google.inject.Inject
import org.slf4j.LoggerFactory
import sh.zachwal.button.db.dao.ContactDAO
import sh.zachwal.button.db.dao.ContactPressCountDAO
import sh.zachwal.button.db.dao.PressDAO
import sh.zachwal.button.db.jdbi.ContactPressCount
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.concurrent.timer

class ContactPressCountMaterializationTask @Inject constructor(
    private val contactDAO: ContactDAO,
    private val pressDAO: PressDAO,
    private val contactPressCountDAO: ContactPressCountDAO
) {
    private val logger = LoggerFactory.getLogger(ContactPressCountMaterializationTask::class.java)

    fun runMaterialization() {
        val today = LocalDate.now(ZoneOffset.UTC)
        val yesterday = today.minusDays(1)
        val contacts = contactDAO.selectActiveContacts()
        for (contact in contacts) {
            logger.info("Materializing press counts for $contact")
            val contactId = contact.id
            val firstPressTimestamp = pressDAO.firstPressTimestampForContact(contactId) ?: continue
            val firstPressDate = firstPressTimestamp.toInstant().atZone(ZoneOffset.UTC).toLocalDate()
            logger.info("Contact's first press date is $firstPressDate")
            val lastMaterialized = contactPressCountDAO.findAllForContact(contactId).maxByOrNull { it.date }?.date
            logger.info("Contact's last materialized date is $lastMaterialized")
            val startDate = lastMaterialized?.plusDays(1) ?: firstPressDate
            if (startDate > yesterday) continue
            val pressCounts = pressDAO.aggregatePressCountsByDate(contactId, startDate, yesterday)
            for ((date, count) in pressCounts) {
                logger.info("Contact pressed $count times on $date")
                contactPressCountDAO.upsert(ContactPressCount(contactId, date, count))
            }
        }
    }

    fun repeatDaily() {
        timer(name = "contact-press-materialization", daemon = true, period = Duration.ofDays(1).toMillis()) {
            try {
                runMaterialization()
            } catch (e: Exception) {
                logger.error("Error running ContactPressCountMaterializationTask", e)
            }
        }
    }
}
