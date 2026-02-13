package sh.zachwal.button.ktorutils

import io.ktor.server.plugins.origin
import io.ktor.server.request.ApplicationRequest

fun ApplicationRequest.remote(): String {
    val clientHost = origin.remoteHost
    val clientPort = origin.remotePort
    return "$clientHost:$clientPort"
}
