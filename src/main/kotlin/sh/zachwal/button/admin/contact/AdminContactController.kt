package sh.zachwal.button.admin.contact

import com.google.inject.Inject
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.server.application.call
import io.ktor.server.html.respondHtml
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.html.FlowContent
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.title
import kotlinx.html.tr
import org.slf4j.LoggerFactory
import sh.zachwal.button.controller.Controller
import sh.zachwal.button.db.jdbi.Contact
import sh.zachwal.button.phone.ContactNotFound
import sh.zachwal.button.phone.PhoneBookService
import sh.zachwal.button.phone.UpdatedContact
import sh.zachwal.button.roles.adminRoute
import sh.zachwal.button.shared_html.headSetup

@Controller
class AdminContactController @Inject constructor(
    private val phoneBookService: PhoneBookService
) {

    private val logger = LoggerFactory.getLogger(AdminContactController::class.java)

    internal fun Routing.contactPage() {
        adminRoute("/admin/contacts") {
            get {
                val contacts = phoneBookService.contacts()
                call.respondHtml {
                    head {
                        title {
                            +"Contacts"
                        }
                        headSetup()
                        script(
                            src = "https://ajax.googleapis.com/ajax/libs/jquery/3.5.1/jquery.min" +
                                ".js"
                        ) {}
                        script(src = "/static/src/admin/contacts/update.js") {}
                    }
                    body {
                        div(classes = "container") {
                            h1 {
                                +"Contacts"
                            }
                            contactsTable(contacts)
                        }
                    }
                }
            }
        }
    }

    private fun FlowContent.contactsTable(contacts: List<Contact>) {
        if (contacts.isNotEmpty()) {
            table(classes = "table") {
                thead {
                    tr {
                        th {
                            +"Name"
                        }
                        th {
                            +"Number"
                        }
                        th {
                            +"Active"
                        }
                        th {
                            +"Update"
                        }
                    }
                }
                tbody {
                    contacts.forEach { c ->
                        tr {
                            td {
                                +c.name
                            }
                            td {
                                +c.phoneNumber
                            }
                            td {
                                if (c.active) {
                                    +"✅"
                                } else {
                                    +"❌"
                                }
                            }
                            td {
                                val bootstrapButtonClass = if (c.active) {
                                    "btn-danger"
                                } else {
                                    "btn-success"
                                }
                                val buttonText = if (c.active) {
                                    "Deactivate"
                                } else {
                                    "Activate"
                                }
                                button(classes = "contact-update btn $bootstrapButtonClass") {
                                    attributes["data-contact-id"] = c.id.toString()
                                    attributes["data-contact-active"] = c.active.not().toString()
                                    +buttonText
                                }
                            }
                        }
                    }
                }
            }
        } else {
            p {
                +"No pending users"
            }
        }
    }

    internal fun Routing.updateContact() {
        adminRoute("/admin/contacts/update") {
            post {
                val request = call.receive<UpdateContactRequest>()
                logger.info("Received request to set active=${request.active} for ${request.contactId}")
                when (
                    val result = phoneBookService.updateContactStatus(
                        request.contactId, request.active
                    )
                ) {
                    ContactNotFound -> call.respond(
                        NotFound, "Contact with id ${request.contactId} does not exist."
                    )
                    is UpdatedContact -> {
                        logger.info(
                            "Contact ${result.contact.id} status set to ${result.contact.active}"
                        )
                        call.respond("Success")
                    }
                }
            }
        }
    }
}
