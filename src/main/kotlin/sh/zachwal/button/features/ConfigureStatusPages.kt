package sh.zachwal.button.features

import io.ktor.http.HttpStatusCode
import io.ktor.server.html.respondHtml
import io.ktor.server.plugins.statuspages.StatusPagesConfig
import io.ktor.server.request.uri
import io.sentry.Sentry
import kotlinx.html.DIV
import kotlinx.html.HTML
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.p
import kotlinx.html.title
import org.slf4j.LoggerFactory
import sh.zachwal.button.auth.UnauthorizedException
import sh.zachwal.button.shared_html.headSetup

private val logger = LoggerFactory.getLogger("StatusPage")

fun StatusPagesConfig.configureStatusPages() {

    exception<Throwable> { call, cause ->
        logger.error("Unhandled error", cause)
        Sentry.captureException(cause)
        call.respondHtml(HttpStatusCode.InternalServerError) {
            statusPage("Internal Server Error") {
                h1 {
                    +"Error!"
                }
                p {
                    +"${cause.message}"
                }
            }
        }
    }

    status(HttpStatusCode.NotFound) { call, status ->
        call.respondHtml(HttpStatusCode.NotFound) {
            statusPage("${status.value} Not Found") {
                h1 {
                    +"Resource not found"
                }
                p {
                    +" ${status.value}, ${status.description}"
                }
            }
        }
    }

    exception<UnauthorizedException> { call, cause ->
        call.respondHtml(HttpStatusCode.Unauthorized) {
            statusPage("Unauthorized") {
                h1 {
                    +"Unauthorized"
                }
                p {
                    +(
                        "You are not logged in, or do not have the correct permissions, to access " +
                            call.request.uri
                        )
                }
                p {
                    +"${cause.message}"
                }
            }
        }
    }

    status(HttpStatusCode.Unauthorized) { call, status ->
        call.respondHtml(HttpStatusCode.Unauthorized) {
            statusPage("${status.value} Not Logged In") {
                h1 {
                    +"Unauthorized"
                }
                p {
                    +"You must be logged in to access: ${call.request.uri}"
                }
                p {
                    +" ${status.value} ${status.description}"
                }
            }
        }
    }

    status(HttpStatusCode.Forbidden) { call, status ->
        call.respondHtml(HttpStatusCode.Forbidden) {
            statusPage("${status.value} Access Denied") {
                h1 {
                    +"Access Denied"
                }
                p {
                    +"You do not have permission to access: ${call.request.uri}"
                }
                p {
                    +" ${status.value} ${status.description}"
                }
            }
        }
    }
}

private fun HTML.statusPage(title: String, block: DIV.() -> Unit) {
    head {
        title {
            +title
        }
        headSetup()
    }
    body {
        div(classes = "container", block = block)
    }
}
