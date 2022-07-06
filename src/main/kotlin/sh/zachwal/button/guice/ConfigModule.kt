package sh.zachwal.button.guice

import com.google.inject.AbstractModule
import com.google.inject.Provides
import io.ktor.config.ApplicationConfig
import sh.zachwal.button.config.AppConfig

class ConfigModule(
    private val applicationConfig: ApplicationConfig,
) : AbstractModule() {

    override fun configure() {
        bind(AppConfig::class.java).toInstance(AppConfig(applicationConfig))
    }

    @Provides
    fun twilioConfig(appConfig: AppConfig) = appConfig.twilioConfig

    @Provides
    fun messagingConfig(appConfig: AppConfig) = appConfig.messagingConfig
}
