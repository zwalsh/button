package sh.zachwal.button

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import io.ktor.html.respondHtml
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.routing.get
import io.ktor.routing.routing
import kotlinx.html.HTML
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.head
import kotlinx.html.script
import kotlinx.html.title
import org.slf4j.event.Level.INFO

fun HTML.index() {
    head {
        title("Hello from Ktor!")
        script {
            src = "static/src/main.js"
        }
    }
    body {
        div {
            +"Hello from Ktor"
        }
    }
}

fun main(args: Array<String>) = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused")
fun Application.module(testing: Boolean = false) {
    install(CallLogging) {
        level = INFO
    }
    install(DefaultHeaders)

    routing {
        static("static") {
            resources("static")
        }
    }

    routing {
        get("/") {
            call.respondHtml(HttpStatusCode.OK, HTML::index)
        }
    }
}
