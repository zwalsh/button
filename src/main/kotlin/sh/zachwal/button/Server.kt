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
import kotlinx.html.div
import kotlinx.html.head
import kotlinx.html.id
import kotlinx.html.script
import kotlinx.html.title
import kotlinx.html.unsafe
import org.slf4j.event.Level.INFO
import sh.zachwal.button.presser.Presser
import sh.zachwal.button.presser.PresserManager
import java.util.concurrent.Executors

const val url = "ws://localhost:8080/socket"
fun HTML.index() {
    head {
        title("Hello from Ktor!")
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
        div {
            +"Hello from Button Presser"
        }
        div {
            id = "buttonPressCount"
            +"Current presser count: 0"
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
            call.respondHtml(HttpStatusCode.OK, HTML::index)
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
