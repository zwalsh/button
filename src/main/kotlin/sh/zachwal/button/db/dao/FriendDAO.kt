package sh.zachwal.authserver.db.dao

import org.jdbi.v3.sqlobject.customizer.BindBean
import org.jdbi.v3.sqlobject.locator.UseClasspathSqlLocator
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import sh.zachwal.authserver.db.jdbi.Friendship
import sh.zachwal.authserver.db.jdbi.User

interface FriendDAO {

    @UseClasspathSqlLocator
    @SqlQuery
    fun friendsOfUser(user: User): List<User>

    @SqlUpdate(
        "insert into public.friendship (user1_id, user2_id) values " +
            "(:friendship.user1.id, :friendship.user2.id);"
    )
    fun createFriendship(@BindBean("friendship") friendship: Friendship)

    @SqlQuery(
        "select exists (select * from public.friendship where " +
            "user1_id = :friendship.user1.id " +
            "and user2_id = :friendship.user2.id);"
    )
    fun friendshipExists(@BindBean("friendship") friendship: Friendship): Boolean

    @UseClasspathSqlLocator
    @SqlQuery
    fun nonFriendsWithUserRole(user: User): List<User>
}
