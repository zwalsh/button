package sh.zachwal.button.wrapped

import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.util.getOrFail
import kotlinx.html.DIV
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.h3
import kotlinx.html.head
import kotlinx.html.link
import kotlinx.html.script
import kotlinx.html.span
import kotlinx.html.title
import sh.zachwal.button.controller.Controller
import sh.zachwal.button.shared_html.headSetup
import javax.inject.Inject

@Controller
class WrappedController @Inject constructor(
    private val wrappedService: WrappedService
) {

    internal fun Routing.wrappedRoute() {
        val wrappedIdParam = "wrappedId"
        val yearParam = "year"
        route("/wrapped/{$yearParam}/{$wrappedIdParam}") {
            get {
                val year = call.parameters.getOrFail(yearParam).toInt()
                val wrappedId = call.parameters.getOrFail(wrappedIdParam)
                val wrapped = wrappedService.wrapped(year, wrappedId)

                call.respondHtml {
                    head {
                        title {
                            +"${wrapped.name}'s ${wrapped.year} Wrapped"
                        }
                        headSetup()
                        link(href = "/static/src/css/wrapped.css", rel = "stylesheet")
                        script {
                            type = "module"
                            src = "/static/src/js/wrapped.js"
                        }
                    }
                    body {
                        div(classes = "container") {
                            wrappedSection(cardClasses = "welcome justify-content-center") {
                                h1(classes = "year") {
                                    +"${wrapped.year}"
                                }
                                h2 {
                                    +"Hello, ${wrapped.name}!"
                                }
                                h3 {
                                    +"Welcome to your Button Wrapped, ${wrapped.year}."
                                }
                            }
                            wrappedSection(cardClasses = "count justify-content-between") {
                                h3(classes = "top-text") {
                                    +"You pressed the Button..."
                                }
                                div(classes = "d-flex flex-row justify-content-center") {
                                    button(classes = "pressMePls") {
                                        span {
                                            +"PRESS"
                                        }
                                    }
                                    h1(classes = "d-none animate-count-up") {
                                        +"${wrapped.count}"
                                    }
                                }
                                h3(classes = "bottom-text") {
                                    +"...times this year."
                                }
                            }
                            wrappedSection(cardClasses = "rank justify-content-between") {
                                h3(classes = "top-text") {
                                    +"You are the number..."
                                }
                                div(classes = "d-flex flex-row justify-content-center") {
                                    button(classes = "pressMePls") {
                                        span {
                                            +"PRESS"
                                        }
                                    }
                                    h1(classes = "d-none animate-count-up") {
                                        +"${wrapped.rank}"
                                    }
                                }
                                h3(classes = "bottom-text") {
                                    +"...Button presser overall!"
                                }
                            }
                            wrappedSection(cardClasses = "percentile justify-content-between") {
                                h3(classes = "top-text") {
                                    +"You are in the top..."
                                }
                                div(classes = "d-flex flex-row justify-content-center") {
                                    button(classes = "pressMePls") {
                                        span {
                                            +"PRESS"
                                        }
                                    }
                                    h1(classes = "d-none") {
                                        +"${wrapped.percentile}%"
                                    }
                                }
                                h3(classes = "bottom-text") {
                                    +"...of Button pressers!"
                                }
                            }
                            wrappedSection(
                                cardClasses = "uniqueDaysCount " + "justify-content-between"
                            ) {
                                h3(classes = "top-text") {
                                    +"You pressed the Button on..."
                                }
                                div(classes = "d-flex flex-row justify-content-center") {
                                    button(classes = "pressMePls") {
                                        span {
                                            +"PRESS"
                                        }
                                    }
                                    h1(classes = "d-none animate-count-up") {
                                        +"${wrapped.uniqueDaysCount}"
                                    }
                                }
                                h3(classes = "bottom-text") {
                                    +"...different days!"
                                }
                            }
                            wrappedSection(cardClasses = "uniqueDaysRank justify-content-between") {
                                h3(classes = "top-text") {
                                    +"That's number..."
                                }
                                div(classes = "d-flex flex-row justify-content-center") {
                                    button(classes = "pressMePls") {
                                        span {
                                            +"PRESS"
                                        }
                                    }
                                    h1(classes = "d-none animate-count-up") {
                                        +"${wrapped.uniqueDaysRank}"
                                    }
                                }
                                h3(classes = "bottom-text") {
                                    +"...of all pressers!"
                                }
                            }
                            wrappedSection(
                                cardClasses = "uniqueDaysPercentile " + "justify-content-between"
                            ) {
                                h3(classes = "top-text") {
                                    +"You pressed on more days than..."
                                }
                                div(classes = "d-flex flex-row justify-content-center") {
                                    button(classes = "pressMePls") {
                                        span {
                                            +"PRESS"
                                        }
                                    }
                                    h1(classes = "d-none") {
                                        +"${100 - wrapped.uniqueDaysPercentile}%"
                                    }
                                }
                                h3(classes = "bottom-text") {
                                    +"...of Button pressers!"
                                }
                            }
                            wrappedSection(cardClasses = "day justify-content-between") {
                                h3(classes = "top-text") {
                                    +"Your favorite day was..."
                                }

                                div(classes = "d-flex flex-row justify-content-center") {
                                    button(classes = "pressMePls") {
                                        span {
                                            +"PRESS"
                                        }
                                    }
                                    h1(classes = "d-none") {
                                        +"${wrapped.favoriteDay}!"
                                    }
                                }
                                h3 {}
                            }
                            wrappedSection(cardClasses = "day-count justify-content-between") {
                                h3(classes = "top-text") {
                                    +"You pressed the Button..."
                                }
                                div(classes = "d-flex flex-row justify-content-center") {
                                    button(classes = "pressMePls") {
                                        span {
                                            +"PRESS"
                                        }
                                    }
                                    h1(classes = "d-none animate-count-up") {
                                        +"${wrapped.favoriteDayCount}"
                                    }
                                }
                                h3(classes = "bottom-text") {
                                    +"...times on ${wrapped.favoriteDay}."
                                }
                            }
                            wrappedSection(cardClasses = "time-of-day justify-content-between") {
                                h3(classes = "top-text") {
                                    +"Your favorite time of day was..."
                                }

                                div(classes = "d-flex flex-row justify-content-center") {
                                    button(classes = "pressMePls") {
                                        span {
                                            +"PRESS"
                                        }
                                    }
                                    h1(classes = "d-none") {
                                        +"${wrapped.favoriteHourString}!"
                                    }
                                }
                                h3 {}
                            }
                            wrappedSection(cardClasses = "time-count justify-content-between") {
                                h3(classes = "top-text") {
                                    +"You pressed the Button..."
                                }
                                div(classes = "d-flex flex-row justify-content-center") {
                                    button(classes = "pressMePls") {
                                        span {
                                            +"PRESS"
                                        }
                                    }
                                    h1(classes = "d-none animate-count-up") {
                                        +"${wrapped.favoriteHourCount}"
                                    }
                                }
                                h3(classes = "bottom-text") {
                                    +"...times at ${wrapped.favoriteHourString}."
                                }
                            }
                            wrappedSection(cardClasses = "justify-content-between") {
                                h3(classes = "top-text") {
                                    +"Thanks for pressing!"
                                }
                                div(classes = "d-flex flex-row justify-content-center") {
                                    a(href = "/") {
                                        button(classes = "pressMePls") {
                                            span {
                                                +"PRESS"
                                            }
                                        }
                                    }
                                }
                                h3 {}
                            }
                        }
                    }
                }
            }
        }
    }

    private fun DIV.wrappedSection(
        cardClasses: String = "",
        content: DIV.() -> Unit
    ) {
        div(classes = "row vh-90 snapChild") {
            div(classes = "card vw-100 m-3 p-3 d-flex $cardClasses") {
                content()
            }
        }
    }
}
