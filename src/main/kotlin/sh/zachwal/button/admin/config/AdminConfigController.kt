package sh.zachwal.button.admin.config

import com.google.inject.Inject
import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion
import io.ktor.request.receive
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.util.getOrFail
import kotlinx.html.DIV
import kotlinx.html.FormMethod
import kotlinx.html.FormMethod.post
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.head
import kotlinx.html.id
import kotlinx.html.label
import kotlinx.html.option
import kotlinx.html.script
import kotlinx.html.select
import kotlinx.html.small
import kotlinx.html.style
import kotlinx.html.submitInput
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.textInput
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.title
import kotlinx.html.tr
import org.slf4j.LoggerFactory
import sh.zachwal.button.controller.Controller
import sh.zachwal.button.roles.adminRoute
import sh.zachwal.button.shared_html.headSetup
import java.lang.IllegalArgumentException
import kotlin.math.log

@Controller
class AdminConfigController @Inject constructor(
    private val buttonConfigService: ButtonConfigService
) {

    private val logger = LoggerFactory.getLogger(AdminConfigController::class.java)

    internal fun Routing.adminConfig() {
        adminRoute("/admin/config") {
            get {
                call.respondHtml {
                    val override = buttonConfigService.getOverride()
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
                            h1(classes = "mt-4 text-center") {
                                +"Button Config"
                            }
                            div(classes = "card mt-4") {
                                div(classes = "card-body") {
                                    buttonConfigForm(override)
                                }
                            }
                        }
                    }
                }
            }
            post {
                val params = call.receiveParameters()
                val shape = params.getOrFail("shape")

                if (shape == "none") {
                    logger.info("Removing button override")
                    buttonConfigService.setOverride(null)
                    call.respondRedirect("/admin/config")
                    return@post
                }

                val buttonShape = try {
                    ButtonShape.valueOf(shape)
                } catch (e: IllegalArgumentException) {
                    logger.error(
                        "Tried to set button override to $shape, which is not a ButtonShape", e
                    )
                    call.respond(
                        HttpStatusCode.BadRequest,
                        "Cannot override button shape to $shape"
                    )
                    return@post
                }

                logger.info("Setting button override to $buttonShape")
                buttonConfigService.setOverride(buttonShape)
                call.respondRedirect("/admin/config")
            }
        }
    }

    private fun DIV.buttonConfigForm(override: ButtonShape?) {
        form(
            method = post,
            classes = "mb-1",
            action = "/admin/config"
        ) {
            div(classes = "form-group") {
                h2 {
                    +"Override Button Shape"
                }
            }
            div(classes = "form-group") {
                label { +"Shape" }
                select(classes = "form-control") {
                    name = "shape"
                    id = "shape"

                    option {
                        value = "none"
                        selected = override == null
                        +"None"
                    }
                    ButtonShape.values().forEach { s ->
                        option {
                            value = s.name
                            selected = s == override
                            +s.name
                        }
                    }
                }
            }
            submitInput(classes = "btn btn-primary") {
                value = "Submit"
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
