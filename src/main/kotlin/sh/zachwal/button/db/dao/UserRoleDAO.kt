package sh.zachwal.button.db.dao

import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.customizer.BindBean
import org.jdbi.v3.sqlobject.locator.UseClasspathSqlLocator
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import sh.zachwal.button.db.jdbi.User
import sh.zachwal.button.db.jdbi.UserRole
import sh.zachwal.button.roles.Role

interface UserRoleDAO {
    @SqlQuery("select * from public.role where user_id = :id")
    fun rolesForUser(@BindBean user: User): List<UserRole>

    @UseClasspathSqlLocator
    @SqlUpdate
    fun grantRoleForUser(@Bind("role") role: Role, @BindBean user: User)

    @SqlQuery("select * from public.role")
    fun allRoles(): List<UserRole>

    @UseClasspathSqlLocator
    @SqlQuery
    fun usersWithoutRole(@Bind("role") role: Role): List<User>
}
