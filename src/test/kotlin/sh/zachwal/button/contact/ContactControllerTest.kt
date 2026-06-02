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
import io.mockk.verify
import org.junit.jupiter.api.Test
import sh.zachwal.button.db.dao.ContactDAO
import sh.zachwal.button.db.jdbi.contact
import sh.zachwal.button.testing.withContactTestApp
import kotlin.test.assertEquals

internal class ContactControllerTest {

    private val contactDAO = mockk<ContactDAO>()
    private val contactDataService = mockk<ContactDataService>()
    private val controller = ContactController(contactDAO, contactDataService)

    @Test
    fun `POST preferences with notificationsEnabled present calls DAO with true`() =
        withContactTestApp(contactId = 1) {
            routing { with(controller) { contactPreferences() } }
            every { contactDAO.updateNotificationPreferences(1, true) } returns contact(id = 1)

            val client = createClient { install(HttpCookies) }
            client.get("/test/set-session")

            val response = client.post("/contact/preferences") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("notificationsEnabled=on")
            }

            assertEquals(HttpStatusCode.Found, response.status)
            assertEquals("/contact?saved=true", response.headers[HttpHeaders.Location])
            verify { contactDAO.updateNotificationPreferences(1, true) }
        }

    @Test
    fun `POST preferences with notificationsEnabled absent calls DAO with false`() =
        withContactTestApp(contactId = 1) {
            routing { with(controller) { contactPreferences() } }
            every { contactDAO.updateNotificationPreferences(1, false) } returns contact(id = 1)

            val client = createClient { install(HttpCookies) }
            client.get("/test/set-session")

            val response = client.post("/contact/preferences") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("")
            }

            assertEquals(HttpStatusCode.Found, response.status)
            assertEquals("/contact?saved=true", response.headers[HttpHeaders.Location])
            verify { contactDAO.updateNotificationPreferences(1, false) }
        }
}
