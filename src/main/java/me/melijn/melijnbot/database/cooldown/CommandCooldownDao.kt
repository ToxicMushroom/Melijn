package me.melijn.melijnbot.database.cooldown

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import java.util.function.Consumer


class CommandCooldownDao(private val driverManager: DriverManager) : Dao(driverManager) {
    override val table: String = "commandCooldowns"
    override val tableStructure: String = "guildId bigint, commandId int, cooldown bigint"
    override val keys: String = "UNIQUE KEY (guildId, commandId)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    fun getCooldown(guildId: Long, commandId: Int, cooldown: Consumer<Long>) {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND commandId = ?", Consumer { resultSet ->
            if (resultSet.next()) {
                cooldown.accept(resultSet.getLong("cooldown"))
            }
        }, guildId, commandId)
    }

    fun getCooldowns(guildId: Long, cooldownMap: Consumer<Map<Int, Long>>) {
        val hashMap = HashMap<Int, Long>()
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", Consumer { resultSet ->
            while (resultSet.next()) {
                hashMap[resultSet.getInt("commandId")] = resultSet.getLong("cooldown")
            }
        }, guildId)
        cooldownMap.accept(hashMap.toMap())
    }

    fun insert(guildId: Long, commandId: Int, cooldown: Long) {
        driverManager.executeUpdate("INSERT IGNORE INTO $table (guildId, commandId, cooldown) VALUES (?, ?, ?)",
                guildId, commandId, cooldown)
    }
}