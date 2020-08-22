package me.melijn.melijnbot.database.disabled

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class DisabledCommandDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "disabledCommands"
    override val tableStructure: String = "guildId bigint, commandId varchar(16)"
    override val primaryKey: String = "guildId, commandId"

    override val cacheName: String = "disabledcommands"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
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

    fun insert(guildId: Long, commandId: String) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, commandId) VALUES (?, ?) ON CONFLICT ($primaryKey) DO NOTHING",
            guildId, commandId)
    }

    fun bulkPut(guildId: Long, commandIds: Set<String>) {
        driverManager.getUsableConnection { con ->
            con.prepareStatement("INSERT INTO $table (guildId, commandId) VALUES (?, ?) ON CONFLICT ($primaryKey) DO NOTHING").use { statement ->
                statement.setLong(1, guildId)
                for (id in commandIds) {
                    statement.setString(2, id)
                    statement.addBatch()
                }
                statement.executeBatch()
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
                statement.executeBatch()
            }
        }
    }
}