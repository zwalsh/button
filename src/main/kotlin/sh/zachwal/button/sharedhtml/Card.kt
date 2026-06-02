package sh.zachwal.button.sharedhtml

import kotlinx.html.DIV
import kotlinx.html.div

fun DIV.card(
    cardHeader: String? = null,
    classes: String = "mt-4 h-100",
    cardBodyClasses: String = "card-body",
    cardBody: DIV.() -> Unit,
) {
    div(classes = "card $classes") {
        cardHeader?.let {
            div(classes = "card-header") {
                +cardHeader
            }
        }
        div(classes = cardBodyClasses, block = cardBody)
    }
}
