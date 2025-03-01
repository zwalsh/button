package sh.zachwal.button.config

import io.sentry.Sentry

data class SentryConfig constructor(
    val kotlinDsn: String,
    val jsDsn: String,
)

lateinit var jsDsn: String
lateinit var jsEnv: String

fun initSentry(sentryConfig: SentryConfig, environment: String) {
    jsDsn = sentryConfig.jsDsn
    jsEnv = environment
    Sentry.init { options ->
        options.dsn = sentryConfig.kotlinDsn
        options.environment = environment
    }
}