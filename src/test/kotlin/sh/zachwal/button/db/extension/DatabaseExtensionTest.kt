package sh.zachwal.button.db.extension

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.jdbi.v3.core.Jdbi
import com.google.common.truth.Truth.assertThat

@ExtendWith(DatabaseExtension::class)
class DatabaseExtensionTest {
    @Test
    fun `can connect to db`(container: ButtonPostgresContainer) {
        val stmt = container.createConnection("").createStatement()
        val resultSet = stmt.executeQuery("SELECT version();")
        resultSet.next()
        val version = resultSet.getString(1)
        assertThat(version).contains("PostgreSQL")
    }

    @Test
    fun `runs migrations`(container: ButtonPostgresContainer) {
        val stmt = container.createConnection("").createStatement()
        val resultSet =
            stmt.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public';")
        val tables = mutableListOf<String>()
        while (resultSet.next()) {
            tables.add(resultSet.getString(1))
        }
        assertThat(tables).containsExactly(
            "databasechangelog",
            "databasechangeloglock",
            "user",
            "session",
            "role",
            "press",
            "contact",
            "sent_message",
            "notification",
            "contact_token",
            "wrapped_link"
        )
    }

    @Test
    fun `creates Jdbi instance`(jdbi: Jdbi) {
        val version = jdbi.withHandle<String, Exception> { handle ->
            handle.createQuery("SELECT version();")
                .mapTo(String::class.java)
                .one()
        }
        assertThat(version).contains("PostgreSQL")
    }
}
