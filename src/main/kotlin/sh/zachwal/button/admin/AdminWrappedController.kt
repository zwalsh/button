package sh.zachwal.button.admin

import com.google.inject.Inject
import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.request.receiveParameters
import io.ktor.response.respondRedirect
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.util.getOrFail
import kotlinx.html.BODY
import kotlinx.html.DIV
import kotlinx.html.FormMethod
import kotlinx.html.TBODY
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.dataList
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.id
import kotlinx.html.label
import kotlinx.html.li
import kotlinx.html.numberInput
import kotlinx.html.option
import kotlinx.html.small
import kotlinx.html.submitInput
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.title
import kotlinx.html.tr
import kotlinx.html.ul
import sh.zachwal.button.controller.Controller
import sh.zachwal.button.db.jdbi.WrappedLink
import sh.zachwal.button.roles.adminRoute
import sh.zachwal.button.shared_html.headSetup
import sh.zachwal.button.shared_html.responsiveTable
import sh.zachwal.button.wrapped.WrappedService
import java.time.LocalDate

@Controller
class AdminWrappedController @Inject constructor(
    private val wrappedService: WrappedService
) {

    private val cardClasses = "col-8 mt-4 h-100"

    internal fun Routing.wrapped() {
        adminRoute("/admin/wrapped") {
            get {
                val wrappedLinks = wrappedService.listWrappedLinks()

                call.respondHtml {
                    head {
                        title {
                            +"Wrapped Admin"
                        }
                        headSetup()
                    }
                    body {
                        div(classes = "container") {
                            h1(classes = "mt-4 text-center") {
                                +"Wrapped Admin"
                            }
                            ul {
                                li {
                                    a(href = "/admin/wrapped/generate") {
                                        +"Generate Page"
                                    }
                                }
                                li {
                                    a(href = "/admin/wrapped/notify") {
                                        +"Notify Page"
                                    }
                                }
                            }
                            h1(classes = "mt-4 text-center") {
                                +"Links"
                            }
                            div(classes = "row justify-content-center") {
                                responsiveTable(classes = "m-2") {
                                    thead {
                                        tr {
                                            th {
                                                +"Wrapped Id"
                                            }
                                            th {
                                                +"Year"
                                            }
                                            th {
                                                +"Contact ID"
                                            }
                                        }
                                    }
                                    tbody {
                                        wrappedLinks.forEach { wrappedLink ->
                                            wrappedLinkRow(wrappedLink)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    internal fun Routing.wrappedGenerate() {
        adminRoute("/admin/wrapped/generate") {
            get {
                val currentYear = LocalDate.now().year
                call.respondHtml {
                    head {
                        title {
                            +"Wrapped Link Generation"
                        }
                        headSetup()
                    }
                    body {
                        card {
                            form(method = FormMethod.post, classes = "mb-1") {
                                div(classes = "form-group") {
                                    h1 {
                                        +"Generate Wrapped Links"
                                    }
                                    small {
                                        +"""
                                        This will generate a wrapped link for each contact that has pressed the button 
                                        in the given year. This is a one-time operation and cannot be undone.
                                        """.trimIndent()
                                    }
                                }
                                div(classes = "form-group") {
                                    label { +"Year" }
                                    numberInput(classes = "form-control") {
                                        name = "year"
                                        value = currentYear.toString()
                                    }
                                }
                                submitInput(classes = "btn btn-primary") {
                                    value = "Generate Links"
                                }
                            }
                        }
                    }
                }
            }
            post {
                val year = call.receiveParameters().getOrFail("year").toInt()
                wrappedService.createWrappedLinks(year)
                call.respondRedirect("/admin/wrapped")
            }
        }
    }

    internal fun Routing.wrappedNotify() {
        adminRoute("/admin/wrapped/notify") {
            get {
                val wrappedLinks = wrappedService.listWrappedLinks()
                val countByYear = wrappedLinks
                    .groupBy { it.year }
                    .mapValues { it.value.size }
                val maxYear = countByYear.keys.maxOrNull() ?: 0

                call.respondHtml {
                    head {
                        title {
                            +"Wrapped Notification"
                        }
                        headSetup()
                    }
                    body {
                        card {
                            form(method = FormMethod.post, classes = "mb-1") {
                                div(classes = "form-group") {
                                    h1 {
                                        +"Send Wrapped Links"
                                    }
                                    small {
                                        +"""
                                        This will send a text message to all contacts with a wrapped link for the given 
                                        year. ($countByYear). This cannot be undone and should only be done once.
                                        """.trimIndent()
                                    }
                                }
                                div(classes = "form-group") {
                                    label { +"Year" }
                                    numberInput(classes = "form-control") {
                                        name = "year"
                                        list = "availableYears"
                                        placeholder = maxYear.toString()
                                    }
                                    dataList {
                                        id = "availableYears"
                                        countByYear.keys.forEach {
                                            option {
                                                value = it.toString()
                                            }
                                        }
                                    }
                                }
                                submitInput(classes = "btn btn-danger") {
                                    value = "Send Links"
                                }
                            }
                        }
                    }
                }
            }
            post {
                val year = call.receiveParameters().getOrFail("year").toInt()
                wrappedService.sendWrappedNotification(year)
                call.respondRedirect("/admin/wrapped")
            }
        }
    }

    private fun TBODY.wrappedLinkRow(wrappedLink: WrappedLink) {
        tr {
            th {
                a(href = "/wrapped/${wrappedLink.year}/${wrappedLink.wrappedId}") {
                    +"Link"
                }
            }
            td {
                +"${wrappedLink.year}"
            }
            td {
                +"${wrappedLink.contactId}"
            }
        }
    }

    private fun BODY.card(cardBody: DIV.() -> Unit) {
        div(classes = "container") {
            div(classes = "row justify-content-center") {
                div(classes = "card mt-4") {
                    div(classes = "card-body") {
                        cardBody()
                    }
                }
            }
        }
    }
}
