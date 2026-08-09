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
import kotlinx.html.hr
import kotlinx.html.id
import kotlinx.html.input
import kotlinx.html.label
import kotlinx.html.option
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.select
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
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
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

private val QUIET_HOURS_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

internal fun formatSnoozedUntil(instant: Instant, now: Instant = Instant.now()): String {
    val zoned = instant.atZone(SNOOZE_ZONE)
    val pattern = if (zoned.year == now.atZone(SNOOZE_ZONE).year) "MMM d" else "MMM d, yyyy"
    return DateTimeFormatter.ofPattern(pattern, Locale.US).format(zoned)
}

/**
 * Friendly display name for a timezone, matching the curated options in the contact page's
 * timezone selector. Falls back to the raw IANA string if not found in the curated list.
 */
internal fun timezoneDisplayName(zoneId: String): String {
    return (TIMEZONE_OPTIONS + OTHER_TIMEZONE_OPTIONS).find { it.zoneId == zoneId }?.display ?: zoneId
}

private data class TimezoneOption(val display: String, val zoneId: String) {
    /**
     * The offset shifts with DST, so this is computed per-request rather than once at startup.
     */
    fun labelWithOffset(now: Instant = Instant.now()): String {
        val offset = ZoneId.of(zoneId).rules.getOffset(now).id.let { if (it == "Z") "+00:00" else it }
        return "(GMT$offset) $display"
    }
}

private val TIMEZONE_OPTIONS = listOf(
    TimezoneOption("Eastern Time (US)", "America/New_York"),
    TimezoneOption("Central Time (US)", "America/Chicago"),
    TimezoneOption("Mountain Time (US)", "America/Denver"),
    TimezoneOption("Pacific Time (US)", "America/Los_Angeles"),
)

// Curated for population coverage, not exhaustiveness: every ~hour-wide GMT offset band with
// significant population should be represented by at least one zone. See PHASE_3.md for the
// coverage check behind this list.
private val OTHER_TIMEZONE_OPTIONS = listOf(
    TimezoneOption("Alaska", "America/Anchorage"),
    TimezoneOption("Hawaii", "Pacific/Honolulu"),
    TimezoneOption("Atlantic (Canada)", "America/Halifax"),
    TimezoneOption("Mexico City", "America/Mexico_City"),
    TimezoneOption("Sao Paulo", "America/Sao_Paulo"),
    TimezoneOption("London / UTC", "Europe/London"),
    TimezoneOption("Central Europe", "Europe/Paris"),
    TimezoneOption("Eastern Europe", "Europe/Helsinki"),
    TimezoneOption("Moscow", "Europe/Moscow"),
    TimezoneOption("Lagos", "Africa/Lagos"),
    TimezoneOption("Johannesburg", "Africa/Johannesburg"),
    TimezoneOption("Cairo", "Africa/Cairo"),
    TimezoneOption("Nairobi", "Africa/Nairobi"),
    TimezoneOption("Dubai", "Asia/Dubai"),
    TimezoneOption("Karachi", "Asia/Karachi"),
    TimezoneOption("India", "Asia/Kolkata"),
    TimezoneOption("Dhaka", "Asia/Dhaka"),
    TimezoneOption("Bangkok", "Asia/Bangkok"),
    TimezoneOption("Singapore / HK", "Asia/Singapore"),
    TimezoneOption("Shanghai", "Asia/Shanghai"),
    TimezoneOption("Tokyo", "Asia/Tokyo"),
    TimezoneOption("Seoul", "Asia/Seoul"),
    TimezoneOption("Sydney", "Australia/Sydney"),
    TimezoneOption("Perth", "Australia/Perth"),
    TimezoneOption("Auckland", "Pacific/Auckland"),
)

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
            notificationToggleSection(contact)
            if (enabled) {
                hr(classes = "my-3")
                snoozeSection(contact)
                hr(classes = "my-3")
                quietHoursSection(contact)
            }
        }
    }

    private fun DIV.notificationToggleSection(contact: Contact) {
        val enabled = contact.notificationPreferences.notificationsEnabled
        div(classes = "d-flex justify-content-between align-items-center") {
            if (enabled) {
                span { +"Text notifications are on." }
            } else {
                span(classes = "text-muted") {
                    +"Text notifications are off."
                }
            }
            form(action = "/contact/preferences/notifications", method = FormMethod.post) {
                input(type = InputType.hidden, name = "notificationsEnabled") {
                    value = (!enabled).toString()
                }
                val toggleClasses = if (enabled) {
                    "btn btn-outline-danger text-nowrap flex-shrink-0 ml-3"
                } else {
                    "btn btn-outline-success text-nowrap flex-shrink-0 ml-3"
                }
                button(type = ButtonType.submit, classes = toggleClasses) {
                    +if (enabled) "Turn Off" else "Turn On"
                }
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

    private fun DIV.quietHoursSection(contact: Contact) {
        val prefs = contact.notificationPreferences
        div(classes = "mt-3") {
            label { +"Quiet hours" }
            form(
                action = "/contact/preferences/quiet-hours",
                method = FormMethod.post,
                classes = "form-row align-items-end"
            ) {
                div(classes = "form-group col-auto mb-2") {
                    label(classes = "small text-muted mb-1") {
                        htmlFor = "quietHoursStart"
                        +"From"
                    }
                    input(
                        type = InputType.time,
                        name = "quietHoursStart",
                        classes = "form-control form-control-sm"
                    ) {
                        id = "quietHoursStart"
                        value = prefs.quietHoursStart?.format(QUIET_HOURS_TIME_FORMAT) ?: ""
                    }
                }
                div(classes = "form-group col-auto mb-2") {
                    label(classes = "small text-muted mb-1") {
                        htmlFor = "quietHoursEnd"
                        +"To"
                    }
                    input(
                        type = InputType.time,
                        name = "quietHoursEnd",
                        classes = "form-control form-control-sm"
                    ) {
                        id = "quietHoursEnd"
                        value = prefs.quietHoursEnd?.format(QUIET_HOURS_TIME_FORMAT) ?: ""
                    }
                }
                div(classes = "form-group col-auto mb-2") {
                    label(classes = "small text-muted mb-1") {
                        htmlFor = "timezone-select"
                        +"Timezone"
                    }
                    select(classes = "form-control form-control-sm") {
                        name = "timezone"
                        val selectedZone = prefs.timezone ?: "America/New_York"
                        TIMEZONE_OPTIONS.forEach { tz ->
                            option {
                                value = tz.zoneId
                                selected = tz.zoneId == selectedZone
                                +tz.labelWithOffset()
                            }
                        }
                        option {
                            disabled = true
                            +"──────────────────"
                        }
                        OTHER_TIMEZONE_OPTIONS
                            .sortedBy { ZoneId.of(it.zoneId).rules.getOffset(Instant.now()).totalSeconds }
                            .forEach { tz ->
                                option {
                                    value = tz.zoneId
                                    selected = tz.zoneId == selectedZone
                                    +tz.labelWithOffset()
                                }
                            }
                    }
                }
                div(classes = "form-group col-auto mb-2") {
                    button(type = ButtonType.submit, classes = "btn btn-outline-secondary btn-sm") {
                        +"Save"
                    }
                }
            }
            if (prefs.quietHoursStart != null) {
                form(
                    action = "/contact/preferences/quiet-hours",
                    method = FormMethod.post,
                    classes = "mt-2"
                ) {
                    input(type = InputType.hidden, name = "quietHoursStart") { value = "" }
                    input(type = InputType.hidden, name = "quietHoursEnd") { value = "" }
                    prefs.timezone?.let { tz ->
                        input(type = InputType.hidden, name = "timezone") { value = tz }
                    }
                    button(type = ButtonType.submit, classes = "btn btn-outline-secondary btn-sm") {
                        +"Clear Quiet Hours"
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

    internal fun Routing.contactQuietHours() {
        contactRoute("/contact/preferences/quiet-hours") {
            post {
                val contactSession = call.sessions.get<ContactSessionPrincipal>()!!
                val params = call.receiveParameters()
                val startParam = params["quietHoursStart"]?.takeIf { it.isNotBlank() }
                val endParam = params["quietHoursEnd"]?.takeIf { it.isNotBlank() }
                val timezoneParam = params["timezone"]?.takeIf { it.isNotBlank() }

                if ((startParam == null) != (endParam == null)) {
                    call.respond(HttpStatusCode.BadRequest, "quietHoursStart and quietHoursEnd must be set together")
                    return@post
                }

                if (timezoneParam != null) {
                    try {
                        ZoneId.of(timezoneParam)
                    } catch (e: DateTimeException) {
                        call.respond(HttpStatusCode.BadRequest, "Invalid timezone: $timezoneParam")
                        return@post
                    }
                }

                if (startParam != null && timezoneParam == null) {
                    call.respond(HttpStatusCode.BadRequest, "timezone is required when quiet hours are set")
                    return@post
                }

                val quietHoursStart: LocalTime?
                val quietHoursEnd: LocalTime?
                try {
                    quietHoursStart = startParam?.let { LocalTime.parse(it) }
                    quietHoursEnd = endParam?.let { LocalTime.parse(it) }
                } catch (e: DateTimeParseException) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid time format, expected HH:mm")
                    return@post
                }

                val updated = contactDAO.updateQuietHours(
                    contactSession.contactId,
                    quietHoursStart,
                    quietHoursEnd,
                    timezoneParam,
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
