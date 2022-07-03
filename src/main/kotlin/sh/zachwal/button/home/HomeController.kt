package sh.zachwal.button.home

import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.http.HttpStatusCode
import io.ktor.routing.Routing
import io.ktor.routing.get
import kotlinx.html.HEAD
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.id
import kotlinx.html.link
import kotlinx.html.meta
import kotlinx.html.script
import kotlinx.html.span
import kotlinx.html.title
import kotlinx.html.unsafe
import sh.zachwal.button.config.AppConfig
import sh.zachwal.button.controller.Controller
import sh.zachwal.button.shared_html.mobileUI
import javax.inject.Inject

@Controller
class HomeController @Inject constructor(
    private val appConfig: AppConfig,
) {
    internal fun Routing.home() {
        get("/") {
            call.respondHtml(HttpStatusCode.OK) {
                head {
                    title("button.")
                    mobileUI()
                    link(href = "static/src/style.css", rel = "stylesheet")
                    favicon()
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
                        button {
                            id = "pressMePls"
                            span {
                                +"PRESS"
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
                    }
                }
            }
        }
    }

    private fun HEAD.favicon() {
        link(href = "static/favicon.png", rel = "icon", type = "image/png")
    }
}
