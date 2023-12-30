package sh.zachwal.button.admin

import com.google.inject.Inject
import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.response.respondRedirect
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import kotlinx.html.FormMethod
import kotlinx.html.TBODY
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.submitInput
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.title
import kotlinx.html.tr
import sh.zachwal.button.controller.Controller
import sh.zachwal.button.db.jdbi.WrappedLink
import sh.zachwal.button.roles.adminRoute
import sh.zachwal.button.shared_html.headSetup
import sh.zachwal.button.shared_html.responsiveTable
import sh.zachwal.button.wrapped.WrappedService

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
                            +"Wrapped"
                        }
                        headSetup()
                    }
                    body {
                        div(classes = "container") {
                            h1(classes = "mt-4 text-center") {
                                a(href = "/admin/wrapped/generate") {
                                    +"Generate Page"
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
                call.respondHtml {
                    head {
                        title {
                            +"Wrapped Link Generation"
                        }
                        headSetup()
                    }
                    body {
                        form(method = FormMethod.post, classes = "mb-1") {
                            submitInput(classes = "btn btn-primary") {
                                value = "Generate Links"
                            }
                        }
                    }
                }
            }
            post {
                wrappedService.createWrappedLinks()
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
}
