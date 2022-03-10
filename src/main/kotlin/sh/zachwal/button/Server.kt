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
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.html.HTML
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
import org.slf4j.event.Level.INFO
import sh.zachwal.button.presser.Presser
import sh.zachwal.button.presser.PresserManager
import java.util.concurrent.Executors


fun index(url: String): HTML.() -> Unit = {
    head {
        title("Hello from Ktor!")
        meta {
            name = "viewport"
            content = "width=device-width, initial-scale=1"
        }
        link(href = "static/src/style.css", rel = "stylesheet")
        script {
            unsafe {
                +"let wsUrl = \"$url\";"
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
        }
    }
}

fun main(args: Array<String>) = io.ktor.server.netty.EngineMain.main(args)


fun url(host: String, port: Int?): String {
    val prefix = "ws://$host"
    val withPort = port?.let { prefix + ":$it" } ?: prefix
    return "$withPort/socket"
}

@Suppress("unused")
fun Application.module(testing: Boolean = false) {
    val host = environment.config.property("ktor.deployment.ws_host").getString()
    val port = if (host == "localhost") 8080 else null
    val url = url(host, port)


    install(CallLogging) {
        level = INFO
    }
    install(DefaultHeaders)
    install(WebSockets) {
        pingPeriodMillis = 1000L
        timeoutMillis = 30_000L
    }

    routing {
        static("static") {
            resources("static")
        }
    }

    routing {
        get("/") {
            call.respondHtml(HttpStatusCode.OK, index(url))
        }
    }

    // all presser coroutines will run on this threadpool
    val presserDispatcher = Executors.newFixedThreadPool(4).asCoroutineDispatcher()

    val manager = PresserManager()

    routing {
        webSocket("/socket") {
            val presser = Presser(this, manager, presserDispatcher)
            manager.addPresser(presser)
            presser.watchChannels()
        }
    }
}
