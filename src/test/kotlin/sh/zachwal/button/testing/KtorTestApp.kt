package sh.zachwal.button.testing

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.session
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import sh.zachwal.button.auth.CONTACT_SESSION_AUTH
import sh.zachwal.button.session.CONTACT_SESSION
import sh.zachwal.button.session.principals.ContactSessionPrincipal
import java.time.Instant

/**
 * Lightweight Ktor integration test harness for contact-auth-gated routes.
 *
 * ## When to use
 *
 * Prefer unit tests (mockk) for pure logic and service-layer code. Use this harness only when
 * the behavior under test lives inside the HTTP layer itself — form parsing, redirects, response
 * codes, or auth gating — things that cannot be exercised without an actual request/response
 * cycle. One test file per controller is a reasonable maximum.
 *
 * ## What it does
 *
 * - Suppresses `application.conf` module loading (which would install real DB-backed plugins).
 * - Installs in-memory Sessions and a contact-session auth provider.
 * - Mounts a `/test/set-session` GET endpoint that seeds a session for contact id [contactId].
 * - Passes the configured [ApplicationTestBuilder] to your [block] so you can add routes,
 *   create a client, and run assertions.
 *
 * ## Usage
 *
 * ```kotlin
 * withContactTestApp(contactId = 1) {
 *     routing { with(controller) { myRoute() } }
 *     val client = createClient { install(HttpCookies) }
 *     client.get("/test/set-session")
 *     val response = client.post("/my-route") { ... }
 *     assertEquals(HttpStatusCode.Found, response.status)
 * }
 * ```
 */
fun withContactTestApp(
    contactId: Int = 1,
    block: suspend ApplicationTestBuilder.() -> Unit,
) = testApplication {
    environment { config = MapApplicationConfig() }
    application {
        installContactTestPlugins(contactId)
    }
    block()
}

private fun Application.installContactTestPlugins(contactId: Int) {
    install(Sessions) {
        cookie<ContactSessionPrincipal>(CONTACT_SESSION)
    }
    install(Authentication) {
        session<ContactSessionPrincipal>(CONTACT_SESSION_AUTH) {
            validate { it.takeIf { p -> p.expiration > Instant.now().toEpochMilli() } }
            challenge { call.respondRedirect("/") }
        }
    }
    routing {
        get("/test/set-session") {
            call.sessions.set(
                ContactSessionPrincipal(
                    contactId = contactId,
                    expiration = Instant.now().plusSeconds(3600).toEpochMilli()
                )
            )
            call.respond(HttpStatusCode.OK)
        }
    }
}
