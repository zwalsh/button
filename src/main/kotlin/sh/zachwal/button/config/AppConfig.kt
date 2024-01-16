package sh.zachwal.button.config

import io.ktor.config.ApplicationConfig

private fun url(config: ApplicationConfig): String {
    val host = config.property("ktor.deployment.ws_host").getString()
    val port = if (host == "localhost") 8080 else null
    val wsProtocol = config.property("ktor.deployment.ws_protocol").getString()
    val prefix = "$wsProtocol://$host"
    val withPort = port?.let { "$prefix:$it" } ?: prefix
    return "$withPort/socket"
}

data class AppConfig(
    val env: String,
    val host: String,
    val dbNameOverride: String?,
    val dbUserOverride: String?,
    val dbPasswordOverride: String?,
    val websocketUrl: String,
    val twilioConfig: TwilioConfig?,
    val messagingConfig: MessagingConfig,
    val cubeButton: Boolean,
    val sentryConfig: SentryConfig
) {
    constructor(config: ApplicationConfig) : this(
        env = config.property("ktor.deployment.environment").getString(),
        host = config.property("ktor.deployment.ws_host").getString(),
        dbNameOverride = config.propertyOrNull("ktor.deployment.db_name")?.getString(),
        dbUserOverride = config.propertyOrNull("ktor.deployment.db_user")?.getString(),
        dbPasswordOverride = config.propertyOrNull("ktor.deployment.db_password")?.getString(),
        websocketUrl = url(config),
        twilioConfig = config.propertyOrNull("ktor.twilio.account")?.let {
            TwilioConfig(
                config.property("ktor.twilio.account").getString(),
                config.property("ktor.twilio.authToken").getString(),
                config.property("ktor.twilio.messagingServiceSid").getString(),
            )
        },
        messagingConfig = MessagingConfig(
            monthlyLimit = config.property("ktor.messaging.monthlyLimit").getString().toInt(),
            adminPhone = config.property("ktor.messaging.adminPhone").getString()
        ),
        cubeButton = config.property("ktor.button.cube").getString().toBooleanStrict(),
        sentryConfig = SentryConfig(
            kotlinDsn = config.property("ktor.sentry.kotlinDsn").getString(),
            jsDsn = config.property("ktor.sentry.jsDsn").getString(),
        ),
    )
}
