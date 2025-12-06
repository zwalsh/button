package sh.zachwal.button.db.extension

import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.sql.Connection
import java.sql.DriverManager

class ButtonPostgresContainer : PostgreSQLContainer<ButtonPostgresContainer>(DockerImageName.parse("postgres:12.18")) {
    fun jdbcConnection(): Connection {
        return DriverManager.getConnection(
            jdbcUrl,
            USERNAME,
            PASSWORD
        )
    }
}