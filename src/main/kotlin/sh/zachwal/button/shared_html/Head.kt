package sh.zachwal.button.shared_html

import kotlinx.html.HEAD
import kotlinx.html.link

fun HEAD.bootstrapCss() {
    link(rel = "stylesheet", href = "https://stackpath.bootstrapcdn.com/bootstrap/4.4.1/css/bootstrap.min.css", type = "text/css")
}
