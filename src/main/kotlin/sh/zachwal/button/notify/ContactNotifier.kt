package sh.zachwal.button.notify

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.google.inject.Inject
import com.google.inject.Singleton
import com.google.inject.name.Named
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import sh.zachwal.button.auth.contact.ContactTokenStore
import sh.zachwal.button.db.dao.ContactDAO
import sh.zachwal.button.db.dao.ContactPressCountDAO
import sh.zachwal.button.db.dao.NotificationDAO
import sh.zachwal.button.db.jdbi.Contact
import sh.zachwal.button.db.jdbi.NotificationPreferences
import sh.zachwal.button.home.TOKEN_PARAMETER
import sh.zachwal.button.presser.Presser
import sh.zachwal.button.presser.PresserObserver
import sh.zachwal.button.sms.ControlledContactMessagingService
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors
import kotlin.concurrent.thread

@Singleton
class ContactNotifier @Inject constructor(
    private val contactDAO: ContactDAO,
    private val contactPressCountDAO: ContactPressCountDAO,
    private val controlledContactMessagingService: ControlledContactMessagingService,
    private val notificationDAO: NotificationDAO,
    @Named("host")
    private val host: String,
    private val contactTokenStore: ContactTokenStore,
) : PresserObserver {

    private val logger = LoggerFactory.getLogger(ContactNotifier::class.java)

    private val threadPool = Executors.newFixedThreadPool(
        1,
        ThreadFactoryBuilder()
            .setNameFormat("contact-notifier-thread-%d")
            .build()
    )
    private val scope = CoroutineScope(threadPool.asCoroutineDispatcher() + SupervisorJob())
    private val link = "https://$host"

    init {
        Runtime.getRuntime().addShutdownHook(
            thread(start = false) {
                threadPool.shutdownNow()
            }
        )
    }

    override suspend fun pressed(presser: Presser) {
        scope.launch {
            val lastNotification = notificationDAO.getLatestNotification()

            val shouldSendNewNotification = lastNotification?.let { n ->
                val oneDayAgo = Instant.now().minus(1, ChronoUnit.DAYS)
                n.sentDate.isBefore(oneDayAgo)
            } ?: true
            if (!shouldSendNewNotification) {
                return@launch
            }
            logger.info(
                "Last notification was at {}, sending a new one triggered by contact={} at remote={}",
                lastNotification?.sentDate,
                presser.contact,
                presser.remote()
            )
            // TODO: Create the notification with the triggering contact id & remote address
            notificationDAO.createNotification()

            val contacts = contactsToNotify()
            logger.info("Sending a notification to {} contacts.", contacts.size)
            contacts.forEach { c ->
                logger.info("Sending notification to contact id=${c.id} name=${c.name}")
                val linkForContact = linkForContact(c)
                controlledContactMessagingService.sendMessage(
                    contact = c,
                    body = "Someone's pressing The Button! Join in: $linkForContact"
                )
            }
        }
    }

    /**
     * Prioritized list of contacts based on recent press activity.
     */
    private fun contactsToNotify(): List<Contact> {
        val active = contactDAO.selectActiveContacts()
        val now = Instant.now()
        val contacts = active.filter { c ->
            val prefs = c.notificationPreferences
            if (!prefs.notificationsEnabled) {
                logger.info("Skipping contact id=${c.id} name=${c.name}: notifications disabled")
                return@filter false
            }
            if (prefs.snoozedUntil?.isAfter(now) == true) {
                logger.info("Skipping contact id=${c.id} name=${c.name}: snoozed until ${prefs.snoozedUntil}")
                return@filter false
            }
            if (isInQuietHours(prefs, now)) {
                logger.info(
                    "Skipping contact id=${c.id} name=${c.name}: in quiet hours " +
                        "(${prefs.quietHoursStart}–${prefs.quietHoursEnd} ${prefs.timezone})"
                )
                return@filter false
            }
            true
        }
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(90)
        val aggregatedCounts = contactPressCountDAO.aggregateCountsByContact(startDate, endDate)
        return contacts.sortedByDescending { c -> aggregatedCounts[c.id] ?: 0 }
    }

    internal fun isInQuietHours(prefs: NotificationPreferences, now: Instant): Boolean {
        val tz = prefs.timezone ?: return false
        val start = prefs.quietHoursStart ?: return false
        val end = prefs.quietHoursEnd ?: return false
        val localTime = now.atZone(ZoneId.of(tz)).toLocalTime()
        return if (start <= end) {
            localTime >= start && localTime < end
        } else {
            // Wraps midnight, e.g. 23:00-07:00
            localTime >= start || localTime < end
        }
    }

    private fun linkForContact(contact: Contact): String {
        val token = contactTokenStore.createToken(contact.id)
        return "$link?$TOKEN_PARAMETER=$token"
    }

    override suspend fun released(presser: Presser) {}

    override suspend fun disconnected(presser: Presser) {}
}
