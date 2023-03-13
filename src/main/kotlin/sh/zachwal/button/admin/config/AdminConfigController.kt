package sh.zachwal.button.admin.config

import com.google.inject.Inject
import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.head
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
import sh.zachwal.button.roles.adminRoute
import sh.zachwal.button.shared_html.headSetup

@Controller
class AdminConfigController @Inject constructor(
    private val buttonConfigService: ButtonConfigService
) {

    private val logger = LoggerFactory.getLogger(AdminConfigController::class.java)

    internal fun Routing.adminConfig() {
        adminRoute("/admin/config") {
            get {
                call.respondHtml {
//                    val isCube = buttonConfigService.isCube()
                    val isCube = true
                    head {
                        title {
                            +"Button Config"
                        }
                        headSetup()
                        script(
                            src = "https://ajax.googleapis.com/ajax/libs/jquery/3.5.1/jquery.min" +
                                ".js"
                        ) {}
                        script(src = "/static/src/admin/config/update.js") {}
                    }
                    body {
                        div(classes = "container") {
                            h1 {
                                +"Button Config"
                            }
                            table(classes = "table") {
                                thead {
                                    tr {
                                        th {
                                            +"Config"
                                        }
                                        th {
                                            +"Value"
                                        }
                                        th {
                                            +"Update"
                                        }
                                    }
                                }
                                tbody {

                                    tr {
                                        td {
                                            +"Cube?"
                                        }
                                        td {
                                            +"$isCube"
                                        }
                                        td {
                                            val buttonText = if (isCube) {
                                                "De-Cubify"
                                            } else {
                                                "Cubify"
                                            }
                                            button(classes = "cube-update btn btn-primary") {
                                                attributes["data-cube"] = isCube.not().toString()
                                                +buttonText
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
    }

    internal fun Routing.updateContact() {
        adminRoute("/admin/config/update-cube") {
            post {
                val request = call.receive<UpdateCubeRequest>()
                logger.info("Received request to set cube=${request.isCube}")
//                buttonConfigService.setCube(request.isCube)
                call.respond("Success")
            }
        }
    }
}
