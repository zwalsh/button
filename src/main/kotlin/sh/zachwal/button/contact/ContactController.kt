package sh.zachwal.button.contact

import io.ktor.http.ContentDisposition
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.html.respondHtml
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import kotlinx.html.ButtonType
import kotlinx.html.DIV
import kotlinx.html.FlowContent
import kotlinx.html.FormMethod
import kotlinx.html.InputType
import kotlinx.html.ThScope
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.head
import kotlinx.html.id
import kotlinx.html.input
import kotlinx.html.label
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.span
import kotlinx.html.table
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.title
import kotlinx.html.tr
import sh.zachwal.button.controller.Controller
import sh.zachwal.button.db.dao.ContactDAO
import sh.zachwal.button.db.jdbi.Contact
import sh.zachwal.button.roles.contactRoute
import sh.zachwal.button.session.principals.ContactSessionPrincipal
import sh.zachwal.button.sharedhtml.bootstrapJs
import sh.zachwal.button.sharedhtml.card
import sh.zachwal.button.sharedhtml.headSetup
import sh.zachwal.button.sharedhtml.jqueryJs
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private val SNOOZE_PRESETS = listOf(
    "1" to "1 day",
    "7" to "7 days",
    "30" to "30 days",
    "90" to "90 days",
)

private val SNOOZE_ZONE = ZoneId.of("America/New_York")

internal fun formatSnoozedUntil(instant: Instant, now: Instant = Instant.now()): String {
    val zoned = instant.atZone(SNOOZE_ZONE)
    val pattern = if (zoned.year == now.atZone(SNOOZE_ZONE).year) "MMM d" else "MMM d, yyyy"
    return DateTimeFormatter.ofPattern(pattern, Locale.US).format(zoned)
}

@Controller
@Singleton
class ContactController @Inject constructor(
    private val contactDAO: ContactDAO,
    private val contactDataService: ContactDataService,
) {

    private fun DIV.contactInfoCard(contact: Contact) {
        card(cardHeader = "Contact Info", classes = "mt-4", cardBodyClasses = "card-body p-0") {
            table(classes = "table mb-0") {
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

    private fun DIV.notificationSettingsCard(contact: Contact) {
        val enabled = contact.notificationPreferences.notificationsEnabled
        card(cardHeader = "Notification Settings", classes = "mt-4") {
            div(classes = "d-flex justify-content-between align-items-center") {
                if (enabled) {
                    span { +"Receive text messages from The Button" }
                } else {
                    span(classes = "text-muted") {
                        +"You won't receive any texts until you turn this back on."
                    }
                }
                form(action = "/contact/preferences/notifications", method = FormMethod.post) {
                    input(type = InputType.hidden, name = "notificationsEnabled") {
                        value = (!enabled).toString()
                    }
                    button(type = ButtonType.submit, classes = "btn btn-outline-secondary") {
                        +if (enabled) "Turn Off" else "Turn On"
                    }
                }
            }
            if (enabled) {
                snoozeSection(contact)
            }
        }
    }

    private fun DIV.snoozeSection(contact: Contact) {
        val snoozedUntil = contact.notificationPreferences.snoozedUntil
        div(classes = "mt-3") {
            if (snoozedUntil != null && snoozedUntil.isAfter(Instant.now())) {
                div(classes = "d-flex justify-content-between align-items-center") {
                    span(classes = "text-muted") {
                        +"Snoozed until ${formatSnoozedUntil(snoozedUntil)}"
                    }
                    form(action = "/contact/preferences/snooze", method = FormMethod.post) {
                        button(type = ButtonType.submit, classes = "btn btn-outline-secondary") {
                            +"Clear Snooze"
                        }
                    }
                }
            } else {
                label { +"Snooze for:" }
                div(classes = "d-flex flex-wrap") {
                    SNOOZE_PRESETS.forEach { (days, display) ->
                        form(
                            action = "/contact/preferences/snooze",
                            method = FormMethod.post,
                            classes = "mr-2 mb-2"
                        ) {
                            input(type = InputType.hidden, name = "days") { value = days }
                            button(type = ButtonType.submit, classes = "btn btn-outline-secondary") {
                                +display
                            }
                        }
                    }
                }
            }
        }
    }

    private fun FlowContent.savedAlert() {
        div(classes = "alert alert-success alert-dismissible fade show fixed-bottom mb-0") {
            id = "savedAlert"
            attributes["role"] = "alert"
            +"Settings saved."
            button(classes = "close") {
                attributes["data-dismiss"] = "alert"
                span { +"×" }
            }
        }
    }

    internal fun Routing.contactSettings() {
        contactRoute("/contact") {
            get {
                val contactSession = call.sessions.get<ContactSessionPrincipal>()!!
                val contact = contactDAO.findContact(contactSession.contactId) ?: run {
                    call.respond(HttpStatusCode.NotFound, "Contact not found")
                    return@get
                }
                val saved = call.request.queryParameters["saved"] == "true"
                call.respondHtml {
                    head {
                        title { +"Settings" }
                        headSetup()
                        jqueryJs()
                        bootstrapJs()
                        script(src = "/static/src/contact/toast.js") {}
                    }
                    body {
                        div(classes = "container") {
                            h1(classes = "mt-4 text-center font-weight-bold") { +"Settings" }
                            contactInfoCard(contact)
                            notificationSettingsCard(contact)
                        }
                        if (saved) {
                            savedAlert()
                        }
                    }
                }
            }
        }
    }

    internal fun Routing.dataManagement() {
        contactRoute("/contact/data") {
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
                        title { +"Data Management" }
                        headSetup()
                    }
                    body {
                        div(classes = "container") {
                            h1(classes = "mt-4 mx-2") {
                                +"Data Management"
                            }
                            h2(classes = "mt-4 mx-2") {
                                +"Export your data."
                            }
                            p {
                                +"""
                                    You can export your Button data in a CSV format. This will include the time of each
                                    press.
                                """.trimIndent()
                            }
                            a(classes = "btn btn-success", href = "/contact/download") {
                                attributes["download"] = "button-data.zip"
                                +"Export Data"
                            }
                            h2(classes = "mt-4 mx-2") {
                                +"Delete your data."
                            }
                            p {
                                +"You can delete your data from the Button database. This is permanent."
                            }
                            button(classes = "btn btn-danger") {
                                +"Delete Data"
                            }
                        }
                    }
                }
            }
        }
    }

    internal fun Routing.contactNotificationsToggle() {
        contactRoute("/contact/preferences/notifications") {
            post {
                val contactSession = call.sessions.get<ContactSessionPrincipal>()!!
                val params = call.receiveParameters()
                val notificationsEnabled = params["notificationsEnabled"] == "true"
                val updated = contactDAO.updateNotificationsEnabled(
                    contactSession.contactId,
                    notificationsEnabled,
                )
                if (updated == null) {
                    call.respond(HttpStatusCode.NotFound, "Contact not found")
                    return@post
                }
                call.respondRedirect("/contact?saved=true")
            }
        }
    }

    internal fun Routing.contactSnooze() {
        contactRoute("/contact/preferences/snooze") {
            post {
                val contactSession = call.sessions.get<ContactSessionPrincipal>()!!
                val params = call.receiveParameters()
                val snoozeDays = params["days"]?.toLongOrNull()
                val snoozedUntil = snoozeDays?.let { Instant.now().plus(it, ChronoUnit.DAYS) }
                val updated = contactDAO.updateSnoozedUntil(
                    contactSession.contactId,
                    snoozedUntil,
                )
                if (updated == null) {
                    call.respond(HttpStatusCode.NotFound, "Contact not found")
                    return@post
                }
                call.respondRedirect("/contact?saved=true")
            }
        }
    }

    internal fun Routing.downloadData() {
        contactRoute("/contact/download") {
            get {
                val contactSession = call.sessions.get<ContactSessionPrincipal>()!!
                val contact = contactDAO.findContact(contactSession.contactId) ?: run {
                    call.respond(
                        HttpStatusCode.NotFound,
                        "Contact not found"
                    )
                    return@get
                }

                call.response.header(
                    name = HttpHeaders.ContentDisposition,
                    value = ContentDisposition.Attachment.withParameter(
                        ContentDisposition.Parameters.FileName,
                        "button-data.zip"
                    ).toString()
                )

                call.respondOutputStream {
                    contactDataService.writeAllPressesToStream(contact.id, this@respondOutputStream)
                }
            }
        }
    }
}
