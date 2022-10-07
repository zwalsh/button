package sh.zachwal.button.sms

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.google.inject.Singleton
import com.twilio.Twilio
import com.twilio.exception.ApiException
import com.twilio.rest.api.v2010.account.Message
import com.twilio.rest.api.v2010.account.Message.Status.FAILED
import com.twilio.rest.api.v2010.account.Message.Status.QUEUED
import com.twilio.rest.lookups.v1.PhoneNumber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import sh.zachwal.button.config.TwilioConfig
import java.util.concurrent.Executors
import kotlin.concurrent.thread

@Singleton
class TwilioMessagingService constructor(
    twilioConfig: TwilioConfig
) : MessagingService {

    private val fromNumber = com.twilio.type.PhoneNumber(twilioConfig.fromNumber)
    private val logger = LoggerFactory.getLogger(TwilioMessagingService::class.java)
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

    override suspend fun sendMessage(toPhoneNumber: String, body: String): MessageStatus =
        withContext(scope.coroutineContext) {
            val message: Message = Message.creator(
                com.twilio.type.PhoneNumber(toPhoneNumber),
                fromNumber,
                body
            ).createAsync().await()

            when (message.status) {
                QUEUED -> {
                    logger.info("Sent message to $toPhoneNumber with id ${message.sid}.")

                    MessageQueued(
                        id = message.sid,
                        sentDate = message.dateCreated.toInstant()
                    )
                }
                FAILED -> {
                    logger.warn(
                        "Failed to send message to $toPhoneNumber: ${message.sid}, " +
                            "${message.errorMessage}."
                    )

                    MessageFailed(message.errorMessage)
                }
                else -> {
                    logger.error(
                        "Got unhandled message status from Twilio when sending message to " +
                            "$toPhoneNumber: ${message.status}, ${message.errorMessage}"
                    )
                    throw RuntimeException("Unhandled Twilio message status: ${message.status}")
                }
            }
        }
}
