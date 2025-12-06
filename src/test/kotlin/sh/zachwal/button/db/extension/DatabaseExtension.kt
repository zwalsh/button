package sh.zachwal.button.db.extension

import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.DirectoryResourceAccessor
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.jackson2.Jackson2Config
import org.jdbi.v3.jackson2.Jackson2Plugin
import org.jdbi.v3.postgres.PostgresPlugin
import org.jdbi.v3.sqlobject.kotlin.KotlinSqlObjectPlugin
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.slf4j.LoggerFactory
import kotlin.io.path.Path

private val postgresContainerNamespace = Namespace.create("postgres")
private const val POSTGRES_CONTAINER_KEY = "POSTGRES_CONTAINER_KEY"
private const val JDBI_KEY = "JDBI"

const val USERNAME = "button"
const val PASSWORD = "button"
const val DB_NAME = "button"

class DatabaseExtension : ParameterResolver, BeforeEachCallback, AfterEachCallback {

    private val logger = LoggerFactory.getLogger(DatabaseExtension::class.java)

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return parameterContext.parameter.parameterizedType in listOf(
            ButtonPostgresContainer::class.java,
            Jdbi::class.java
        )
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        return when (parameterContext.parameter.parameterizedType) {
            ButtonPostgresContainer::class.java -> getPostgresContainer(extensionContext)
            Jdbi::class.java -> getJdbiInstance(extensionContext)
            else -> throw IllegalArgumentException("Cannot resolve parameter of type ${parameterContext.parameter.parameterizedType}")
        }
    }

    private fun getPostgresContainer(context: ExtensionContext): ButtonPostgresContainer {
        return context
            .getStore(postgresContainerNamespace)
            .getOrComputeIfAbsent(
                POSTGRES_CONTAINER_KEY,
                { createPostgresContainer() },
                ButtonPostgresContainer::class.java
            )
    }

    private fun createPostgresContainer(): ButtonPostgresContainer {
        logger.info("Creating container")
        val container = ButtonPostgresContainer()
            .withUsername(USERNAME)
            .withPassword(PASSWORD)
            .withDatabaseName(DB_NAME)
        logger.info("Starting container")
        container.start()
        logger.info("Container started, resetting database")
        resetDatabase(container)
        return container
    }

    private fun resetDatabase(container: ButtonPostgresContainer) {
        assert(container.isRunning)

        logger.info("Making connection")
        val jdbcConnection = JdbcConnection(container.jdbcConnection())
        logger.info("Connected. Initializing Liquibase")
        val database = DatabaseFactory.getInstance()
            .findCorrectDatabaseImplementation(jdbcConnection)
        val liquibase = Liquibase(
            "changelog.json",
            DirectoryResourceAccessor(Path("db")),
            database
        )

        logger.info("Running migrations")
        liquibase.dropAll()
        liquibase.update()
    }

    private fun getJdbiInstance(context: ExtensionContext): Jdbi {
        return context
            .getStore(postgresContainerNamespace)
            .getOrComputeIfAbsent(
                JDBI_KEY,
                { createJdbiInstance(context) },
                Jdbi::class.java
            )
    }

    private fun createJdbiInstance(context: ExtensionContext): Jdbi {
        val container = getPostgresContainer(context)
        val jdbi = Jdbi.create(container.jdbcUrl, USERNAME, PASSWORD)
            .installPlugin(KotlinPlugin())
            .installPlugin(PostgresPlugin())
            .installPlugin(KotlinSqlObjectPlugin())
            .installPlugin(Jackson2Plugin())
        jdbi.getConfig(Jackson2Config::class.java)
        return jdbi
    }

    private fun truncateData(container: ButtonPostgresContainer) {
        assert(container.isRunning)

        val truncate = """
            DO $$ DECLARE
                r RECORD;
            BEGIN
                -- Disable all triggers (including foreign key constraints)
                PERFORM 'ALTER TABLE ' || quote_ident(schemaname) || '.' || quote_ident(tablename) || ' DISABLE TRIGGER ALL'
                FROM pg_tables
                WHERE schemaname = 'public';

                -- Truncate all tables
                FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename != 'game') LOOP
                    EXECUTE 'TRUNCATE TABLE ' || quote_ident(r.tablename) || ' CASCADE';
                END LOOP;

                -- Enable all triggers
                PERFORM 'ALTER TABLE ' || quote_ident(schemaname) || '.' || quote_ident(tablename) || ' ENABLE TRIGGER ALL'
                FROM pg_tables
                WHERE schemaname = 'public';
            END $$;
        """.trimIndent()

        logger.info("Truncating data")
        container.jdbcConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(truncate)
            }
        }
    }

    override fun afterEach(context: ExtensionContext) {
        val container = getPostgresContainer(context)
        truncateData(container)
    }

    override fun beforeEach(context: ExtensionContext) {
        // Optionally run fixtures here if needed
    }
}
