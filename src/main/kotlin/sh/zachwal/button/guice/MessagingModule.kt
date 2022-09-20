package sh.zachwal.button.guice

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import sh.zachwal.button.config.AppConfig
import sh.zachwal.button.sms.MessagingService
import sh.zachwal.button.sms.StdOutMessagingService
import sh.zachwal.button.sms.TwilioMessagingService
import javax.annotation.Nullable

class MessagingModule : AbstractModule() {

    @Provides
    @Singleton
    fun twilioMessagingService(appConfig: AppConfig): TwilioMessagingService? {
        return appConfig.twilioConfig?.let {
            TwilioMessagingService(it)
        }
    }

    @Provides
    @Singleton
    fun messagingService(
        @Nullable
        twilioMessagingService: TwilioMessagingService?,
        stdOutMessagingService: StdOutMessagingService
    ): MessagingService {
        return twilioMessagingService ?: stdOutMessagingService
    }
}
