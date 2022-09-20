package sh.zachwal.button.guice

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import com.google.inject.name.Named
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import sh.zachwal.button.sms.MessagingService
import sh.zachwal.button.sms.TwilioMessagingService
import java.util.concurrent.Executors

class ApplicationModule : AbstractModule() {

    @Provides
    @Singleton
    @Named("presserDispatcher")
    fun presserDispatcher(): CoroutineDispatcher = Executors.newFixedThreadPool(4)
        .asCoroutineDispatcher()
}
