package me.melijn.melijnbot.database.disabled

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import java.util.function.Consumer

class DisabledCommandDao(val driverManager: DriverManager) : Dao(driverManager) {
    override val table: String = "disabledCommands"
    override val tableStructure: String = "guildId bigint, commandId int"
    override val keys: String = "UNIQUE KEY (guildId, commandId)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    fun get(guildId: Long, disabled: Consumer<Set<Int>>) {
        val list = HashSet<Int>()
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", Consumer { resultSet ->
            while (resultSet.next()) {
                list.add(resultSet.getInt("commandId"))
            }
        }, guildId)
        disabled.accept(list.toSet())
    }

    fun contains(guildId: Long, commandId: Int, contains: Consumer<Boolean>) {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND commandId = ?", Consumer {
            contains.accept(it.next())
        }, guildId, commandId)
    }

    fun insert(guildId: Long, commandId: Int) {
        driverManager.executeUpdate("INSERT IGNORE INTO $table (guildId, commandId) VALUES (?, ?)",
                guildId, commandId)
    }
}