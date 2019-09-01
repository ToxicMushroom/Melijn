package me.melijn.melijnbot.database.supporter

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager


class UserSupporterDao(val driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "supporters"
    override val tableStructure: String = "userId bigint, guildId bigint, startDate bigint"
    override val keys: String = "PRIMARY KEY (userId)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    fun getSupporters(supporters: (Set<Supporter>) -> Unit) {
        val list = HashSet<Supporter>()
        driverManager.executeQuery("SELECT * FROM $table", { resultset ->
            while (resultset.next()) {
                list.add(Supporter(
                        resultset.getLong("userId"),
                        resultset.getLong("guildId"),
                        resultset.getLong("startDate")
                ))
            }
            supporters.invoke(list)
        })
    }

    fun contains(userId: Long, contains: (Boolean) -> Unit) {
        driverManager.executeQuery("SELECT * FROM $table WHERE userId = ?",  { resultset ->
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
}

class Supporter(val userId: Long, val guildId: Long, val startMillis: Long)