package sh.zachwal.authserver.db.jdbi

import org.jdbi.v3.core.mapper.reflect.ColumnName

data class User(
    val id: Long,
    val username: String,
    @ColumnName("hash")
    val hashedPassword: String
)
