package me.melijn.melijnbot.database.supporters

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import java.util.function.Consumer


class UserSupporterDao(val driverManager: DriverManager) : Dao(driverManager) {
    override val table: String = "supporters"
    override val tableStructure: String = "userId bigint, guildId bigint, startDate bigint"
    override val keys: String = "PRIMARY KEY (userId)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    fun getSupporters(supporters: Consumer<Set<Supporter>>) {
        val list = HashSet<Supporter>()
        driverManager.executeQuery("SELECT * FROM $table", Consumer { resultset ->
            while (resultset.next()) {
                list.add(Supporter(
                        resultset.getLong("userId"),
                        resultset.getLong("guildId"),
                        resultset.getLong("startDate")
                ))
            }
        })
    }

    fun contains(userId: Long, contains: Consumer<Boolean>) {
        driverManager.executeQuery("SELECT * FROM $table WHERE userId = ?", Consumer { resultset ->
            contains.accept(resultset.next())
        }, userId)
    }

    fun getGuildId(userId: Long, guildId: Consumer<Long>) {
        driverManager.executeQuery("SELECT * FROM $table WHERE userId = ?", Consumer { resultset ->
            if (resultset.next()) {
                guildId.accept(resultset.getLong("guildId"))
            }
        }, userId)
    }
}

class Supporter(val userId: Long, val guildId: Long, val startMillis: Long)