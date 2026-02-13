package sh.zachwal.button.controller

import io.ktor.server.routing.Routing
import io.ktor.server.routing.routing
import kotlin.annotation.AnnotationTarget.CLASS

/**
 * This annotation marks a class that contains endpoints or paths defined as [Routing] extension
 * functions. At application startup, all classes annotated with this annotation are instantiated
 * via injection and all routes are created in a [routing] block.
 */
@Target(CLASS)
annotation class Controller
