package sh.zachwal.button.admin

import com.google.inject.Inject
import io.ktor.server.application.call
import io.ktor.server.html.respondHtml
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import kotlinx.html.FormMethod
import kotlinx.html.HTML
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.link
import kotlinx.html.option
import kotlinx.html.script
import kotlinx.html.select
import kotlinx.html.span
import kotlinx.html.submitInput
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.title
import kotlinx.html.tr
import sh.zachwal.button.controller.Controller
import sh.zachwal.button.roles.adminRoute
import sh.zachwal.button.roles.contactRoute
import sh.zachwal.button.sharedhtml.headSetup
import sh.zachwal.button.sharedhtml.responsiveTable

@Controller
class ContactPressStatsController @Inject constructor(
    private val service: ContactPressStatsService,
) {

    internal fun Routing.contactPressStats() {
        adminRoute("/admin/contact-press-stats") {
            get {
                val range = TimeRange.fromParam(call.request.queryParameters["range"])
                val stats = service.pressStats(range)
                call.respondHtml { leaderboardPage(range, stats) }
            }
        }
        contactRoute("/leaderboard") {
            get {
                val range = TimeRange.fromParam(call.request.queryParameters["range"])
                val stats = service.pressStats(range)
                call.respondHtml { leaderboardPage(range, stats) }
            }
        }
    }

    private fun HTML.leaderboardPage(range: TimeRange, stats: List<ContactPressStat>) {
        head {
            title { +"Leaderboard" }
            headSetup()
            link(rel = "stylesheet", href = "/static/src/css/leaderboard.css", type = "text/css")
        }
        body {
            div(classes = "container") {
                h1(classes = "mt-4 text-center") { +"Leaderboard" }
                form(classes = "my-3 px-2", method = FormMethod.get) {
                    div(classes = "d-flex align-items-center") {
                        select(classes = "form-control flex-grow-1 mr-2") {
                            name = "range"
                            TimeRange.entries.forEach { tr ->
                                option {
                                    value = tr.queryParam
                                    selected = tr == range
                                    +tr.label
                                }
                            }
                        }
                        submitInput(classes = "btn btn-primary") { value = "Go" }
                    }
                    div(classes = "text-center mt-1") {
                        span(classes = "text-muted small") {
                            attributes["id"] = "stale-indicator"
                            attributes["style"] = "display:none"
                            +"Hit Go to refresh"
                        }
                    }
                }
                responsiveTable(classes = "mt-3") {
                    thead {
                        tr {
                            th { +"Name" }
                            th { +"Presses" }
                        }
                    }
                    tbody {
                        stats.forEach { stat ->
                            tr {
                                attributes["style"] = "view-transition-name: contact-${stat.contact.id}"
                                td { +stat.contact.name }
                                td { +stat.count.toString() }
                            }
                        }
                    }
                }
            }
            script { src = "/static/src/js/leaderboard.js" }
        }
    }
}
