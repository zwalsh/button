package sh.zachwal.button.admin.config

import com.google.inject.Inject
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.html.respondHtml
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.util.getOrFail
import kotlinx.html.DIV
import kotlinx.html.FormMethod.post
import kotlinx.html.body
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
import kotlinx.html.submitInput
import kotlinx.html.title
import org.slf4j.LoggerFactory
import sh.zachwal.button.controller.Controller
import sh.zachwal.button.roles.adminRoute
import sh.zachwal.button.shared_html.headSetup
import java.lang.IllegalArgumentException

@Controller
class AdminConfigController @Inject constructor(
    private val buttonConfigService: ButtonConfigService
) {

    private val logger = LoggerFactory.getLogger(AdminConfigController::class.java)

    internal fun Routing.adminConfig() {
        adminRoute("/admin/config") {
            get {
                call.respondHtml {
                    val buttonShapeOverride = buttonConfigService.getOverride()
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
                                    buttonConfigForm(buttonShapeOverride)
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

    private fun DIV.buttonConfigForm(buttonShapeOverride: ButtonShape?) {
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
                        selected = buttonShapeOverride == null
                        +"None"
                    }
                    ButtonShape.values().forEach { s ->
                        option {
                            value = s.name
                            selected = s == buttonShapeOverride
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
}
