package sh.zachwal.button

import com.fasterxml.jackson.databind.SerializationFeature
import com.google.inject.Guice
import com.zaxxer.hikari.HikariConfig
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.auth.Authentication
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.features.StatusPages
import io.ktor.features.XForwardedHeaderSupport
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.jackson.jackson
import io.ktor.routing.routing
import io.ktor.sessions.Sessions
import io.ktor.sessions.cookie
import io.ktor.websocket.WebSockets
import org.slf4j.event.Level
import sh.zachwal.authserver.auth.configureFormAuth
import sh.zachwal.authserver.auth.configureSessionAuth
import sh.zachwal.authserver.controller.createControllers
import sh.zachwal.button.features.configureRoleAuthorization
import sh.zachwal.button.features.configureStatusPages
import sh.zachwal.button.guice.ApplicationModule
import sh.zachwal.button.guice.HikariModule
import sh.zachwal.button.guice.JdbiModule
import sh.zachwal.button.roles.RoleAuthorization
import sh.zachwal.button.roles.RoleService
import sh.zachwal.button.session.DbSessionStorage
import sh.zachwal.button.session.SessionCleanupTask
import sh.zachwal.button.session.SessionPrincipal
import sh.zachwal.button.users.UserService
import kotlin.collections.set
import kotlin.time.ExperimentalTime

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

private fun url(host: String, port: Int?, protocol: String): String {
    val prefix = "$protocol://$host"
    val withPort = port?.let { "$prefix:$it" } ?: prefix
    return "$withPort/socket"
}

@ExperimentalTime
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    val env = this.environment.config.property("ktor.deployment.environment").getString()
    val host = environment.config.property("ktor.deployment.ws_host").getString()
    val wsProtocol = environment.config.property("ktor.deployment.ws_protocol").getString()
    val port = if (host == "localhost") 8080 else null
    val url = url(host, port, wsProtocol)

    log.info("Starting app in $env")

    val hikariConfig = HikariConfig("/hikari.properties")
    val injector = Guice.createInjector(
        ApplicationModule(url),
        JdbiModule(),
        HikariModule(hikariConfig)
    )

    val userService = injector.getInstance(UserService::class.java)
    val roleService = injector.getInstance(RoleService::class.java)
    val dbSessionStorage = injector.getInstance(DbSessionStorage::class.java)

    install(CallLogging) {
        level = Level.INFO
    }
    install(DefaultHeaders)
    install(WebSockets) {
        pingPeriodMillis = 1000L
        timeoutMillis = 30_000L
    }

    if (host != "localhost") {
        install(XForwardedHeaderSupport)
    }

    install(Sessions) {
        cookie<SessionPrincipal>(
            "AUTH_SESSION",
            storage = dbSessionStorage
        ) {
            cookie.httpOnly = true
            cookie.secure = env != "dev"
            cookie.extensions["SameSite"] = "lax"
        }
    }

    install(Authentication) {
        configureFormAuth(userService)
        configureSessionAuth()
    }

    install(RoleAuthorization) {
        configureRoleAuthorization(this, this@module, userService, roleService)
    }

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }

    install(StatusPages) {
        configureStatusPages()
    }

    createControllers(injector)

    routing {
        static("static") {
            resources("static")
        }
    }

    // clean up expired sessions every hour
    val cleanupTask = injector.getInstance(SessionCleanupTask::class.java)
    cleanupTask.repeatCleanup()
}
