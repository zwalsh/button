package sh.zachwal.button.wrapped

import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.util.getOrFail
import kotlinx.html.DIV
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.head
import kotlinx.html.link
import kotlinx.html.p
import kotlinx.html.title
import sh.zachwal.button.controller.Controller
import sh.zachwal.button.shared_html.headSetup
import java.time.format.TextStyle.FULL
import java.util.Locale
import javax.inject.Inject


@Controller
class WrappedController @Inject constructor(
    private val wrappedService: WrappedService
) {

    internal fun Routing.wrappedRoute() {
        route("/wrapped/{year}/{id}") {
            get {
                val year = call.parameters.getOrFail("year").toInt()
                val id = call.parameters.getOrFail("id")

                val wrapped = wrappedService.wrapped(year, id)

                val favoriteDayString = wrapped.favoriteDay.getDisplayName(FULL, Locale.US)

                call.respondHtml {
                    head {
                        title {
                            +"${wrapped.year} Wrapped"
                        }
                        headSetup()
                        link(href = "/static/src/css/wrapped.css", rel = "stylesheet")
                    }
                    body {
                        div(classes = "container") {
                            wrappedSection(cardClasses = "card vw-100 m-3 p-3 d-flex justify-content-center") {
                                h1 {
                                    +"Hello, ${wrapped.id}!"
                                }
                                h2 {
                                    +"Welcome to your Button Wrapped, ${wrapped.year}."
                                }
                            }
                            wrappedSection {
                                p {
                                    +"You pressed the Button ${wrapped.count} times this year."
                                }
                            }
                            wrappedSection {
                                p {
                                    +"You really loved ${favoriteDayString}!"
                                }
                                p {
                                    +("You pressed the Button ${wrapped.favoriteDayCount} times " +
                                        "on" +
                                        " ${favoriteDayString}s.")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun DIV.wrappedSection(
        cardClasses: String = "card vw-100 m-3 p-3",
        content: DIV.() -> Unit
    ) {
        div(classes = "row vh-90 snapChild") {
            div(classes = cardClasses) {
                content()
            }
        }
    }
}
