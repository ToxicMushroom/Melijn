package me.melijn.melijnbot.database.cooldown

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.objects.command.AbstractCommand
import java.util.function.Consumer


class CommandCooldownDao(private val driverManager: DriverManager) : Dao(driverManager) {
    override val table: String = "commandCooldowns"
    override val tableStructure: String = "guildId bigint, commandId int, cooldown bigint"
    override val keys: String = "UNIQUE KEY (guildId, commandId)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    fun getCooldown(guildId: Long, commandId: Int, cooldown: Consumer<Long>) {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND commandId = ?", { resultSet ->
            if (resultSet.next()) {
                cooldown.accept(resultSet.getLong("cooldown"))
            }
        }, guildId, commandId)
    }

    fun getCooldowns(guildId: Long, cooldownMap: (Map<Int, Long>) -> Unit) {
        val hashMap = HashMap<Int, Long>()
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { resultSet ->
            while (resultSet.next()) {
                hashMap[resultSet.getInt("commandId")] = resultSet.getLong("cooldown")
            }
        }, guildId)
        cooldownMap.invoke(hashMap.toMap())
    }

    fun insert(guildId: Long, commandId: Int, cooldown: Long) {
        driverManager.executeUpdate("INSERT IGNORE INTO $table (guildId, commandId, cooldown) VALUES (?, ?, ?)",
                guildId, commandId, cooldown)
    }

    fun bulkPut(guildId: Long, commands: Set<AbstractCommand>, cooldownMillis: Long) {
        driverManager.getUsableConnection { con ->
            con.prepareStatement("INSERT INTO $table (guildId, commandId, cooldownMillis) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE cooldownMillis = ?").use {
                preparedStatement ->
                preparedStatement.setLong(1, guildId)
                preparedStatement.setLong(3, cooldownMillis)
                preparedStatement.setLong(4, cooldownMillis)
                for (cmd in commands) {
                    preparedStatement.setInt(2, cmd.id)
                    preparedStatement.addBatch()
                }
                preparedStatement.executeLargeBatch()
            }
        }
    }

    fun bulkDelete(guildId: Long, commands: Set<AbstractCommand>) {
        driverManager.getUsableConnection { con ->
            con.prepareStatement("DELETE FROM $table WHERE guildId = ? AND commandId = ?").use {
                preparedStatement ->
                preparedStatement.setLong(1, guildId)
                for (cmd in commands) {
                    preparedStatement.setInt(2, cmd.id)
                    preparedStatement.addBatch()
                }
                preparedStatement.executeLargeBatch()
            }
        }
    }
}