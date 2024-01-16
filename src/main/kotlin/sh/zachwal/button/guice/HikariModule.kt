package sh.zachwal.button.guice

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.slf4j.LoggerFactory
import sh.zachwal.button.config.AppConfig
import javax.sql.DataSource

class HikariModule : AbstractModule() {

    private val logger = LoggerFactory.getLogger(HikariModule::class.java)

    @Provides
    @Singleton
    fun hikariConfig(appConfig: AppConfig): HikariConfig {
        val hikariConfig = HikariConfig("/hikari.properties")

        appConfig.dbNameOverride?.let { override ->
            logger.info("Overriding configured db name with value from ktor.deployment.db_name=$override")
            hikariConfig.dataSourceProperties.setProperty("databaseName", override)
        } ?: run {
            logger.info("No db name override. Continuing with value in hikari.properties.")
        }
        appConfig.dbUserOverride?.let { override ->
            logger.info(
                "Overriding configured db user with value from ktor.deployment" +
                    ".db_user=$override"
            )
            hikariConfig.dataSourceProperties.setProperty("user", override)
        } ?: run {
            logger.info("No db user override. Continuing with value in hikari.properties.")
        }

        appConfig.dbPasswordOverride?.let { override ->
            logger.info("Overriding configured db password with value from ktor.deployment.db_password")
            hikariConfig.dataSourceProperties.setProperty("password", override)
        } ?: run {
            logger.info("No db password override. Continuing with value in hikari.properties.")
        }
        return hikariConfig
    }

    @Provides
    @Singleton
    fun dataSource(config: HikariConfig): DataSource {
        config.schema = "public"
        return HikariDataSource(config)
    }
}
