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
    val dbPasswordOverride: String?,
    val websocketUrl: String
) {
    constructor(config: ApplicationConfig) : this(
        env = config.property("ktor.deployment.environment").getString(),
        dbPasswordOverride = config.propertyOrNull("ktor.deployment.db_password")?.getString(),
        websocketUrl = url(config)
    )
}
