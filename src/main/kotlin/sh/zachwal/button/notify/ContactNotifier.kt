package sh.zachwal.button.notify

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.google.inject.Inject
import com.google.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import sh.zachwal.button.db.dao.ContactDAO
import sh.zachwal.button.presser.Presser
import sh.zachwal.button.presser.PresserObserver
import sh.zachwal.button.sms.ControlledContactMessagingService
import java.util.concurrent.Executors
import kotlin.concurrent.thread

@Singleton
class ContactNotifier @Inject constructor(
    private val contactDAO: ContactDAO,
    private val controlledContactMessagingService: ControlledContactMessagingService,
) : PresserObserver {

    private val logger = LoggerFactory.getLogger(ContactNotifier::class.java)

    private val threadPool = Executors.newFixedThreadPool(
        1,
        ThreadFactoryBuilder()
            .setNameFormat("contact-notifier-thread-%d")
            .build()
    )
    private val scope = CoroutineScope(threadPool.asCoroutineDispatcher())

    init {
        Runtime.getRuntime().addShutdownHook(
            thread(start = false) {
                threadPool.shutdownNow()
            }
        )
    }

    override suspend fun pressed(presser: Presser) = withContext(scope.coroutineContext) {
        // TODO grab last notification
        // TODO if > 1d ago, create a new row
        // TODO else return

        val contacts = contactDAO.selectActiveContacts()
        logger.info("Sending a notification to ${contacts.size} contacts.")
        contacts.forEach { c ->
            controlledContactMessagingService.sendMessage(
                contact = c,
                // TODO parameterize url
                body = "Someone's pressing The Button! Join in: https://button.zachwal.sh"
            )
            // Don't get our number blocked
            delay(1000)
        }
    }

    override suspend fun released(presser: Presser) {}

    override suspend fun disconnected(presser: Presser) {}
}
