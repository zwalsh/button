package sh.zachwal.button.guice

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.postgres.PostgresPlugin
import org.jdbi.v3.sqlobject.kotlin.KotlinSqlObjectPlugin
import org.jdbi.v3.sqlobject.kotlin.onDemand
import sh.zachwal.button.db.dao.ContactDAO
import sh.zachwal.button.db.dao.ContactTokenDAO
import sh.zachwal.button.db.dao.NotificationDAO
import sh.zachwal.button.db.dao.PressDAO
import sh.zachwal.button.db.dao.SentMessageDAO
import sh.zachwal.button.db.dao.SessionDAO
import sh.zachwal.button.db.dao.UserDAO
import sh.zachwal.button.db.dao.UserRoleDAO
import javax.sql.DataSource

class JdbiModule : AbstractModule() {

    @Provides
    @Singleton
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
    fun pressDao(jdbi: Jdbi): PressDAO = jdbi.onDemand()

    @Provides
    fun contactDao(jdbi: Jdbi): ContactDAO = jdbi.onDemand()

    @Provides
    fun sentMessageDao(jdbi: Jdbi): SentMessageDAO = jdbi.onDemand()

    @Provides
    fun notificationDao(jdbi: Jdbi): NotificationDAO = jdbi.onDemand()

    @Provides
    fun contactTokenDao(jdbi: Jdbi): ContactTokenDAO = jdbi.onDemand()
}
