package sh.zachwal.button.contact

import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.routing.routing
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import sh.zachwal.button.db.dao.ContactDAO
import sh.zachwal.button.db.jdbi.contact
import sh.zachwal.button.testing.withContactTestApp
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class ContactControllerTest {

    private val contactDAO = mockk<ContactDAO>()
    private val contactDataService = mockk<ContactDataService>()
    private val controller = ContactController(contactDAO, contactDataService)

    @Test
    fun `POST preferences with notificationsEnabled present calls DAO with true`() =
        withContactTestApp(contactId = 1) {
            routing { with(controller) { contactPreferences() } }
            every { contactDAO.updateNotificationPreferences(1, true, null) } returns contact(id = 1)

            val client = createClient { install(HttpCookies) }
            client.get("/test/set-session")

            val response = client.post("/contact/preferences") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("notificationsEnabled=on")
            }

            assertEquals(HttpStatusCode.Found, response.status)
            assertEquals("/contact?saved=true", response.headers[HttpHeaders.Location])
            verify { contactDAO.updateNotificationPreferences(1, true, null) }
        }

    @Test
    fun `POST preferences with notificationsEnabled absent calls DAO with false`() =
        withContactTestApp(contactId = 1) {
            routing { with(controller) { contactPreferences() } }
            every { contactDAO.updateNotificationPreferences(1, false, null) } returns contact(id = 1)

            val client = createClient { install(HttpCookies) }
            client.get("/test/set-session")

            val response = client.post("/contact/preferences") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("")
            }

            assertEquals(HttpStatusCode.Found, response.status)
            assertEquals("/contact?saved=true", response.headers[HttpHeaders.Location])
            verify { contactDAO.updateNotificationPreferences(1, false, null) }
        }

    @Test
    fun `POST preferences with snoozePreset 7 calls DAO with snoozedUntil about 7 days out`() =
        withContactTestApp(contactId = 1) {
            routing { with(controller) { contactPreferences() } }
            val snoozedUntil = slot<Instant>()
            every {
                contactDAO.updateNotificationPreferences(1, true, capture(snoozedUntil))
            } returns contact(id = 1)

            val client = createClient { install(HttpCookies) }
            client.get("/test/set-session")

            val response = client.post("/contact/preferences") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("notificationsEnabled=on&snoozePreset=7")
            }

            assertEquals(HttpStatusCode.Found, response.status)
            assertEquals("/contact?saved=true", response.headers[HttpHeaders.Location])
            val expected = Instant.now().plus(7, ChronoUnit.DAYS)
            assertTrue(
                snoozedUntil.captured.isAfter(expected.minusSeconds(5)) &&
                    snoozedUntil.captured.isBefore(expected.plusSeconds(5))
            )
        }

    @Test
    fun `POST preferences with snoozePreset none calls DAO with null snoozedUntil`() =
        withContactTestApp(contactId = 1) {
            routing { with(controller) { contactPreferences() } }
            every { contactDAO.updateNotificationPreferences(1, true, null) } returns contact(id = 1)

            val client = createClient { install(HttpCookies) }
            client.get("/test/set-session")

            val response = client.post("/contact/preferences") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("notificationsEnabled=on&snoozePreset=none")
            }

            assertEquals(HttpStatusCode.Found, response.status)
            assertEquals("/contact?saved=true", response.headers[HttpHeaders.Location])
            verify { contactDAO.updateNotificationPreferences(1, true, null) }
        }

    @Test
    fun `formatSnoozedUntil omits year when same as now`() {
        val now = Instant.parse("2026-08-09T12:00:00Z")
        val snoozedUntil = Instant.parse("2026-08-16T12:00:00Z")
        assertEquals("Aug 16", formatSnoozedUntil(snoozedUntil, now))
    }

    @Test
    fun `formatSnoozedUntil includes year when different from now`() {
        val now = Instant.parse("2026-12-30T12:00:00Z")
        val snoozedUntil = Instant.parse("2027-01-06T12:00:00Z")
        assertEquals("Jan 6, 2027", formatSnoozedUntil(snoozedUntil, now))
    }
}
