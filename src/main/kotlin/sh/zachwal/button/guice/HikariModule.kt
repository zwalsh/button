package sh.zachwal.button.guice

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource

class HikariModule(private val config: HikariConfig) : AbstractModule() {

    @Provides
    fun dataSource(): DataSource {
        config.schema = "public"
        return HikariDataSource(config)
    }
}
