package sh.zachwal.button.home

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.http.HttpStatusCode
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.util.pipeline.PipelineContext
import kotlinx.html.Draggable.htmlFalse
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.draggable
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.id
import kotlinx.html.img
import kotlinx.html.link
import kotlinx.html.onDragStart
import kotlinx.html.script
import kotlinx.html.span
import kotlinx.html.title
import kotlinx.html.unsafe
import org.slf4j.LoggerFactory
import sh.zachwal.button.admin.config.ButtonConfigService
import sh.zachwal.button.admin.config.ButtonShape
import sh.zachwal.button.admin.config.ButtonShape.CHRISTMAS_TREE
import sh.zachwal.button.admin.config.ButtonShape.CIRCLE
import sh.zachwal.button.admin.config.ButtonShape.CUBE
import sh.zachwal.button.admin.config.ButtonShape.HEART
import sh.zachwal.button.admin.config.ButtonShape.SHAMROCK
import sh.zachwal.button.admin.config.ButtonShape.TURKEY
import sh.zachwal.button.admin.config.isSpecial
import sh.zachwal.button.auth.ContactTokenStore
import sh.zachwal.button.config.AppConfig
import sh.zachwal.button.controller.Controller
import sh.zachwal.button.session.SessionService
import sh.zachwal.button.shared_html.favicon
import sh.zachwal.button.shared_html.mobileUI
import javax.inject.Inject
import kotlin.IllegalArgumentException

const val TOKEN_PARAMETER = "t"

@Controller
class HomeController @Inject constructor(
    private val appConfig: AppConfig,
    private val buttonConfigService: ButtonConfigService,
    private val contactTokenStore: ContactTokenStore,
    private val sessionService: SessionService,
) {

    private val logger = LoggerFactory.getLogger(HomeController::class.java)
    private fun svgForShape(shape: ButtonShape): String {
        return when (shape) {
            CIRCLE -> throw IllegalArgumentException("No svg for circle")
            CUBE -> throw IllegalArgumentException("No svg for cube")
            SHAMROCK -> "static/special/shamrock.svg"
            HEART -> "static/special/heart.svg"
            CHRISTMAS_TREE -> "static/special/christmas-tree.svg"
            TURKEY -> "static/special/turkey.svg"
        }
    }

    internal fun Routing.home() {
        get("/") {
            checkContactToken()

            val buttonShape = buttonConfigService.currentShape()
            call.respondHtml(HttpStatusCode.OK) {
                head {
                    title("button.")
                    mobileUI()
                    favicon()

                    link(href = "static/src/style.css", rel = "stylesheet")
                    if (buttonShape.isSpecial()) {
                        link(href = "static/src/special.css", rel = "stylesheet")
                    }
                    if (buttonShape == CUBE) {
                        link(href = "static/src/cube.css", rel = "stylesheet")
                    }

                    script {
                        unsafe {
                            +"let wsUrl = \"${appConfig.websocketUrl}\";"
                        }
                    }
                    script {
                        src = "static/src/main.js"
                    }
                }
                body {
                    div(classes = "container") {
                        if (buttonShape.isSpecial()) {
                            div(classes = "specialContainer") {
                                img(src = svgForShape(buttonShape)) {
                                    id = "pressMePls"
                                    draggable = htmlFalse
                                    onDragStart = "false;"
                                }
                                span {
                                    +"PRESS"
                                }
                            }
                        } else {
                            button {
                                id = "pressMePls"
                                span {
                                    +"PRESS"
                                }
                            }
                        }

                        h1 {
                            id = "buttonPressCount"
                            +"BUTTON PRESSERS: 0"
                        }
                        h1(classes = "whiteButton") {
                            id = "buttonPressCountWhite"
                            +"BUTTON PRESSERS: 0"
                        }
                        div {
                            id = "signup"
                            +"Love the button? "
                            a(href = "/phone/signup") {
                                +"Sign up for texts."
                            }
                        }
                    }
                }
            }
        }
    }

    private fun PipelineContext<Unit, ApplicationCall>.checkContactToken() {
        val token = call.parameters[TOKEN_PARAMETER]

        if (token != null) {
            logger.info("Got token $token, checking & setting session")
            val contactId = contactTokenStore.checkToken(token)
            if (contactId != null) {
                logger.info("Token is associated with id $contactId, creating session.")
                sessionService.createContactSession(call, contactId)
            } else {
                logger.info("Token was not associated with an id.")
            }
        } else {
            logger.info("No token on call")
        }
    }
}
