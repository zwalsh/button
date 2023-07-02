package sh.zachwal.button.admin

import com.google.inject.Inject
import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.routing.Routing
import io.ktor.routing.get
import kotlinx.html.TBODY
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.head
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.title
import kotlinx.html.tr
import sh.zachwal.button.controller.Controller
import sh.zachwal.button.db.dao.PressDAO
import sh.zachwal.button.db.jdbi.RecentPressCount
import sh.zachwal.button.roles.adminRoute
import sh.zachwal.button.shared_html.card
import sh.zachwal.button.shared_html.headSetup
import sh.zachwal.button.shared_html.responsiveTable
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

@Controller
class AdminStatsController @Inject constructor(
    private val pressDAO: PressDAO
) {

    private val cardClasses = "col-8 mt-4 h-100"

    internal fun Routing.recentPresses() {
        adminRoute("/admin/recent-presses") {
            get {
                val recentPresses = pressDAO.recentPresses()

                call.respondHtml {
                    head {
                        title {
                            +"Recent Presses"
                        }
                        headSetup()
                    }
                    body {
                        div(classes = "container") {
                            h1(classes = "mt-4 text-center") {
                                +"Recent Presses"
                            }
                            div(classes = "row justify-content-center") {
                                responsiveTable(classes = "m-2") {
                                    thead {
                                        tr {
                                            th {
                                                +"Name"
                                            }
                                            th {
                                                +"Count"
                                            }
                                            th {
                                                +"Last Press"
                                            }
                                        }
                                    }
                                    tbody {
                                        recentPresses.forEach { recentPressCount ->
                                            recentPressRow(recentPressCount)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    private fun TBODY.recentPressRow(recentPressCount: RecentPressCount) {
        val mostRecentFormatted = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            .format(recentPressCount.mostRecent.atZone(ZoneId.of("America/New_York")))

        tr {
            th {
                +(recentPressCount.name ?: "")
            }
            td {
                +recentPressCount.count.toString()
            }
            td {
                +mostRecentFormatted
            }
        }
    }

    internal fun Routing.stats() {
        adminRoute("/admin/press-stats") {
            get {
                val now = Instant.now()
                val thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS)
                val todayMidnight = now.truncatedTo(ChronoUnit.DAYS)
                val pressCountToday = pressDAO.countSince(time = todayMidnight)
                val pressCount30Days = pressDAO.countSince(time = thirtyDaysAgo)
                val pressCountForever = pressDAO.countSince(time = Instant.EPOCH)

                call.respondHtml {
                    head {
                        title {
                            +"Press Stats"
                        }
                        headSetup()
                    }
                    body {
                        div(classes = "container") {
                            h1(classes = "text-center") {
                                +"Press Stats"
                            }
                            div(classes = "row justify-content-center") {
                                card(cardHeader = "Presses Today", classes = cardClasses) {
                                    h2(classes = "text-center") {
                                        +"$pressCountToday"
                                    }
                                }
                            }
                            div(classes = "row justify-content-center") {
                                card(cardHeader = "Presses Last 30 Days", classes = cardClasses) {
                                    h2(classes = "text-center") {
                                        +"$pressCount30Days"
                                    }
                                }
                            }
                            div(classes = "row justify-content-center") {
                                card(cardHeader = "Presses All-Time", classes = cardClasses) {
                                    h2(classes = "text-center") {
                                        +"$pressCountForever"
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
