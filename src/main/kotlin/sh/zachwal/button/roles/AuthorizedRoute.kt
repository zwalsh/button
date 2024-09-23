package sh.zachwal.button.roles

import io.ktor.application.feature
import io.ktor.auth.authenticate
import io.ktor.routing.Route
import io.ktor.routing.RouteSelector
import io.ktor.routing.RouteSelectorEvaluation
import io.ktor.routing.Routing
import io.ktor.routing.RoutingResolveContext
import io.ktor.routing.application
import io.ktor.routing.createRouteFromPath
import sh.zachwal.button.auth.CONTACT_SESSION_AUTH
import sh.zachwal.button.roles.Role.ADMIN
import sh.zachwal.button.roles.Role.USER

fun Route.requireOneOfRoles(vararg roles: Role, build: Route.() -> Unit): Route {
    val authorisedRoute = createChild(AuthorisedRouteSelector())
    authorisedRoute.build()
    application.feature(RoleAuthorization).interceptPipeline(this, roles.toSet())
    return authorisedRoute
}

class AuthorisedRouteSelector : RouteSelector() {
    override fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation =
        RouteSelectorEvaluation.Constant
}

internal fun Routing.protectedRoute(path: String, roles: List<Role>, build: Route.() -> Unit): Route {
    return createRouteFromPath(path).apply {
        authenticate {
            requireOneOfRoles(*roles.toTypedArray()) {
                build()
            }
        }
    }
}

internal fun Routing.adminRoute(path: String, build: Route.() -> Unit): Route =
    protectedRoute(path, listOf(ADMIN), build)

internal fun Routing.approvedUserRoute(path: String, build: Route.() -> Unit): Route =
    protectedRoute(path, listOf(USER), build)

internal fun Routing.contactRoute(path: String, build: Route.() -> Unit): Route =
    createRouteFromPath(path).apply {
        authenticate(CONTACT_SESSION_AUTH) {
            build()
        }
    }