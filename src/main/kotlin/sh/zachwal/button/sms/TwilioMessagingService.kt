package sh.zachwal.button.sms

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.google.inject.Inject
import com.google.inject.Singleton
import com.twilio.Twilio
import com.twilio.rest.lookups.v1.PhoneNumber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import sh.zachwal.button.config.TwilioConfig
import java.util.concurrent.Executors
import kotlin.concurrent.thread

@Singleton
class TwilioMessagingService @Inject constructor(
    twilioConfig: TwilioConfig
) : MessagingService {

    private val threadPool = Executors.newFixedThreadPool(
        1,
        ThreadFactoryBuilder()
            .setNameFormat("twilio-thread-%d")
            .build()
    )
    private val scope = CoroutineScope(threadPool.asCoroutineDispatcher())

    init {
        Twilio.init(twilioConfig.accountSID, twilioConfig.authToken)
        Runtime.getRuntime().addShutdownHook(
            thread(start = false) {
                threadPool.shutdownNow()
            }
        )
    }

    override suspend fun validateNumber(phoneNumber: String): String {
        val number = PhoneNumber.fetcher(com.twilio.type.PhoneNumber(phoneNumber))

        // run the fetch on the Twilio thread
        return withContext(scope.coroutineContext) {
            val validatedNumber = number.fetchAsync().await()
            validatedNumber.phoneNumber.toString()
        }
    }
}
