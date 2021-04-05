package me.melijn.melijnbot.database.supporter

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class SupporterDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "supporters"
    override val tableStructure: String = "userId bigint, guildId bigint, startDate bigint, lastServerPickTime bigint"
    override val primaryKey: String = "userId"

    override val cacheName: String = "supporter"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun getSupporters(supporters: (Set<Supporter>) -> Unit) {
        driverManager.executeQuery("SELECT * FROM $table", { resultset ->
            val list = HashSet<Supporter>()
            while (resultset.next()) {
                list.add(
                    Supporter(
                        resultset.getLong("userId"),
                        resultset.getLong("guildId"),
                        resultset.getLong("startDate"),
                        resultset.getLong("lastServerPickTime")
                    )
                )
            }
            supporters.invoke(list)
        })
    }

    suspend fun getSupporters(): Set<Supporter> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table", { resultset ->
            val list = HashSet<Supporter>()
            while (resultset.next()) {
                list.add(
                    Supporter(
                        resultset.getLong("userId"),
                        resultset.getLong("guildId"),
                        resultset.getLong("startDate"),
                        resultset.getLong("lastServerPickTime")
                    )
                )
            }
            it.resume(list)
        })
    }

    fun contains(userId: Long, contains: (Boolean) -> Unit) {
        driverManager.executeQuery("SELECT * FROM $table WHERE userId = ?", { resultset ->
            contains.invoke(resultset.next())
        }, userId)
    }

    fun getGuildId(userId: Long, guildId: (Long) -> Unit) {
        driverManager.executeQuery("SELECT * FROM $table WHERE userId = ?", { resultset ->
            if (resultset.next()) {
                guildId.invoke(resultset.getLong("guildId"))
            }
        }, userId)
    }

    fun addUser(supporter: Supporter) {
        driverManager.executeUpdate(
            "INSERT INTO $table (userId, guildId, startDate, lastServerPickTime) VALUES (?, ?, ?, ?)",
            supporter.userId, supporter.guildId, supporter.startMillis, supporter.lastServerPickTime
        )
    }

    fun removeUser(userId: Long) {
        driverManager.executeUpdate(
            "DELETE FROM $table WHERE userId = ?",
            userId
        )
    }

    fun setGuild(authorId: Long, guildId: Long, lastServerPickTime: Long) {
        driverManager.executeUpdate(
            "UPDATE $table SET guildId = ?, lastServerPickTime = ? WHERE userId = ?",
            guildId, lastServerPickTime, authorId
        )
    }
}

class Supporter(
    val userId: Long,
    val guildId: Long,
    val startMillis: Long,
    val lastServerPickTime: Long
)