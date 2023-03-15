package sh.zachwal.button.home

import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.http.HttpStatusCode
import io.ktor.routing.Routing
import io.ktor.routing.get
import kotlinx.css.button
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
import sh.zachwal.button.admin.config.ButtonConfigService
import sh.zachwal.button.admin.config.ButtonShape
import sh.zachwal.button.admin.config.ButtonShape.CHRISTMAS_TREE
import sh.zachwal.button.admin.config.ButtonShape.CIRCLE
import sh.zachwal.button.admin.config.ButtonShape.CUBE
import sh.zachwal.button.admin.config.ButtonShape.HEART
import sh.zachwal.button.admin.config.ButtonShape.SHAMROCK
import sh.zachwal.button.admin.config.ButtonShape.TURKEY
import sh.zachwal.button.admin.config.isSpecial
import sh.zachwal.button.config.AppConfig
import sh.zachwal.button.controller.Controller
import sh.zachwal.button.shared_html.favicon
import sh.zachwal.button.shared_html.mobileUI
import javax.inject.Inject
import kotlin.IllegalArgumentException

@Controller
class HomeController @Inject constructor(
    private val appConfig: AppConfig,
    private val buttonConfigService: ButtonConfigService,
) {
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
}
