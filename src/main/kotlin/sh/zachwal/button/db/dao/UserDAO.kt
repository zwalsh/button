package sh.zachwal.authserver.db.dao

import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery
import sh.zachwal.authserver.db.jdbi.User

interface UserDAO {
    @SqlQuery("select * from public.user where username = ?")
    fun getByUsername(username: String): User?

    @SqlQuery("insert into public.user (username, hash) values (:username, :hash) returning *")
    fun createUser(@Bind("username") username: String, @Bind("hash") hash: String): User?

    @SqlQuery("select * from public.user")
    fun listUsers(): List<User>

    @SqlQuery("select * from public.user where id = ?")
    fun getById(id: Long): User?
}
