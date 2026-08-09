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
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class ContactControllerTest {

    private val contactDAO = mockk<ContactDAO>()
    private val contactDataService = mockk<ContactDataService>()
    private val controller = ContactController(contactDAO, contactDataService)

    @Test
    fun `POST notifications with notificationsEnabled=true calls DAO with true`() =
        withContactTestApp(contactId = 1) {
            routing { with(controller) { contactNotificationsToggle() } }
            every { contactDAO.updateNotificationsEnabled(1, true) } returns contact(id = 1)

            val client = createClient { install(HttpCookies) }
            client.get("/test/set-session")

            val response = client.post("/contact/preferences/notifications") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("notificationsEnabled=true")
            }

            assertEquals(HttpStatusCode.Found, response.status)
            assertEquals("/contact?saved=true", response.headers[HttpHeaders.Location])
            verify { contactDAO.updateNotificationsEnabled(1, true) }
        }

    @Test
    fun `POST notifications with notificationsEnabled=false calls DAO with false`() =
        withContactTestApp(contactId = 1) {
            routing { with(controller) { contactNotificationsToggle() } }
            every { contactDAO.updateNotificationsEnabled(1, false) } returns contact(id = 1)

            val client = createClient { install(HttpCookies) }
            client.get("/test/set-session")

            val response = client.post("/contact/preferences/notifications") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("notificationsEnabled=false")
            }

            assertEquals(HttpStatusCode.Found, response.status)
            assertEquals("/contact?saved=true", response.headers[HttpHeaders.Location])
            verify { contactDAO.updateNotificationsEnabled(1, false) }
        }

    @Test
    fun `POST snooze with days=7 calls DAO with snoozedUntil about 7 days out`() =
        withContactTestApp(contactId = 1) {
            routing { with(controller) { contactSnooze() } }
            val snoozedUntil = slot<Instant>()
            every {
                contactDAO.updateSnoozedUntil(1, capture(snoozedUntil))
            } returns contact(id = 1)

            val client = createClient { install(HttpCookies) }
            client.get("/test/set-session")

            val response = client.post("/contact/preferences/snooze") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("days=7")
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
    fun `POST snooze with no days field clears snoozedUntil`() =
        withContactTestApp(contactId = 1) {
            routing { with(controller) { contactSnooze() } }
            every { contactDAO.updateSnoozedUntil(1, null) } returns contact(id = 1)

            val client = createClient { install(HttpCookies) }
            client.get("/test/set-session")

            val response = client.post("/contact/preferences/snooze") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("")
            }

            assertEquals(HttpStatusCode.Found, response.status)
            assertEquals("/contact?saved=true", response.headers[HttpHeaders.Location])
            verify { contactDAO.updateSnoozedUntil(1, null) }
        }

    @Test
    fun `POST quiet-hours with times and timezone calls DAO with parsed values`() =
        withContactTestApp(contactId = 1) {
            routing { with(controller) { contactQuietHours() } }
            every {
                contactDAO.updateQuietHours(1, LocalTime.of(23, 0), LocalTime.of(7, 0), "America/New_York")
            } returns contact(id = 1)

            val client = createClient { install(HttpCookies) }
            client.get("/test/set-session")

            val response = client.post("/contact/preferences/quiet-hours") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("quietHoursStart=23:00&quietHoursEnd=07:00&timezone=America/New_York")
            }

            assertEquals(HttpStatusCode.Found, response.status)
            assertEquals("/contact?saved=true", response.headers[HttpHeaders.Location])
            verify { contactDAO.updateQuietHours(1, LocalTime.of(23, 0), LocalTime.of(7, 0), "America/New_York") }
        }

    @Test
    fun `POST quiet-hours with empty times clears quiet hours but keeps timezone`() =
        withContactTestApp(contactId = 1) {
            routing { with(controller) { contactQuietHours() } }
            every {
                contactDAO.updateQuietHours(1, null, null, "America/Chicago")
            } returns contact(id = 1)

            val client = createClient { install(HttpCookies) }
            client.get("/test/set-session")

            val response = client.post("/contact/preferences/quiet-hours") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("quietHoursStart=&quietHoursEnd=&timezone=America/Chicago")
            }

            assertEquals(HttpStatusCode.Found, response.status)
            assertEquals("/contact?saved=true", response.headers[HttpHeaders.Location])
            verify { contactDAO.updateQuietHours(1, null, null, "America/Chicago") }
        }

    @Test
    fun `POST quiet-hours with only one time field returns 400`() =
        withContactTestApp(contactId = 1) {
            routing { with(controller) { contactQuietHours() } }

            val client = createClient { install(HttpCookies) }
            client.get("/test/set-session")

            val response = client.post("/contact/preferences/quiet-hours") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("quietHoursStart=23:00&quietHoursEnd=&timezone=America/New_York")
            }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

    @Test
    fun `POST quiet-hours with times but no timezone returns 400`() =
        withContactTestApp(contactId = 1) {
            routing { with(controller) { contactQuietHours() } }

            val client = createClient { install(HttpCookies) }
            client.get("/test/set-session")

            val response = client.post("/contact/preferences/quiet-hours") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("quietHoursStart=23:00&quietHoursEnd=07:00")
            }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

    @Test
    fun `POST quiet-hours with invalid timezone returns 400`() =
        withContactTestApp(contactId = 1) {
            routing { with(controller) { contactQuietHours() } }

            val client = createClient { install(HttpCookies) }
            client.get("/test/set-session")

            val response = client.post("/contact/preferences/quiet-hours") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("quietHoursStart=23:00&quietHoursEnd=07:00&timezone=Not/AZone")
            }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

    @Test
    fun `POST quiet-hours with invalid time format returns 400`() =
        withContactTestApp(contactId = 1) {
            routing { with(controller) { contactQuietHours() } }

            val client = createClient { install(HttpCookies) }
            client.get("/test/set-session")

            val response = client.post("/contact/preferences/quiet-hours") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("quietHoursStart=not-a-time&quietHoursEnd=07:00&timezone=America/New_York")
            }

            assertEquals(HttpStatusCode.BadRequest, response.status)
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
