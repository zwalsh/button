package sh.zachwal.button.guice

import com.google.inject.AbstractModule
import com.google.inject.Provides
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.postgres.PostgresPlugin
import org.jdbi.v3.sqlobject.kotlin.KotlinSqlObjectPlugin
import org.jdbi.v3.sqlobject.kotlin.onDemand
import sh.zachwal.authserver.db.dao.FriendDAO
import sh.zachwal.authserver.db.dao.FriendRequestDAO
import sh.zachwal.authserver.db.dao.SessionDAO
import sh.zachwal.authserver.db.dao.UserDAO
import sh.zachwal.authserver.db.dao.UserRoleDAO
import javax.sql.DataSource

class JdbiModule : AbstractModule() {

    @Provides
    fun jdbi(ds: DataSource): Jdbi {
        return Jdbi.create(ds).installPlugin(KotlinPlugin())
            .installPlugin(PostgresPlugin())
            .installPlugin(KotlinSqlObjectPlugin())
    }

    @Provides
    fun userDao(jdbi: Jdbi): UserDAO = jdbi.onDemand()

    @Provides
    fun sessionDao(jdbi: Jdbi): SessionDAO = jdbi.onDemand()

    @Provides
    fun roleDao(jdbi: Jdbi): UserRoleDAO = jdbi.onDemand()

    @Provides
    fun friendDao(jdbi: Jdbi): FriendDAO = jdbi.onDemand()

    @Provides
    fun friendRequestDAO(jdbi: Jdbi): FriendRequestDAO = jdbi.onDemand()
}
