package sh.zachwal.button.shared_html

import kotlinx.html.HEAD
import kotlinx.html.link
import kotlinx.html.meta
import kotlinx.html.script
import kotlinx.html.unsafe
import sh.zachwal.button.config.jsDsn
import sh.zachwal.button.config.jsEnv
import sh.zachwal.button.config.umamiConfig

fun HEAD.bootstrapCss() {
    link(
        rel = "stylesheet",
        href = "https://stackpath.bootstrapcdn.com/bootstrap/4.4.1/css/bootstrap.min.css",
        type = "text/css"
    )
}

fun HEAD.favicon() {
    link(href = "/static/favicon.png", rel = "icon", type = "image/png")
}

fun HEAD.mobileUI() {
    meta {
        name = "viewport"
        content = "width=device-width, initial-scale=1, user-scalable=no"
    }
}

fun HEAD.sentryScript() {
    script(src = "https://js.sentry-cdn.com/$jsDsn.min.js") {
        attributes["crossorigin"] = "anonymous"
    }
    script {
        unsafe {
            +"""
                Sentry.init({
                  environment: "$jsEnv",
                });
            """.trimIndent()
        }
    }
}

// Points at my personal Umami instance for anonymized site analytics
// See https://umami.is
fun HEAD.umamiScript() {
    if (umamiConfig.umamiUrl.isNotBlank()) {
        script {
            defer = true
            src = "https://${umamiConfig.umamiUrl}/script.js"
            attributes["data-website-id"] = umamiConfig.websiteId
        }
    }
}

fun HEAD.headSetup() {
    bootstrapCss()
    favicon()
    mobileUI()
    sentryScript()
    umamiScript()
}
