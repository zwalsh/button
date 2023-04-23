package sh.zachwal.button.sentry

import io.sentry.Sentry
import sh.zachwal.button.config.SentryConfig

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
