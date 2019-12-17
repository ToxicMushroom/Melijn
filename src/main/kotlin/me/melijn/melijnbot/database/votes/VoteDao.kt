package me.melijn.melijnbot.database.votes

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class VoteDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "votes"
    override val tableStructure: String = "userId bigint, votes bigint, streak bigint, lastTime bigint"
    override val primaryKey: String = "userId"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun getVotesObject(userId: Long): UserVote? = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM votes WHERE userId= ? LIMIT 1", { rs ->
            if (rs.next()) {
                it.resume(UserVote(
                    rs.getLong("userId"),
                    rs.getLong("votes"),
                    rs.getLong("streak"),
                    rs.getLong("lastTime")
                ))
            } else {
                it.resume(null)
            }
        }, userId)
    }

    suspend fun set(userId: Long, votes: Long, streak: Long, lastTime: Long) {
        val sql = "INSERT INTO $table (userId, votes, streak, lastTime) VALUES (?, ?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET votes = ?, streak = ?, lastTime = ?"
        driverManager.executeUpdate(sql, userId, votes, streak, lastTime, votes, streak, lastTime)
    }
}

data class UserVote(
    val userId: Long,
    val votes: Long,
    val streak: Long,
    val lastTime: Long
)