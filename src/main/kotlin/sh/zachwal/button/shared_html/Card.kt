package sh.zachwal.button.shared_html

import kotlinx.html.DIV
import kotlinx.html.div

fun DIV.card(
    cardHeader: String? = null,
    classes: String = "mt-4 h-100",
    cardBody: DIV.() -> Unit,
) {
    div(classes = "card $classes") {
        cardHeader?.let {
            div(classes = "card-header") {
                +cardHeader
            }
        }
        div(classes = "card-body", block = cardBody)
    }
}
