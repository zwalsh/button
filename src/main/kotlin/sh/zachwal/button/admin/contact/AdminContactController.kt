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
import kotlinx.html.FormMethod
import kotlinx.html.InputType
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.input
import kotlinx.html.meta
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
import sh.zachwal.button.admin.ContactPressStat
import sh.zachwal.button.admin.ContactPressStatsService
import sh.zachwal.button.admin.TimeRange
import sh.zachwal.button.controller.Controller
import sh.zachwal.button.phone.ContactNotFound
import sh.zachwal.button.phone.PhoneBookService
import sh.zachwal.button.phone.UpdatedContact
import sh.zachwal.button.roles.adminRoute
import sh.zachwal.button.sharedhtml.headSetup
import java.net.URLEncoder

@Controller
class AdminContactController @Inject constructor(
    private val phoneBookService: PhoneBookService,
    private val contactPressStatsService: ContactPressStatsService,
) {

    private val logger = LoggerFactory.getLogger(AdminContactController::class.java)

    internal fun Routing.contactPage() {
        adminRoute("/admin/contacts") {
            get {
                val query = call.request.queryParameters["query"]?.lowercase()?.takeIf { it.isNotEmpty() }
                val activeFilter = call.request.queryParameters["active"]?.toBooleanStrictOrNull()

                val allStats = contactPressStatsService.allContactStats(TimeRange.LAST_90_DAYS)
                val filtered = allStats
                    .filter { row ->
                        query == null ||
                            row.contact.name.lowercase().contains(query) ||
                            row.contact.phoneNumber.contains(query)
                    }
                    .filter { row ->
                        activeFilter == null || row.contact.active == activeFilter
                    }

                call.respondHtml {
                    head {
                        title {
                            +"Contacts"
                        }
                        headSetup()
                        meta {
                            name = "view-transition"
                            content = "same-origin"
                        }
                        script(
                            src = "https://ajax.googleapis.com/ajax/libs/jquery/3.5.1/jquery.min" +
                                ".js"
                        ) {}
                        script(src = "/static/src/admin/contacts/update.js") {}
                    }
                    body {
                        div(classes = "container") {
                            h1(classes = "mt-4 text-center") {
                                +"Contacts"
                            }
                            searchForm(query, activeFilter)
                            contactsTable(filtered)
                        }
                    }
                }
            }
        }
    }

    private fun FlowContent.searchForm(query: String?, activeFilter: Boolean?) {
        form(action = "/admin/contacts", method = FormMethod.get) {
            if (activeFilter != null) {
                input(type = InputType.hidden) {
                    name = "active"
                    value = activeFilter.toString()
                }
            }
            div(classes = "input-group mb-2") {
                input(type = InputType.text, classes = "form-control") {
                    name = "query"
                    placeholder = "Search by name or number"
                    value = query ?: ""
                    attributes["autocomplete"] = "off"
                }
                div(classes = "input-group-append") {
                    button(classes = "btn btn-outline-secondary") {
                        attributes["type"] = "submit"
                        +"Search"
                    }
                }
            }
            div(classes = "mb-3 text-center") {
                div(classes = "btn-group") {
                    filterLink("All", null, activeFilter, query)
                    filterLink("Active", true, activeFilter, query)
                    filterLink("Inactive", false, activeFilter, query)
                }
            }
        }
    }

    private fun FlowContent.filterLink(label: String, value: Boolean?, activeFilter: Boolean?, query: String?) {
        val isSelected = value == activeFilter
        val btnClass = if (isSelected) "btn btn-primary" else "btn btn-outline-secondary"
        val params = buildList {
            if (value != null) add("active=$value")
            if (query != null) add("query=${URLEncoder.encode(query, "UTF-8")}")
        }.joinToString("&")
        val href = "/admin/contacts" + if (params.isNotEmpty()) "?$params" else ""
        a(href = href, classes = btnClass) {
            +label
        }
    }

    private fun FlowContent.contactsTable(rows: List<ContactPressStat>) {
        if (rows.isNotEmpty()) {
            table(classes = "table") {
                thead {
                    tr {
                        th { +"Name" }
                        th { +"Number" }
                        th { +"Active" }
                        th { +"Presses (90d)" }
                        th { +"Update" }
                    }
                }
                tbody {
                    rows.forEach { row ->
                        val c = row.contact
                        tr {
                            td { +c.name }
                            td { +c.phoneNumber }
                            td {
                                if (c.active) +"✅" else +"❌"
                            }
                            td { +row.count.toString() }
                            td {
                                val bootstrapButtonClass = if (c.active) "btn-danger" else "btn-success"
                                val buttonText = if (c.active) "Deactivate" else "Activate"
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
                +"No contacts found"
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
