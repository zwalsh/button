package sh.zachwal.authserver.db.dao

import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import org.jdbi.v3.sqlobject.CreateSqlObject
import org.jdbi.v3.sqlobject.config.RegisterRowMapper
import org.jdbi.v3.sqlobject.customizer.BindBean
import org.jdbi.v3.sqlobject.locator.UseClasspathSqlLocator
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.jdbi.v3.sqlobject.transaction.Transaction
import sh.zachwal.authserver.db.jdbi.FriendRequest
import sh.zachwal.authserver.db.jdbi.Friendship
import sh.zachwal.authserver.db.jdbi.User
import java.sql.ResultSet

interface FriendRequestDAO {

    @SqlUpdate(
        "insert into public.friend_request (requester_id, requested_id) " +
            "values (:request.requester.id, :request.requested.id)"
    )
    fun createRequest(@BindBean("request") request: FriendRequest)

    @UseClasspathSqlLocator
    @SqlQuery
    @RegisterRowMapper(FriendRequestMapper::class)
    fun sentRequests(@BindBean("user") user: User): List<FriendRequest>

    @UseClasspathSqlLocator
    @SqlQuery
    @RegisterRowMapper(FriendRequestMapper::class)
    fun pendingRequests(@BindBean("user") user: User): List<FriendRequest>

    @SqlUpdate(
        "delete from public.friend_request where " +
            "requester_id = :request.requester.id " +
            "and requested_id = :request.requested.id"
    )
    fun deleteFriendRequest(@BindBean("request") request: FriendRequest)

    @CreateSqlObject
    fun createFriendDAO(): FriendDAO

    @Transaction
    fun acceptFriendRequest(request: FriendRequest): Friendship {
        val friendDAO = createFriendDAO()
        deleteFriendRequest(request)
        val friendship = Friendship.create(request.requested, request.requester)
        friendDAO.createFriendship(friendship)
        return friendship
    }

    class FriendRequestMapper : RowMapper<FriendRequest> {
        override fun map(rs: ResultSet, ctx: StatementContext): FriendRequest {
            return FriendRequest(
                User(
                    rs.getLong(1),
                    rs.getString(2),
                    rs.getString(3)
                ),
                User(
                    rs.getLong(4),
                    rs.getString(5),
                    rs.getString(6)
                )
            )
        }
    }
}
