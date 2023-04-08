package sh.zachwal.button.admin

import com.google.inject.Inject
import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.routing.Routing
import io.ktor.routing.get
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.head
import kotlinx.html.title
import sh.zachwal.button.controller.Controller
import sh.zachwal.button.db.dao.PressDAO
import sh.zachwal.button.roles.adminRoute
import sh.zachwal.button.shared_html.card
import sh.zachwal.button.shared_html.headSetup
import java.time.Instant
import java.time.temporal.ChronoUnit

@Controller
class AdminStatsController @Inject constructor(
    private val pressDAO: PressDAO
) {

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
                            val cardClasses = "col-8 mt-4 h-100"
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
