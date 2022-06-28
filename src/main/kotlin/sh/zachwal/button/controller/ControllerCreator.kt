package sh.zachwal.button.controller

import com.google.inject.Injector
import io.ktor.application.Application
import io.ktor.routing.Routing
import io.ktor.routing.routing
import io.ktor.util.reflect.instanceOf
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.lang.reflect.Method
import java.net.URL
import java.util.Enumeration

private val logger = LoggerFactory.getLogger("ControllerCreator")

fun Application.createControllers(injector: Injector) {
    val classes = getClasses("sh.zachwal.button")
    val controllers = classes.filter { clazz ->
        clazz.annotations.any { ann ->
            ann.instanceOf(Controller::class)
        }
    }
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

/**
 * Below classloader magic copied from StackOverflow:
 * https://stackoverflow.com/a/520344
 */

/**
 * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
 *
 * @param packageName The base package
 * @return The classes
 * @throws ClassNotFoundException
 * @throws IOException
 */
@Throws(ClassNotFoundException::class, IOException::class)
private fun getClasses(packageName: String): List<Class<*>> {
    val classLoader = Thread.currentThread().contextClassLoader
    val path = packageName.replace('.', '/')
    val resources: Enumeration<URL> = classLoader.getResources(path)
    val dirs: MutableList<File> = ArrayList()
    while (resources.hasMoreElements()) {
        val resource: URL = resources.nextElement()
        dirs.add(File(resource.file))
    }
    val classes = ArrayList<Class<*>>()
    dirs.forEach { directory ->
        logger.info("Looking for classes in ${directory.name}...")
        val foundClasses = findClasses(directory, packageName)!!
        logger.info("Found ${foundClasses.size} classes.")
        classes.addAll(foundClasses)
    }
    return classes
}

/**
 * Recursive method used to find all classes in a given directory and subdirs.
 *
 * @param directory   The base directory
 * @param packageName The package name for classes found inside the base directory
 * @return The classes
 * @throws ClassNotFoundException
 */
@Throws(ClassNotFoundException::class)
private fun findClasses(directory: File, packageName: String): List<Class<*>>? {
    val classes: MutableList<Class<*>> = ArrayList()
    if (!directory.exists()) {
        return classes
    }
    val files = directory.listFiles()
    files.forEach { file ->
        if (file.isDirectory) {
            assert(!file.name.contains("."))
            classes.addAll(findClasses(file, packageName + "." + file.name)!!)
        } else if (file.name.endsWith(".class") && !file.name.contains("$")) {
            val fileNameWithoutClass = file.name.substring(0, file.name.length - 6)
            classes.add(Class.forName("$packageName.$fileNameWithoutClass"))
        }
    }
    return classes
}
