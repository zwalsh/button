package sh.zachwal.button.notify

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.google.inject.Inject
import com.google.inject.Singleton
import com.google.inject.name.Named
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import sh.zachwal.button.auth.contact.ContactTokenStore
import sh.zachwal.button.db.dao.ContactDAO
import sh.zachwal.button.db.dao.NotificationDAO
import sh.zachwal.button.db.jdbi.Contact
import sh.zachwal.button.home.TOKEN_PARAMETER
import sh.zachwal.button.presser.Presser
import sh.zachwal.button.presser.PresserObserver
import sh.zachwal.button.sms.ControlledContactMessagingService
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors
import kotlin.concurrent.thread

@Singleton
class ContactNotifier @Inject constructor(
    private val contactDAO: ContactDAO,
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
                "Last notification was at ${lastNotification?.sentDate}, " +
                    "sending a new one " +
                    "triggered by contact=${presser.contact} " +
                    "at remote=${presser.remote()}"
            )
            // TODO: Create the notification with the triggering contact id & remote address
            notificationDAO.createNotification()

            val contacts = contactDAO.selectActiveContacts()
            logger.info("Sending a notification to ${contacts.size} contacts.")
            contacts.forEach { c ->
                val linkForContact = linkForContact(c)
                controlledContactMessagingService.sendMessage(
                    contact = c,
                    body = "Someone's pressing The Button! Join in: $linkForContact"
                )
                // Don't get our number blocked
                delay(1000)
            }
        }
    }

    private fun linkForContact(contact: Contact): String {
        val token = contactTokenStore.createToken(contact.id)
        return "$link?$TOKEN_PARAMETER=$token"
    }

    override suspend fun released(presser: Presser) {}

    override suspend fun disconnected(presser: Presser) {}
}
