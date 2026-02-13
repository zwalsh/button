package sh.zachwal.button

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.random.Random.Default.nextLong
import kotlin.test.Ignore

class LoadTest {

    @Test
    @Ignore // don't run in CI
    fun `can handle load`() {
        val clients = 100
        val requests = 100
        val host = "wss://button.zachwal.sh" // or ws://localhost
        val port = 443 // or 8080
        val path = "/socket"

        runBlocking {
            repeat(clients) {
                launch {
                    launchClient(host, port, path, requests)
                }
            }
        }
    }

    private suspend fun launchClient(host: String, port: Int, path: String, requests: Int) {
        val client = HttpClient(CIO) {
            install(WebSockets)
        }
        client.webSocket(urlString = "$host:$port$path") {
            val sender = launch {
                var current = "pressing"
                for (i in 1..requests) {
                    val delayMs = nextLong(1000, 10000) // wait between 1 and 10 seconds
                    delay(delayMs)
                    send(Frame.Text(current))
                    current = when (current) {
                        "released" -> "pressing"
                        else -> "released"
                    }
                }
            }
            val receiver = launch {
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> println(frame.readText())
                        else -> println("Unexpected frame! $frame")
                    }
                }
            }

            sender.join()
            receiver.join()
            println("Done")
        }
    }
}
