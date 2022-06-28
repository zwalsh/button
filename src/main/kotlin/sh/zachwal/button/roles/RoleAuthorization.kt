package sh.zachwal.button.roles

import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelinePhase
import org.slf4j.LoggerFactory
import sh.zachwal.button.session.SessionPrincipal

// taken from https://medium.com/@shrikantjagtap99/role-based-authorization-feature-in-ktor-web-framework-in-kotlin-dda88262a86a
class RoleAuthorization internal constructor(config: Configuration) {
    private val logger = LoggerFactory.getLogger(RoleAuthorization::class.java)

    constructor(provider: RoleBasedAuthorizer) : this(Configuration(provider))

    private val config = config.copy()

    class Configuration internal constructor(var provider: RoleBasedAuthorizer) {
        internal fun copy(): Configuration = Configuration(provider)
    }

    class RoleBasedAuthorizer {
        internal lateinit var authorizationFunction: suspend ApplicationCall.(
            Set<Role>,
            SessionPrincipal
        ) -> Role?

        fun validate(body: suspend ApplicationCall.(Set<Role>, SessionPrincipal) -> Role?) {
            authorizationFunction = body
        }
    }

    fun interceptPipeline(pipeline: ApplicationCallPipeline, roles: Set<Role>) {
        pipeline.insertPhaseAfter(ApplicationCallPipeline.Features, authorizationPhase)
        pipeline.intercept(authorizationPhase) {
            val call = call
            val session = call.sessions.get<SessionPrincipal>() ?: run {
                logger.info("Blocking access; unauthenticated")
                call.respond(HttpStatusCode.Unauthorized)
                finish()
                return@intercept
            }

            if (!session.isValid()) {
                logger.info("Blocking access; session expired")
                call.respondRedirect("/login")
                finish()
                return@intercept
            }

            config.provider.authorizationFunction(call, roles, session)?.let {
                logger.info("Passing user ${session.user} with role $it")
                return@intercept
            } ?: run {
                call.respond(HttpStatusCode.Forbidden)
                finish()
            }
        }
    }

    companion object Feature : ApplicationFeature<ApplicationCallPipeline, RoleBasedAuthorizer, RoleAuthorization> {
        val authorizationPhase = PipelinePhase("authorization")

        override val key: AttributeKey<RoleAuthorization> = AttributeKey("RoleAuthorization")

        override fun install(
            pipeline: ApplicationCallPipeline,
            configure: RoleBasedAuthorizer.() -> Unit
        ): RoleAuthorization {
            val configuration = RoleBasedAuthorizer().apply(configure)
            return RoleAuthorization(configuration)
        }
    }
}
