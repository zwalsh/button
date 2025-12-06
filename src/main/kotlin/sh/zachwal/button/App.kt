package sh.zachwal.button

import com.fasterxml.jackson.databind.SerializationFeature
import com.google.inject.Guice
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
import sh.zachwal.button.auth.configureContactSessionAuth
import sh.zachwal.button.auth.configureFormAuth
import sh.zachwal.button.auth.configureUserSessionAuth
import sh.zachwal.button.auth.contact.ContactTokenCleanupTask
import sh.zachwal.button.presshistory.ContactPressCountMaterializationTask
import sh.zachwal.button.config.AppConfig
import sh.zachwal.button.config.initSentry
import sh.zachwal.button.config.initUmami
import sh.zachwal.button.controller.createControllers
import sh.zachwal.button.features.configureRoleAuthorization
import sh.zachwal.button.features.configureStatusPages
import sh.zachwal.button.guice.ApplicationModule
import sh.zachwal.button.guice.ConfigModule
import sh.zachwal.button.guice.HikariModule
import sh.zachwal.button.guice.JacksonModule
import sh.zachwal.button.guice.JdbiModule
import sh.zachwal.button.guice.MessagingModule
import sh.zachwal.button.roles.RoleAuthorization
import sh.zachwal.button.roles.RoleService
import sh.zachwal.button.session.CONTACT_SESSION
import sh.zachwal.button.session.CONTACT_SESSION_LENGTH
import sh.zachwal.button.session.DbSessionStorage
import sh.zachwal.button.session.SessionCleanupTask
import sh.zachwal.button.session.USER_SESSION
import sh.zachwal.button.session.USER_SESSION_LENGTH
import sh.zachwal.button.session.principals.ContactSessionPrincipal
import sh.zachwal.button.session.principals.UserSessionPrincipal
import sh.zachwal.button.users.UserService
import kotlin.collections.set
import kotlin.time.ExperimentalTime

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@ExperimentalTime
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    val injector = Guice.createInjector(
        ApplicationModule(),
        ConfigModule(environment.config),
        JdbiModule(),
        HikariModule(),
        MessagingModule(),
        JacksonModule()
    )

    val config = injector.getInstance(AppConfig::class.java)
    log.info("Starting app in ${config.env}")

    initSentry(config.sentryConfig, config.env)
    initUmami(config.umamiConfig)

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

    if (config.env != "DEV") {
        install(XForwardedHeaderSupport)
    }

    install(Sessions) {
        cookie<UserSessionPrincipal>(
            USER_SESSION,
            storage = dbSessionStorage
        ) {
            cookie.httpOnly = true
            cookie.secure = config.env != "DEV"
            cookie.extensions["SameSite"] = "lax"
            cookie.maxAgeInSeconds = USER_SESSION_LENGTH.toSeconds()
        }
        cookie<ContactSessionPrincipal>(
            CONTACT_SESSION,
            storage = dbSessionStorage
        ) {
            cookie.httpOnly = true
            cookie.secure = config.env != "DEV"
            cookie.extensions["SameSite"] = "lax"
            cookie.maxAgeInSeconds = CONTACT_SESSION_LENGTH.toSeconds()
        }
    }

    install(Authentication) {
        configureFormAuth(userService)
        configureUserSessionAuth()
        configureContactSessionAuth()
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

    // clean up expired contact tokens every hour
    val contactTokenCleanupTask = injector.getInstance(ContactTokenCleanupTask::class.java)
    contactTokenCleanupTask.repeatCleanup()

    // materialize contact press counts daily
    val contactPressCountMaterializationTask = injector.getInstance(ContactPressCountMaterializationTask::class.java)
    contactPressCountMaterializationTask.repeatDaily()
}
