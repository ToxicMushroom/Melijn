package me.melijn.melijnbot.database.supporter

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager


class UserSupporterDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "supporters"
    override val tableStructure: String = "userId bigint, guildId bigint, startDate bigint, lastServerPickTime bigint"
    override val primaryKey: String = "userId"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun getSupporters(supporters: (Set<Supporter>) -> Unit) {
        val list = HashSet<Supporter>()
        driverManager.executeQuery("SELECT * FROM $table", { resultset ->
            while (resultset.next()) {
                list.add(Supporter(
                    resultset.getLong("userId"),
                    resultset.getLong("guildId"),
                    resultset.getLong("startDate"),
                    resultset.getLong("lastServerPickTime")
                ))
            }
            supporters.invoke(list)
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

    suspend fun addUser(userId: Long) {
        driverManager.executeUpdate("INSERT INTO $table (userId, guildId, startDate, lastServerPickTime) VALUES (?, ?, ?, ?)",
            userId, -1, System.currentTimeMillis(), 0)
    }

    suspend fun removeUser(userId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE userId = ?",
            userId)
    }

    suspend fun setGuild(authorId: Long, guildId: Long, lastServerPickTime: Long) {
        driverManager.executeUpdate("UPDATE $table SET guildId = ?, lastServerPickTime = ? WHERE userId = ?",
            guildId, lastServerPickTime, authorId)
    }
}

class Supporter(
    val userId: Long,
    val guildId: Long,
    val startMillis: Long,
    val lastServerPickTime: Long
)