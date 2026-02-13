package sh.zachwal.button.controller

import com.google.inject.Injector
import io.ktor.server.application.Application
import io.ktor.server.routing.Routing
import io.ktor.server.routing.routing
import org.reflections.Reflections
import org.slf4j.LoggerFactory
import java.lang.reflect.Method

private val logger = LoggerFactory.getLogger("ControllerCreator")

private const val PACKAGE_NAME = "sh.zachwal.button"

fun Application.createControllers(injector: Injector) {
    val controllers = getControllerClassesWithReflections()
    controllers.forEach { controller ->
        logger.info("Found controller $controller")
    }
    routing {
        controllers.forEach { controller ->
            val c = injector.getInstance(controller)
            val routeMethods = controller.declaredMethods.filter(Method::isRoutingMethod)
            logger.info("Controller $controller has ${routeMethods.size} routing methods")
            routeMethods.forEach { m ->
                logger.debug(m.name)
                m.invoke(c, this@routing)
            }
        }
    }
}

private fun Method.isRoutingMethod(): Boolean {
    return parameterCount == 1 && parameterTypes.single().equals(Routing::class.java)
}

private fun getControllerClassesWithReflections(): Set<Class<*>> {
    val reflections = Reflections(PACKAGE_NAME)
    return reflections.getTypesAnnotatedWith(Controller::class.java)
}
