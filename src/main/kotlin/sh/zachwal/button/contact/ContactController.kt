package sh.zachwal.button.contact

import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import kotlinx.css.td
import kotlinx.html.ThScope
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.table
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.title
import kotlinx.html.tr
import sh.zachwal.button.controller.Controller
import sh.zachwal.button.db.dao.ContactDAO
import sh.zachwal.button.roles.contactRoute
import sh.zachwal.button.session.principals.ContactSessionPrincipal
import sh.zachwal.button.shared_html.headSetup
import javax.inject.Inject
import javax.inject.Singleton

@Controller
@Singleton
class ContactController @Inject constructor(
    private val contactDAO: ContactDAO
) {

    internal fun Routing.contactInfo() {
        contactRoute("/contact") {
            get {
                val contactSession = call.sessions.get<ContactSessionPrincipal>()!!
                val contact = contactDAO.findContact(contactSession.contactId) ?: run {
                    call.respond(
                        HttpStatusCode.NotFound,
                        "Contact not found"
                    )
                    return@get
                }
                call.respondHtml {
                    head {
                        title { +"Contact" }
                        headSetup()
                    }
                    body {
                        div(classes = "container") {
                            h1(classes = "mt-4 mx-2") {
                                +"Contact Info"
                            }
                            table(classes = "table mt-4") {
                                tr {
                                    th {
                                        scope = ThScope.row
                                        +"Name"
                                    }
                                    td { +contact.name }
                                }
                                tr {
                                    th {
                                        scope = ThScope.row
                                        +"Phone number"
                                    }
                                    td { +contact.phoneNumber }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
