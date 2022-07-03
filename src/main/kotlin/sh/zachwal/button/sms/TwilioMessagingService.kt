package sh.zachwal.button.sms

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.google.inject.Inject
import com.google.inject.Singleton
import com.twilio.Twilio
import com.twilio.exception.ApiException
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

    override suspend fun validateNumber(phoneNumber: String): PhoneNumberValidation {
        val number = PhoneNumber.fetcher(com.twilio.type.PhoneNumber(phoneNumber))

        // run the fetch on the Twilio thread
        return withContext(scope.coroutineContext) {
            val validatedNumber = try {
                number.fetchAsync().await()
            } catch (apiException: ApiException) {
                if (apiException.statusCode == 404) {
                    return@withContext InvalidNumber(
                        phoneNumber, "The phone number $phoneNumber does not exist."
                    )
                }

                return@withContext InvalidNumber(phoneNumber, apiException.localizedMessage)
            } catch (e: Exception) {
                throw e
            }

            ValidNumber(validatedNumber.phoneNumber.toString())
        }
    }
}
