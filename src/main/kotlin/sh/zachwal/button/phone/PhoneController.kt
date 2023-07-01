package sh.zachwal.button.phone

import com.google.inject.Inject
import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.request.receiveParameters
import io.ktor.response.respondRedirect
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.util.getOrFail
import kotlinx.html.FormMethod
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.label
import kotlinx.html.link
import kotlinx.html.small
import kotlinx.html.span
import kotlinx.html.style
import kotlinx.html.submitInput
import kotlinx.html.telInput
import kotlinx.html.textInput
import kotlinx.html.title
import org.slf4j.LoggerFactory
import sh.zachwal.button.controller.Controller
import sh.zachwal.button.shared_html.headSetup

@Controller
class PhoneController @Inject constructor(private val phoneBookService: PhoneBookService) {

    private val logger = LoggerFactory.getLogger(PhoneController::class.java)

    internal fun Routing.signupRoutes() {
        route("/phone/signup") {
            get {
                val badNumber = call.request.queryParameters["badNumber"]?.equals("true") ?: false
                val number = call.request.queryParameters["number"]
                val name = call.request.queryParameters["name"]

                call.respondHtml {
                    head {
                        title {
                            +"Sign up for Button texts"
                        }
                        headSetup()
                    }
                    body {
                        div(classes = "container") {
                            div(classes = "row justify-content-center") {
                                div(classes = "card mt-4") {
                                    div(classes = "card-body") {
                                        form(method = FormMethod.post, classes = "mb-1") {
                                            div(classes = "form-group") {
                                                h1 {
                                                    +"Register For Texts"
                                                }
                                                small(classes = "form-text") {
                                                    style = "max-width: 300px"
                                                    +(
                                                        "We'll text you (at most once a day) when " +
                                                            "someone " +
                                                            "presses the " +
                                                            "button so you can join in!"
                                                        )
                                                }
                                            }
                                            div(classes = "form-group") {
                                                label { +"Name" }
                                                textInput(name = "name", classes = "form-control") {
                                                    placeholder = "name"
                                                    // prefill with value from query param
                                                    name?.let {
                                                        value = it
                                                    }
                                                }
                                            }
                                            div(classes = "form-group") {
                                                label(classes = "form-text") { +"Phone Number" }
                                                small(classes = "form-text text-muted mb-2") {
                                                    style = "max-width: 300px"
                                                    +"Format: 8001234567"
                                                }
                                                telInput(
                                                    name = "phone-number",
                                                    classes = "form-control",
                                                ) {
                                                    pattern = "[0-9]{10}"
                                                    placeholder = "phone number"
                                                    // prefill with value from query param
                                                    number?.let {
                                                        value = it
                                                    }
                                                }
                                            }
                                            if (badNumber) {
                                                div(classes = "alert alert-danger") {
                                                    +"$number is not recognized."
                                                }
                                            }
                                            submitInput(classes = "btn btn-primary") {
                                                value = "Register"
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            post {
                val params = call.receiveParameters()
                val name = params.getOrFail("name")
                val phoneNumber = params.getOrFail("phone-number")
                val contact = try {
                    phoneBookService.register(
                        name,
                        phoneNumber
                    )
                } catch (e: InvalidNumberException) {
                    call.respondRedirect(
                        "/phone/signup?number=${e.invalidNumber.originalNumber}" +
                            "&name=$name" +
                            "&badNumber=true"
                    )
                }
                logger.info("Created contact $contact.")
                call.respondRedirect("/phone/postSignup?name=$name")
            }
        }
    }

    internal fun Routing.postSignup() {
        route("/phone/postSignup") {
            get {
                val name = call.request.queryParameters.getOrFail("name")
                call.respondHtml {
                    head {
                        title {
                            +"Yay!"
                        }
                        link(href = "/static/src/css/button.css", rel = "stylesheet")
                        headSetup()
                    }
                    body {
                        div(classes = "container") {
                            div(classes = "row justify-content-center") {
                                h1(classes = "mt-4") {
                                    +"Congrats, $name!"
                                }
                                div(classes = "m-3") {
                                    +(
                                        "You'll receive texts from now on the first time the button" +
                                            " " +
                                            "is pressed in a given day."
                                        )
                                }
                                a(href = "/", classes = "button") {
                                    span {
                                        +"PRESS"
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
