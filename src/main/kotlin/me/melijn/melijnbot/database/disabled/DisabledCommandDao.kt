package me.melijn.melijnbot.database.disabled

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class DisabledCommandDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "disabledCommands"
    override val tableStructure: String = "guildId bigint UNIQUE, commandId varchar(16) UNIQUE"
    override val keys: String = ""

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    suspend fun get(guildId: Long): Set<String> = suspendCoroutine {
        val list = HashSet<String>()
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { resultSet ->
            while (resultSet.next()) {
                list.add(resultSet.getString("commandId"))
            }
            it.resume(list)
        }, guildId)
    }

    suspend fun contains(guildId: Long, commandId: String): Boolean = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND commandId = ?", { rs ->
            it.resume(rs.next())
        }, guildId, commandId)
    }

    suspend fun insert(guildId: Long, commandId: String) {
        driverManager.executeUpdate("INSERT IGNORE INTO $table (guildId, commandId) VALUES (?, ?)",
            guildId, commandId)
    }

    fun bulkPut(guildId: Long, commandIds: Set<String>) {
        driverManager.getUsableConnection { con ->
            con.prepareStatement("INSERT IGNORE INTO $table (guildId, commandId) VALUES (?, ?)").use { statement ->
                statement.setLong(1, guildId)
                for (id in commandIds) {
                    statement.setString(2, id)
                    statement.addBatch()
                }
                statement.executeLargeBatch()
            }
        }
    }

    fun bulkDelete(guildId: Long, commandIds: Set<String>) {
        driverManager.getUsableConnection { con ->
            con.prepareStatement("DELETE FROM $table WHERE guildId = ? AND commandId = ?").use { statement ->
                statement.setLong(1, guildId)
                for (id in commandIds) {
                    statement.setString(2, id)
                    statement.addBatch()
                }
                statement.executeLargeBatch()
            }
        }
    }
}