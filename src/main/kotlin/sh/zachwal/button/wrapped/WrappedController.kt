package sh.zachwal.button.wrapped

import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.util.getOrFail
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.head
import kotlinx.html.p
import kotlinx.html.title
import sh.zachwal.button.controller.Controller
import sh.zachwal.button.shared_html.headSetup
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

                call.respondHtml {
                    head {
                        title {
                            +"${wrapped.year} Wrapped"
                        }
                        headSetup()
                    }
                    body {
                        div(classes = "container") {
                            div(classes = "row justify-content-center") {
                                h1 {
                                    +"Hello, ${wrapped.id}!"
                                }
                                h2 {
                                    +"Welcome to your Button Wrapped, ${wrapped.year}."
                                }

                                p {
                                    +"You pressed the Button ${wrapped.count} times this year."
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
