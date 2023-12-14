package sh.zachwal.button.wrapped

import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.route
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.head
import kotlinx.html.title
import sh.zachwal.button.controller.Controller
import sh.zachwal.button.shared_html.headSetup
import javax.inject.Inject


@Controller
class WrappedController @Inject constructor() {

    internal fun Routing.wrappedRoute() {
        route("/wrapped/{year}/{id}") {
            get {
                val year = call.parameters["year"]
                val id = call.parameters["id"]

                call.respondHtml {
                    head {
                        title {
                            +"Wrapped"
                        }
                        headSetup()
                    }
                    body {
                        div(classes = "container") {
                            div(classes = "row justify-content-center") {
                                h1 {
                                    +"Hello, $id!"
                                }
                                h2 {
                                    +"Welcome to your Button Wrapped, $year."
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
