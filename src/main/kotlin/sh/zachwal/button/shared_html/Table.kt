package sh.zachwal.button.shared_html

import kotlinx.html.FlowContent
import kotlinx.html.TABLE
import kotlinx.html.div
import kotlinx.html.table

fun FlowContent.responsiveTable(block: TABLE.() -> Unit) {
    div(classes = "table-responsive") {
        table(classes = "table", block = block)
    }
}
