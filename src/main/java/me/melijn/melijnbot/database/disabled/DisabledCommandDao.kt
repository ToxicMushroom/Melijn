package me.melijn.melijnbot.database.disabled

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.objects.command.AbstractCommand

class DisabledCommandDao(val driverManager: DriverManager) : Dao(driverManager) {
    override val table: String = "disabledCommands"
    override val tableStructure: String = "guildId bigint, commandId int"
    override val keys: String = "UNIQUE KEY (guildId, commandId)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    fun get(guildId: Long, disabled: (Set<Int>) -> Unit) {
        val list = HashSet<Int>()
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { resultSet ->
            while (resultSet.next()) {
                list.add(resultSet.getInt("commandId"))
            }
        }, guildId)
        disabled.invoke(list.toSet())
    }

    fun contains(guildId: Long, commandId: Int, contains: (Boolean) -> Unit) {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND commandId = ?", {
            contains.invoke(it.next())
        }, guildId, commandId)
    }

    fun insert(guildId: Long, commandId: Int) {
        driverManager.executeUpdate("INSERT IGNORE INTO $table (guildId, commandId) VALUES (?, ?)",
                guildId, commandId)
    }

    fun bulkPut(guildId: Long, commands: Set<AbstractCommand>) {
        driverManager.getUsableConnection { con ->
            con.prepareStatement("INSERT IGNORE INTO $table (guildId, commandId) VALUES (?, ?)").use { statement ->
                statement.setLong(1, guildId)
                for (cmd in commands) {
                    statement.setInt(2, cmd.id)
                    statement.addBatch()
                }
                statement.executeLargeBatch()
            }
        }
    }

    fun bulkDelete(guildId: Long, commands: Set<AbstractCommand>) {
        driverManager.getUsableConnection { con ->
            con.prepareStatement("DELETE FROM $table WHERE guildId = ? AND commandId = ?)").use { statement ->
                statement.setLong(1, guildId)
                for (cmd in commands) {
                    statement.setInt(2, cmd.id)
                    statement.addBatch()
                }
                statement.executeLargeBatch()
            }
        }
    }
}