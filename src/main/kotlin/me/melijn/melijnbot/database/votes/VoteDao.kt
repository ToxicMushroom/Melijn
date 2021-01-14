package me.melijn.melijnbot.database.votes

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class VoteDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "votes"
    override val tableStructure: String =
        "userId bigint, votes bigint, streak bigint, topggLastTime bigint, dblLastTime bigint, bfdLastTime bigint, dboatsLastTime bigint"
    override val primaryKey: String = "userId"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun getVotesObject(userId: Long): UserVote? = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM votes WHERE userId= ? LIMIT 1", { rs ->
            if (rs.next()) {
                it.resume(
                    UserVote(
                        rs.getLong("userId"),
                        rs.getLong("votes"),
                        rs.getLong("streak"),
                        rs.getLong("topggLastTime"),
                        rs.getLong("dblLastTime"),
                        rs.getLong("bfdLastTime"),
                        rs.getLong("dboatsLastTime"),
                    )
                )
            } else {
                it.resume(null)
            }
        }, userId)
    }

    fun set(userVote: UserVote) {
        val sql =
            "INSERT INTO $table (userId, votes, streak, topggLastTime, dblLastTime, bfdLastTime, dboatsLastTime) VALUES (?, ?, ?, ?, ?, ?, ?)" +
                " ON CONFLICT ($primaryKey) DO UPDATE SET votes = ?, streak = ?, topggLastTime = ?, dblLastTime = ?, bfdLastTime = ?, dboatsLastTime = ?"
        val (userId, votes, streak, topggLastTime, dblLastTime, bfdLastTime, dboatsLastTime) = userVote
        driverManager.executeUpdate(
            sql, userId, votes, streak, topggLastTime, dblLastTime, bfdLastTime, dboatsLastTime,
            votes, streak, topggLastTime, dblLastTime, bfdLastTime, dboatsLastTime
        )
    }

    suspend fun getTop(users: Int, offset: Int): Map<Long, Long> = suspendCoroutine {
        driverManager.executeQuery(
            "SELECT * FROM $table ORDER BY votes DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
            { rs ->
                val map = mutableMapOf<Long, Long>()

                while (rs.next()) {
                    map[rs.getLong("userId")] = rs.getLong("votes")
                }

                it.resume(map)
            },
            offset,
            users
        )
    }

    suspend fun getPosition(userId: Long): Pair<Long, Long> = suspendCoroutine {
        driverManager.executeQuery(
            "SELECT * FROM (SELECT *, row_number() OVER (ORDER BY votes DESC) as position FROM $table) x WHERE userId = ?",
            { rs ->
                if (rs.next()) {
                    it.resume(Pair(rs.getLong("votes"), rs.getLong("position")))
                } else {
                    it.resume(Pair(0, -1))
                }
            },
            userId
        )
    }
}

data class UserVote(
    val userId: Long,
    val votes: Long,
    val streak: Long,
    val topggLastTime: Long,
    val dblLastTime: Long,
    val bfdLastTime: Long,
    val dboatsLastTime: Long
)